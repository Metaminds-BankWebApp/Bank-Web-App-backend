package com.bank_web_app.backend.creditlens.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Optional officer-supplied metadata used when creating a bank-customer CreditLens evaluation.
 */
public record CreateBankCreditEvaluationRequest(
	@Size(max = 30, message = "Evaluation source cannot exceed 30 characters.")
	String evaluationSource,
	@Size(max = 2000, message = "Remarks cannot exceed 2000 characters.")
	String remarks
) {
}
