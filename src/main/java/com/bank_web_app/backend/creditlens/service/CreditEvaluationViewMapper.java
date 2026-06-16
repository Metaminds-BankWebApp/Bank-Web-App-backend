package com.bank_web_app.backend.creditlens.service;

import com.bank_web_app.backend.creditlens.entity.BankCreditEvaluation;
import com.bank_web_app.backend.creditlens.entity.SelfCreditEvaluation;
import org.springframework.stereotype.Component;

@Component
public class CreditEvaluationViewMapper {

	// Converts a public self evaluation entity into a shared dashboard view.
	EvaluationView toView(SelfCreditEvaluation evaluation) {
		return new EvaluationView(
			evaluation.getSelfEvaluationId(),
			evaluation.getPublicRecord().getRecordId(),
			"PUBLIC",
			"Self Assessment",
			evaluation.getTotalRiskPoints(),
			evaluation.getRiskLevel(),
			evaluation.getTotalMonthlyIncome(),
			evaluation.getTotalMonthlyDebtPayment(),
			evaluation.getTotalCardLimit(),
			evaluation.getTotalCardOutstanding(),
			evaluation.getDtiRatio(),
			evaluation.getCreditUtilizationRatio(),
			evaluation.getActiveFacilitiesCount(),
			evaluation.getMissedPaymentsCount(),
			evaluation.getPaymentHistoryPoints(),
			evaluation.getDtiPoints(),
			evaluation.getUtilizationPoints(),
			evaluation.getIncomeStabilityPoints(),
			evaluation.getExposurePoints(),
			evaluation.getCreatedAt()
		);
	}

	// Converts a bank evaluation entity into a shared dashboard view.
	EvaluationView toView(BankCreditEvaluation evaluation) {
		return new EvaluationView(
			evaluation.getBankEvaluationId(),
			evaluation.getBankRecord().getBankRecordId(),
			"BANK",
			"Bank Assessment",
			evaluation.getTotalRiskPoints(),
			evaluation.getRiskLevel(),
			evaluation.getTotalMonthlyIncome(),
			evaluation.getTotalMonthlyDebtPayment(),
			evaluation.getTotalCardLimit(),
			evaluation.getTotalCardOutstanding(),
			evaluation.getDtiRatio(),
			evaluation.getCreditUtilizationRatio(),
			evaluation.getActiveFacilitiesCount(),
			evaluation.getMissedPaymentsCount(),
			evaluation.getPaymentHistoryPoints(),
			evaluation.getDtiPoints(),
			evaluation.getUtilizationPoints(),
			evaluation.getIncomeStabilityPoints(),
			evaluation.getExposurePoints(),
			evaluation.getCreatedAt()
		);
	}
}
