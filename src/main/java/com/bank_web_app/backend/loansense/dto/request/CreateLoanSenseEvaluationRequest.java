package com.bank_web_app.backend.loansense.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateLoanSenseEvaluationRequest(
	@Size(max = 4, message = "You can request at most 4 loan types per evaluation.")
	List<@Valid LoanSenseLoanInputRequest> loans
) {
}
