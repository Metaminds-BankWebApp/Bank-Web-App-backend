package com.bank_web_app.backend.creditlens.mapper;

import com.bank_web_app.backend.creditlens.dto.response.BankCreditAnalysisCustomerRowResponse;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditEvaluationResponse;
import com.bank_web_app.backend.creditlens.dto.response.BankCreditEvaluationSummaryResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditRiskFactorResponse;
import com.bank_web_app.backend.creditlens.dto.response.SelfCreditEvaluationResponse;
import com.bank_web_app.backend.creditlens.dto.response.SelfCreditEvaluationSummaryResponse;
import com.bank_web_app.backend.creditlens.entity.BankCreditEvaluation;
import com.bank_web_app.backend.creditlens.entity.SelfCreditEvaluation;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CreditEvaluationMapper {

	public SelfCreditEvaluationResponse toSelfResponse(SelfCreditEvaluation evaluation) {
		return new SelfCreditEvaluationResponse(
			evaluation.getSelfEvaluationId(),
			evaluation.getPublicCustomer().getPublicCustomerId(),
			evaluation.getPublicRecord().getRecordId(),
			evaluation.getTotalRiskPoints(),
			evaluation.getRiskLevel(),
			toTitleCase(evaluation.getRiskLevel()),
			evaluation.getTotalMonthlyIncome(),
			evaluation.getTotalMonthlyDebtPayment(),
			evaluation.getTotalCardLimit(),
			evaluation.getTotalCardOutstanding(),
			evaluation.getDtiRatio(),
			resolveDtiBand(evaluation.getDtiRatio()),
			evaluation.getCreditUtilizationRatio(),
			resolveUtilizationBand(evaluation.getCreditUtilizationRatio()),
			evaluation.getActiveFacilitiesCount(),
			evaluation.getMissedPaymentsCount(),
			evaluation.getPaymentHistoryPoints(),
			evaluation.getDtiPoints(),
			evaluation.getUtilizationPoints(),
			evaluation.getIncomeStabilityPoints(),
			evaluation.getExposurePoints(),
			evaluation.getReportGenerated(),
			evaluation.getCreatedAt(),
			buildFactors(
				evaluation.getPaymentHistoryPoints(),
				evaluation.getDtiPoints(),
				evaluation.getUtilizationPoints(),
				evaluation.getIncomeStabilityPoints(),
				evaluation.getExposurePoints()
			)
		);
	}

	public SelfCreditEvaluationSummaryResponse toSelfSummary(SelfCreditEvaluation evaluation) {
		return new SelfCreditEvaluationSummaryResponse(
			evaluation.getSelfEvaluationId(),
			evaluation.getPublicCustomer().getPublicCustomerId(),
			evaluation.getPublicRecord().getRecordId(),
			evaluation.getTotalRiskPoints(),
			evaluation.getRiskLevel(),
			toTitleCase(evaluation.getRiskLevel()),
			evaluation.getCreatedAt()
		);
	}

	public BankCreditEvaluationResponse toBankResponse(BankCreditEvaluation evaluation) {
		return new BankCreditEvaluationResponse(
			evaluation.getBankEvaluationId(),
			evaluation.getBankCustomer().getBankCustomerId(),
			evaluation.getBankRecord().getBankRecordId(),
			evaluation.getEvaluatedByOfficer().getOfficerId(),
			evaluation.getEvaluationSource(),
			evaluation.getRemarks(),
			evaluation.getTotalRiskPoints(),
			evaluation.getRiskLevel(),
			toTitleCase(evaluation.getRiskLevel()),
			evaluation.getTotalMonthlyIncome(),
			evaluation.getTotalMonthlyDebtPayment(),
			evaluation.getTotalCardLimit(),
			evaluation.getTotalCardOutstanding(),
			evaluation.getDtiRatio(),
			resolveDtiBand(evaluation.getDtiRatio()),
			evaluation.getCreditUtilizationRatio(),
			resolveUtilizationBand(evaluation.getCreditUtilizationRatio()),
			evaluation.getActiveFacilitiesCount(),
			evaluation.getMissedPaymentsCount(),
			evaluation.getPaymentHistoryPoints(),
			evaluation.getDtiPoints(),
			evaluation.getUtilizationPoints(),
			evaluation.getIncomeStabilityPoints(),
			evaluation.getExposurePoints(),
			evaluation.getReportGenerated(),
			evaluation.getCreatedAt(),
			buildFactors(
				evaluation.getPaymentHistoryPoints(),
				evaluation.getDtiPoints(),
				evaluation.getUtilizationPoints(),
				evaluation.getIncomeStabilityPoints(),
				evaluation.getExposurePoints()
			)
		);
	}

	public BankCreditEvaluationSummaryResponse toBankSummary(BankCreditEvaluation evaluation) {
		return new BankCreditEvaluationSummaryResponse(
			evaluation.getBankEvaluationId(),
			evaluation.getBankCustomer().getBankCustomerId(),
			evaluation.getBankRecord().getBankRecordId(),
			evaluation.getEvaluatedByOfficer().getOfficerId(),
			evaluation.getEvaluationSource(),
			evaluation.getTotalRiskPoints(),
			evaluation.getRiskLevel(),
			toTitleCase(evaluation.getRiskLevel()),
			evaluation.getCreatedAt()
		);
	}

	public BankCreditAnalysisCustomerRowResponse toDashboardRow(BankCreditEvaluation evaluation) {
		String firstName = safe(evaluation.getBankCustomer().getUser().getFirstName());
		String lastName = safe(evaluation.getBankCustomer().getUser().getLastName());
		return new BankCreditAnalysisCustomerRowResponse(
			evaluation.getBankCustomer().getBankCustomerId(),
			evaluation.getBankCustomer().getCustomerCode(),
			(firstName + " " + lastName).trim(),
			safe(evaluation.getBankCustomer().getUser().getEmail()),
			safe(evaluation.getBankCustomer().getUser().getPhone()),
			evaluation.getBankEvaluationId(),
			evaluation.getTotalRiskPoints(),
			evaluation.getRiskLevel(),
			toTitleCase(evaluation.getRiskLevel()),
			evaluation.getCreatedAt()
		);
	}

	private List<CreditRiskFactorResponse> buildFactors(
		Integer paymentHistoryPoints,
		Integer dtiPoints,
		Integer utilizationPoints,
		Integer incomeStabilityPoints,
		Integer exposurePoints
	) {
		return List.of(
			new CreditRiskFactorResponse("Payment History", paymentHistoryPoints, 30),
			new CreditRiskFactorResponse("Debt-to-Income", dtiPoints, 25),
			new CreditRiskFactorResponse("Credit Utilization", utilizationPoints, 20),
			new CreditRiskFactorResponse("Income Stability", incomeStabilityPoints, 15),
			new CreditRiskFactorResponse("Active Facilities", exposurePoints, 10)
		);
	}

	private String resolveDtiBand(BigDecimal dtiRatio) {
		BigDecimal ratio = sanitizeRatio(dtiRatio);
		if (ratio.compareTo(new BigDecimal("0.30")) <= 0) {
			return "Low";
		}
		if (ratio.compareTo(new BigDecimal("0.50")) <= 0) {
			return "Medium";
		}
		return "High";
	}

	private String resolveUtilizationBand(BigDecimal utilizationRatio) {
		BigDecimal ratio = sanitizeRatio(utilizationRatio);
		if (ratio.compareTo(new BigDecimal("0.40")) <= 0) {
			return "Low";
		}
		if (ratio.compareTo(new BigDecimal("0.70")) <= 0) {
			return "Medium";
		}
		return "High";
	}

	private BigDecimal sanitizeRatio(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private String toTitleCase(String value) {
		String normalized = safe(value).toLowerCase();
		if (normalized.isBlank()) {
			return "";
		}
		return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}
