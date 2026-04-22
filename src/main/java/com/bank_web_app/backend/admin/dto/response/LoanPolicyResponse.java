package com.bank_web_app.backend.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "LoanPolicyResponse", description = "LoanSense loan policy response")
public record LoanPolicyResponse(
	@Schema(example = "1")
	Long policyId,
	@Schema(example = "PERSONAL")
	String loanType,
	@Schema(example = "Personal Loan")
	String loanTypeLabel,
	@Schema(example = "0.4000")
	BigDecimal maxDbrRatio,
	@Schema(example = "17.00")
	BigDecimal baseInterestRate,
	@Schema(example = "60")
	Integer maxTenureMonths,
	@Schema(example = "21")
	Integer minAge,
	@Schema(example = "60")
	Integer maxAge,
	@Schema(example = "80.00")
	BigDecimal maxFinancePercentage,
	@Schema(example = "50000.00")
	BigDecimal minIncomeRequired,
	@Schema(example = "ACTIVE")
	String status,
	@Schema(example = "2026-04-20T12:00:00")
	String updatedAt
) {
}
