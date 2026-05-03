package com.bank_web_app.backend.creditlens.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Officer dashboard payload containing portfolio counts and customer rows for CreditLens analysis.
 */
@Schema(name = "BankCreditAnalysisDashboardResponse", description = "Dashboard summary for the logged-in bank officer's credit analysis view.")
public record BankCreditAnalysisDashboardResponse(
	@Schema(description = "Total number of customers shown on the dashboard", example = "2450")
	int totalCustomers,
	@Schema(description = "Count of low-risk customers", example = "1820")
	int lowRiskCount,
	@Schema(description = "Count of medium-risk customers", example = "420")
	int mediumRiskCount,
	@Schema(description = "Count of high-risk customers", example = "210")
	int highRiskCount,
	@Schema(description = "Customer rows to render in the dashboard table")
	List<BankCreditAnalysisCustomerRowResponse> customers
) {
}
