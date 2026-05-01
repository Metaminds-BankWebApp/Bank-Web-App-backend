package com.bank_web_app.backend.loansense.dto.response;

import java.util.List;

public record LoanSenseOfficerDashboardResponse(
	int totalCustomers,
	int evaluatedCustomers,
	int eligibleCustomers,
	int partiallyEligibleCustomers,
	int notEligibleCustomers,
	List<LoanSenseOfficerCustomerRowResponse> customers
) {
}
