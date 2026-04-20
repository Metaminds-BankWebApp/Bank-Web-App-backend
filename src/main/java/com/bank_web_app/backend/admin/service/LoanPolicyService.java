package com.bank_web_app.backend.admin.service;

import com.bank_web_app.backend.admin.dto.request.BulkLoanPolicyInterestRateUpdateRequest;
import com.bank_web_app.backend.admin.dto.request.LoanPolicyUpdateRequest;
import com.bank_web_app.backend.admin.dto.response.LoanPolicyResponse;
import com.bank_web_app.backend.admin.entity.LoanPolicy;
import com.bank_web_app.backend.admin.repository.LoanPolicyRepository;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanPolicyService {

	private static final Set<String> SUPPORTED_LOAN_TYPES = Set.of("PERSONAL", "VEHICLE", "EDUCATION", "HOUSING");

	private final LoanPolicyRepository loanPolicyRepository;

	public LoanPolicyService(LoanPolicyRepository loanPolicyRepository) {
		this.loanPolicyRepository = loanPolicyRepository;
	}

	@Transactional(readOnly = true)
	public List<LoanPolicyResponse> getAll() {
		return loanPolicyRepository.findAllByOrderByLoanTypeAsc().stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public LoanPolicyResponse getById(Long policyId) {
		return toResponse(findPolicy(policyId));
	}

	@Transactional
	public LoanPolicyResponse update(Long policyId, LoanPolicyUpdateRequest request) {
		LoanPolicy policy = findPolicy(policyId);
		String loanType = normalizeLoanType(request.loanType());

		loanPolicyRepository.findByLoanType(loanType).ifPresent(existing -> {
			if (!existing.getPolicyId().equals(policyId)) {
				throw new IllegalArgumentException("Loan type is already configured.");
			}
		});

		validateAgeRange(request.minAge(), request.maxAge());
		policy.setLoanType(loanType);
		policy.setMaxDbrRatio(normalizeRatio(request.maxDbrRatio(), "Max DBR ratio is required."));
		policy.setBaseInterestRate(normalizeNonNegative(request.baseInterestRate(), "Base interest rate is required."));
		policy.setMaxTenureMonths(normalizePositive(request.maxTenureMonths(), "Max tenure months is required."));
		policy.setMinAge(request.minAge());
		policy.setMaxAge(request.maxAge());
		policy.setMaxFinancePercentage(normalizeOptionalPercentage(request.maxFinancePercentage()));
		policy.setMinIncomeRequired(normalizeOptionalCurrency(request.minIncomeRequired()));
		policy.setStatus(normalizeStatus(request.status()));

		return toResponse(loanPolicyRepository.save(policy));
	}

	@Transactional
	public List<LoanPolicyResponse> updateInterestRates(BulkLoanPolicyInterestRateUpdateRequest request) {
		if (request == null || request.policies() == null || request.policies().isEmpty()) {
			throw new IllegalArgumentException("At least one policy update is required.");
		}

		Set<Long> policyIds = new LinkedHashSet<>();
		request.policies().forEach(item -> {
			if (!policyIds.add(item.policyId())) {
				throw new IllegalArgumentException("Duplicate policy id found in bulk update payload.");
			}
		});

		Map<Long, LoanPolicy> policyMap = loanPolicyRepository
			.findAllById(policyIds)
			.stream()
			.collect(Collectors.toMap(LoanPolicy::getPolicyId, Function.identity()));

		if (policyMap.size() != policyIds.size()) {
			throw new IllegalArgumentException("One or more loan policies were not found.");
		}

		request.policies().forEach(item -> {
			LoanPolicy policy = policyMap.get(item.policyId());
			policy.setBaseInterestRate(normalizeNonNegative(item.baseInterestRate(), "Base interest rate is required."));
		});

		return loanPolicyRepository
			.saveAll(policyMap.values())
			.stream()
			.sorted((left, right) -> left.getLoanType().compareTo(right.getLoanType()))
			.map(this::toResponse)
			.toList();
	}

	private LoanPolicy findPolicy(Long policyId) {
		return loanPolicyRepository.findById(policyId).orElseThrow(() -> new IllegalArgumentException("Loan policy not found."));
	}

	private LoanPolicyResponse toResponse(LoanPolicy policy) {
		return new LoanPolicyResponse(
			policy.getPolicyId(),
			policy.getLoanType(),
			toLoanTypeLabel(policy.getLoanType()),
			policy.getMaxDbrRatio(),
			policy.getBaseInterestRate(),
			policy.getMaxTenureMonths(),
			policy.getMinAge(),
			policy.getMaxAge(),
			policy.getMaxFinancePercentage(),
			policy.getMinIncomeRequired(),
			policy.getStatus(),
			policy.getUpdatedAt() == null ? null : policy.getUpdatedAt().toString()
		);
	}

	private String normalizeLoanType(String value) {
		String normalized = normalizeRequired(value, "Loan type is required.").toUpperCase(Locale.ROOT);
		if (!SUPPORTED_LOAN_TYPES.contains(normalized)) {
			throw new IllegalArgumentException("Loan type must be PERSONAL, VEHICLE, EDUCATION, or HOUSING.");
		}
		return normalized;
	}

	private String normalizeStatus(String value) {
		String normalized = value == null || value.isBlank() ? "ACTIVE" : value.trim().toUpperCase(Locale.ROOT);
		if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
			throw new IllegalArgumentException("Status must be ACTIVE or INACTIVE.");
		}
		return normalized;
	}

	private BigDecimal normalizeRatio(BigDecimal value, String message) {
		if (value == null) {
			throw new IllegalArgumentException(message);
		}
		if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.ONE) > 0) {
			throw new IllegalArgumentException("Max DBR ratio must be greater than 0 and must not exceed 1.");
		}
		return value;
	}

	private BigDecimal normalizeNonNegative(BigDecimal value, String message) {
		if (value == null) {
			throw new IllegalArgumentException(message);
		}
		if (value.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Numeric values must not be negative.");
		}
		return value;
	}

	private BigDecimal normalizeOptionalPercentage(BigDecimal value) {
		if (value == null) {
			return null;
		}
		if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
			throw new IllegalArgumentException("Max finance percentage must be between 0 and 100.");
		}
		return value;
	}

	private BigDecimal normalizeOptionalCurrency(BigDecimal value) {
		if (value == null) {
			return null;
		}
		if (value.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Minimum income required must not be negative.");
		}
		return value;
	}

	private Integer normalizePositive(Integer value, String message) {
		if (value == null) {
			throw new IllegalArgumentException(message);
		}
		if (value <= 0) {
			throw new IllegalArgumentException("Numeric fields that represent counts or months must be greater than 0.");
		}
		return value;
	}

	private void validateAgeRange(Integer minAge, Integer maxAge) {
		if (minAge == null || maxAge == null) {
			throw new IllegalArgumentException("Minimum and maximum ages are required.");
		}
		if (minAge < 18 || maxAge < 18 || maxAge < minAge) {
			throw new IllegalArgumentException("Maximum age must be greater than or equal to minimum age, and both must be at least 18.");
		}
	}

	private String normalizeRequired(String value, String message) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}

	private String toLoanTypeLabel(String loanType) {
		return switch (loanType == null ? "" : loanType) {
			case "PERSONAL" -> "Personal Loan";
			case "VEHICLE" -> "Vehicle Loan";
			case "EDUCATION" -> "Education Loan";
			case "HOUSING" -> "Housing Loan";
			default -> loanType;
		};
	}
}
