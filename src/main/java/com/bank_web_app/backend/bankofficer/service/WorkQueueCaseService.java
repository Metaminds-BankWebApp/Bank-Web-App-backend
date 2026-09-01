package com.bank_web_app.backend.bankofficer.service;

import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.dto.request.WorkQueueCaseStatusRequest;
import com.bank_web_app.backend.bankofficer.dto.response.WorkQueueCaseResponse;
import com.bank_web_app.backend.bankofficer.entity.OfficerWorkQueueCase;
import com.bank_web_app.backend.bankofficer.repository.OfficerWorkQueueCaseRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkQueueCaseService {
	private static final List<String> STATES = List.of("OPEN", "IN_PROGRESS", "COMPLETED", "ESCALATED");
	private final OfficerWorkQueueCaseRepository repository;
	private final BankCustomerRepository customerRepository;
	private final BankOfficerContextService officerContext;
	public WorkQueueCaseService(OfficerWorkQueueCaseRepository repository, BankCustomerRepository customerRepository, BankOfficerContextService officerContext) { this.repository = repository; this.customerRepository = customerRepository; this.officerContext = officerContext; }
	@Transactional(readOnly = true) public List<WorkQueueCaseResponse> getAll() { officerContext.resolveLoggedInBankOfficer(); return repository.findAllByOrderByUpdatedAtDesc().stream().map(this::toResponse).toList(); }
	@Transactional public WorkQueueCaseResponse update(WorkQueueCaseStatusRequest request) {
		var officer = officerContext.resolveLoggedInBankOfficer();
		String type = request.caseType().trim().toUpperCase(Locale.ROOT);
		String status = request.status().trim().toUpperCase(Locale.ROOT);
		if (!List.of("RISK_REVIEW", "PROFILE_COMPLETION").contains(type)) throw new IllegalArgumentException("Unsupported work-queue case type.");
		if (!STATES.contains(status)) throw new IllegalArgumentException("Unsupported work-queue status.");
		var customer = customerRepository.findByUser_UserId(request.userId()).orElseThrow(() -> new IllegalArgumentException("Bank customer not found."));
		var item = repository.findByBankCustomer_BankCustomerIdAndCaseType(customer.getBankCustomerId(), type).orElseGet(OfficerWorkQueueCase::new);
		item.setBankCustomer(customer); item.setCaseType(type); item.setStatus(status); item.setUpdatedByOfficer(officer);
		return toResponse(repository.save(item));
	}
	private WorkQueueCaseResponse toResponse(OfficerWorkQueueCase item) { return new WorkQueueCaseResponse(item.getBankCustomer().getUser().getUserId(), item.getCaseType(), item.getStatus(), item.getUpdatedByOfficer().getOfficerId(), item.getUpdatedAt()); }
}
