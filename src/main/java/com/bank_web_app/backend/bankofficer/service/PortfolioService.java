package com.bank_web_app.backend.bankofficer.service;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.dto.request.BankOfficerCustomerFilterRequest;
import com.bank_web_app.backend.bankofficer.dto.response.BankOfficerCustomerSummaryResponse;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.creditlens.entity.BankCreditEvaluation;
import com.bank_web_app.backend.creditlens.repository.BankCreditEvaluationRepository;
import com.bank_web_app.backend.user.entity.User;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

	private final BankOfficerContextService bankOfficerContextService;
	private final BankCustomerRepository bankCustomerRepository;
	private final BankCreditEvaluationRepository bankCreditEvaluationRepository;

	public PortfolioService(
		BankOfficerContextService bankOfficerContextService,
		BankCustomerRepository bankCustomerRepository,
		BankCreditEvaluationRepository bankCreditEvaluationRepository
	) {
		this.bankOfficerContextService = bankOfficerContextService;
		this.bankCustomerRepository = bankCustomerRepository;
		this.bankCreditEvaluationRepository = bankCreditEvaluationRepository;
	}

	@Transactional(readOnly = true)
	public List<BankOfficerCustomerSummaryResponse> getBankCustomersForOfficer(BankOfficerCustomerFilterRequest filters) {
		BankOfficer officer = bankOfficerContextService.resolveLoggedInBankOfficer();
		Map<Long, BankCreditEvaluation> latestEvaluationByCustomerId = loadLatestEvaluations(officer.getOfficerId());
		String normalizedSearch = normalize(filters == null ? null : filters.search());
		String normalizedStatus = normalize(filters == null ? null : filters.status());
		String normalizedRiskLevel = normalize(filters == null ? null : filters.riskLevel());
		String normalizedSortBy = normalize(filters == null ? null : filters.sortBy());

		List<CustomerSummaryView> rows = bankCustomerRepository
			.findAllByOfficer_OfficerId(officer.getOfficerId())
			.stream()
			.map(customer -> toSummary(customer, latestEvaluationByCustomerId.get(customer.getBankCustomerId())))
			// Apply server-side filters and sorting here. These filters use the
			// canonical values from the database (including the latest
			// `BankCreditEvaluation` where available) so that results are
			// consistent and scalable for large datasets.
			.filter(view -> matchesSearch(view.summary(), normalizedSearch))
			.filter(view -> matchesStatus(view.summary(), normalizedStatus))
			.filter(view -> matchesRiskLevel(view.summary(), normalizedRiskLevel))
			.sorted(comparatorFor(normalizedSortBy))
			.toList();

		return rows.stream().map(CustomerSummaryView::summary).toList();
	}

	@Transactional(readOnly = true)
	public List<BankOfficerCustomerSummaryResponse> getBankCustomersForOfficer() {
		return getBankCustomersForOfficer(new BankOfficerCustomerFilterRequest(null, null, null, null));
	}

	private CustomerSummaryView toSummary(BankCustomer customer, BankCreditEvaluation evaluation) {
		User user = customer.getUser();
		// Map the canonical credit evaluation values into the officer-facing
		// summary response. `riskLevel` and `creditScore` come from the
		// `BankCreditEvaluation` entity when present and are the source of
		// truth for filter and sort operations on the server.
		String riskLevel = evaluation == null ? "UNKNOWN" : safe(evaluation.getRiskLevel()).toUpperCase(Locale.ROOT);
		Integer creditScore = evaluation == null ? null : evaluation.getTotalRiskPoints();
		LocalDateTime lastUpdated = user.getUpdatedAt();
		return new CustomerSummaryView(
			new BankOfficerCustomerSummaryResponse(
				user.getUserId(),
				customer.getCustomerCode(),
				(safe(user.getFirstName()) + " " + safe(user.getLastName())).trim(),
				safe(user.getNic()),
				safe(user.getEmail()),
				safe(user.getPhone()),
				safe(user.getStatus()),
				riskLevel,
				creditScore,
				lastUpdated == null ? null : lastUpdated.toString()
			),
			lastUpdated
		);
	}

	private Map<Long, BankCreditEvaluation> loadLatestEvaluations(Long officerId) {
		// Retrieve evaluations ordered by customer id and newest createdAt so
		// we can pick the first entry per customer as the latest evaluation.
		Map<Long, BankCreditEvaluation> latestEvaluations = new LinkedHashMap<>();
		for (BankCreditEvaluation evaluation : bankCreditEvaluationRepository
				.findAllByBankCustomer_Officer_OfficerIdOrderByBankCustomer_BankCustomerIdAscCreatedAtDesc(officerId)) {
			Long bankCustomerId = evaluation.getBankCustomer().getBankCustomerId();
			latestEvaluations.putIfAbsent(bankCustomerId, evaluation);
		}
		return latestEvaluations;
	}

	private boolean matchesSearch(BankOfficerCustomerSummaryResponse summary, String normalizedSearch) {
		// Server-side search: combine the most common identity fields into a
		// single haystack and perform a case-insensitive contains check.
		if (normalizedSearch.isBlank()) {
			return true;
		}
		String haystack = String.join(" ",
			safe(summary.customerId()),
			safe(summary.fullName()),
			safe(summary.nic()),
			safe(summary.email()),
			safe(summary.phone())
		).toLowerCase(Locale.ROOT);
		return haystack.contains(normalizedSearch);
	}

	private boolean matchesStatus(BankOfficerCustomerSummaryResponse summary, String normalizedStatus) {
		// Simple equality match on the canonical status value.
		return normalizedStatus.isBlank() || safe(summary.status()).equalsIgnoreCase(normalizedStatus);
	}

	private boolean matchesRiskLevel(BankOfficerCustomerSummaryResponse summary, String normalizedRiskLevel) {
		// Risk-level matching is performed against the canonical riskLevel
		// value provided by the credit evaluation service (LOW/MEDIUM/HIGH).
		return normalizedRiskLevel.isBlank() || safe(summary.riskLevel()).equalsIgnoreCase(normalizedRiskLevel);
	}

	private Comparator<CustomerSummaryView> comparatorFor(String normalizedSortBy) {
		return switch (normalizedSortBy) {
			case "updated-asc" -> Comparator.comparing(CustomerSummaryView::updatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
			case "score-desc" -> Comparator.comparing((CustomerSummaryView view) -> scoreValue(view.summary()), Comparator.nullsLast(Comparator.naturalOrder())).reversed();
			case "score-asc" -> Comparator.comparing((CustomerSummaryView view) -> scoreValue(view.summary()), Comparator.nullsLast(Comparator.naturalOrder()));
			case "name-asc" -> Comparator.comparing(view -> safe(view.summary().fullName()), String.CASE_INSENSITIVE_ORDER);
			case "name-desc" -> Comparator.comparing((CustomerSummaryView view) -> safe(view.summary().fullName()), String.CASE_INSENSITIVE_ORDER).reversed();
			default -> Comparator.comparing(CustomerSummaryView::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
		};
	}

	private Integer scoreValue(BankOfficerCustomerSummaryResponse summary) {
		return summary.creditScore() == null ? Integer.MIN_VALUE : summary.creditScore();
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}

	private record CustomerSummaryView(BankOfficerCustomerSummaryResponse summary, LocalDateTime updatedAt) {}
}
