package com.bank_web_app.backend.bankofficer.dto.request;

public record BankOfficerCustomerFilterRequest(
	String search,
	String status,
	String riskLevel,
	String sortBy
) {
}