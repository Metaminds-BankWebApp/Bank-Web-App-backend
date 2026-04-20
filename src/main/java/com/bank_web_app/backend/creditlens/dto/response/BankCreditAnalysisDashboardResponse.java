package com.bank_web_app.backend.creditlens.dto.response;

import java.util.List;

public record BankCreditAnalysisDashboardResponse(
	int totalCustomers,
	int lowRiskCount,
	int mediumRiskCount,
	int highRiskCount,
	List<BankCreditAnalysisCustomerRowResponse> customers
) {
}
