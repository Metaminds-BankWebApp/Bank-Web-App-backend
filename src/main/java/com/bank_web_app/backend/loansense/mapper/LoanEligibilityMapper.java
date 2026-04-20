package com.bank_web_app.backend.loansense.mapper;

import com.bank_web_app.backend.admin.entity.LoanPolicy;
import com.bank_web_app.backend.admin.entity.RiskAdjustment;
import com.bank_web_app.backend.loansense.dto.response.LoanSenseEvaluationResponse;
import com.bank_web_app.backend.loansense.dto.response.LoanSenseHistoryItemResponse;
import com.bank_web_app.backend.loansense.dto.response.LoanSenseLoanOptionResponse;
import com.bank_web_app.backend.loansense.dto.response.LoanTypeDetailResponse;
import com.bank_web_app.backend.loansense.entity.LoanEligibilityResult;
import com.bank_web_app.backend.loansense.entity.LoanSenseEvaluation;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class LoanEligibilityMapper {

	private static final DateTimeFormatter HISTORY_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH);

	public LoanSenseEvaluationResponse toEvaluationResponse(LoanSenseEvaluation evaluation) {
		List<LoanSenseLoanOptionResponse> loanOptions = evaluation.getResults()
			.stream()
			.sorted(Comparator.comparingInt(result -> loanTypeOrder(result.getLoanType())))
			.map(this::toLoanOptionResponse)
			.toList();

		return new LoanSenseEvaluationResponse(
			evaluation.getLoansenseEvaluationId(),
			evaluation.getBankCustomer().getBankCustomerId(),
			evaluation.getBankRecord().getBankRecordId(),
			evaluation.getBankEvaluation().getBankEvaluationId(),
			evaluation.getMonthlyIncome(),
			evaluation.getTotalExistingLoanEmi(),
			evaluation.getLeasingHirePurchasePayment(),
			evaluation.getCreditCardOutstanding(),
			evaluation.getCreditCardLimit(),
			evaluation.getCreditCardMinPayment(),
			evaluation.getMissedPaymentsCount(),
			evaluation.getTmdo(),
			evaluation.getDbr(),
			evaluation.getMaxAllowedEmi(),
			evaluation.getAvailableEmiCapacity(),
			evaluation.getRiskLevel(),
			toRiskLabel(evaluation.getRiskLevel()),
			evaluation.getRiskMultiplier(),
			evaluation.getOverallStatus(),
			toStatusLabel(evaluation.getOverallStatus()),
			evaluation.getRemarks(),
			evaluation.getCreatedAt(),
			loanOptions
		);
	}

	public LoanTypeDetailResponse toDetailResponse(
		LoanSenseEvaluation evaluation,
		LoanEligibilityResult result,
		LoanPolicy policy,
		RiskAdjustment adjustment
	) {
		int minTenure = resolveMinTenureMonths(result.getLoanType());
		return new LoanTypeDetailResponse(
			evaluation.getLoansenseEvaluationId(),
			result.getLoanResultId(),
			result.getLoanType(),
			toLoanTypeKey(result.getLoanType()),
			toLoanTypeLabel(result.getLoanType()),
			result.getEligibilityStatus(),
			toStatusLabel(result.getEligibilityStatus()),
			result.getRecommendedMaxAmount(),
			result.getEstimatedEmi(),
			result.getInterestRate(),
			minTenure,
			policy == null ? null : policy.getMaxTenureMonths(),
			buildTenureLabel(minTenure, policy == null ? result.getTenureMonths() : policy.getMaxTenureMonths()),
			result.getCustomerAge(),
			evaluation.getMonthlyIncome(),
			evaluation.getTotalExistingLoanEmi(),
			evaluation.getCreditCardMinPayment(),
			evaluation.getLeasingHirePurchasePayment(),
			evaluation.getTmdo(),
			evaluation.getDbr(),
			policy == null ? null : policy.getMaxDbrRatio(),
			evaluation.getMaxAllowedEmi(),
			evaluation.getAvailableEmiCapacity(),
			evaluation.getRiskLevel(),
			toRiskLabel(evaluation.getRiskLevel()),
			evaluation.getRiskMultiplier(),
			adjustment == null ? null : adjustment.getDescription(),
			policy == null ? null : policy.getMinIncomeRequired(),
			policy == null ? null : policy.getMinAge(),
			policy == null ? null : policy.getMaxAge(),
			result.getDecisionReason(),
			evaluation.getCreatedAt()
		);
	}

	public LoanSenseHistoryItemResponse toHistoryItemResponse(LoanSenseEvaluation evaluation, LoanEligibilityResult result) {
		int minTenure = resolveMinTenureMonths(result.getLoanType());
		LocalDateTime evaluationDate = evaluation.getCreatedAt();
		return new LoanSenseHistoryItemResponse(
			evaluation.getLoansenseEvaluationId(),
			result.getLoanResultId(),
			evaluationDate == null ? "" : evaluationDate.format(HISTORY_MONTH_FORMATTER),
			evaluationDate,
			result.getLoanType(),
			toLoanTypeKey(result.getLoanType()),
			toLoanTypeLabel(result.getLoanType()),
			result.getEligibilityStatus(),
			toStatusLabel(result.getEligibilityStatus()),
			result.getRecommendedMaxAmount(),
			result.getTenureMonths(),
			buildTenureLabel(minTenure, result.getTenureMonths()),
			evaluation.getRiskLevel(),
			toRiskLabel(evaluation.getRiskLevel())
		);
	}

	private LoanSenseLoanOptionResponse toLoanOptionResponse(LoanEligibilityResult result) {
		return new LoanSenseLoanOptionResponse(
			result.getLoanResultId(),
			result.getLoanType(),
			toLoanTypeKey(result.getLoanType()),
			toLoanTypeLabel(result.getLoanType()),
			result.getEligibilityStatus(),
			toStatusLabel(result.getEligibilityStatus()),
			result.getRecommendedMaxAmount(),
			result.getEstimatedEmi(),
			result.getInterestRate(),
			result.getTenureMonths(),
			buildTenureLabel(resolveMinTenureMonths(result.getLoanType()), result.getTenureMonths()),
			result.getCustomerAge(),
			result.getDecisionReason(),
			result.getCreatedAt()
		);
	}

	private String toLoanTypeLabel(String loanType) {
		return switch (normalize(loanType)) {
			case "PERSONAL" -> "Personal Loan";
			case "VEHICLE" -> "Vehicle Loan";
			case "EDUCATION" -> "Education Loan";
			case "HOUSING" -> "Housing Loan";
			default -> toTitleCase(loanType);
		};
	}

	private String toLoanTypeKey(String loanType) {
		return normalize(loanType).toLowerCase(Locale.ROOT);
	}

	private String toStatusLabel(String status) {
		return switch (normalize(status)) {
			case "ELIGIBLE" -> "Eligible";
			case "PARTIALLY_ELIGIBLE" -> "Partially Eligible";
			case "NOT_ELIGIBLE" -> "Not Eligible";
			default -> toTitleCase(status);
		};
	}

	private String toRiskLabel(String riskLevel) {
		return switch (normalize(riskLevel)) {
			case "LOW" -> "Low Risk";
			case "MEDIUM" -> "Medium Risk";
			case "HIGH" -> "High Risk";
			default -> toTitleCase(riskLevel);
		};
	}

	private String buildTenureLabel(Integer minTenure, Integer maxTenure) {
		if (minTenure == null && maxTenure == null) {
			return "";
		}
		if (minTenure == null) {
			return maxTenure + " months";
		}
		if (maxTenure == null) {
			return minTenure + " months";
		}
		return minTenure + "-" + maxTenure + " months";
	}

	private int resolveMinTenureMonths(String loanType) {
		return switch (normalize(loanType)) {
			case "PERSONAL" -> 12;
			case "VEHICLE" -> 24;
			case "EDUCATION" -> 36;
			case "HOUSING" -> 60;
			default -> 12;
		};
	}

	private int loanTypeOrder(String loanType) {
		return switch (normalize(loanType)) {
			case "PERSONAL" -> 0;
			case "VEHICLE" -> 1;
			case "EDUCATION" -> 2;
			case "HOUSING" -> 3;
			default -> 99;
		};
	}

	private String toTitleCase(String value) {
		String normalized = normalize(value).toLowerCase(Locale.ROOT).replace('_', ' ');
		if (normalized.isBlank()) {
			return "";
		}
		String[] words = normalized.split("\\s+");
		StringBuilder builder = new StringBuilder();
		for (String word : words) {
			if (builder.length() > 0) {
				builder.append(' ');
			}
			builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return builder.toString();
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
	}
}
