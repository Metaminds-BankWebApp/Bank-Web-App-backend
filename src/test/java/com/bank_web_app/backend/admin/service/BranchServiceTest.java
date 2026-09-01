package com.bank_web_app.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank_web_app.backend.admin.dto.request.BranchRequest;
import com.bank_web_app.backend.admin.dto.response.BranchResponse;
import com.bank_web_app.backend.admin.entity.Branch;
import com.bank_web_app.backend.admin.entity.BranchStatus;
import com.bank_web_app.backend.admin.repository.BranchRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.common.exception.DuplicateFieldsException;
import com.bank_web_app.backend.notification.event.NotificationEventPublisher;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

	@Mock private BranchRepository branchRepository;
	@Mock private BankOfficerRepository bankOfficerRepository;
	@Mock private BankCustomerRepository bankCustomerRepository;
	@Mock private UserRepository userRepository;
	@Mock private AuditLogService auditLogService;
	@Mock private NotificationEventPublisher notificationEventPublisher;

	private BranchService branchService;

	@BeforeEach
	void setUp() {
		branchService = new BranchService(
			branchRepository,
			bankOfficerRepository,
			bankCustomerRepository,
			userRepository,
			auditLogService,
			notificationEventPublisher
		);
	}

	@Test
	void createsBranchWithNextGeneratedCodeAndAuditLog() {
		when(branchRepository.findMaxBranchCodeNumericValue()).thenReturn(6L);
		when(branchRepository.existsByBranchCode("BR-0007")).thenReturn(false);
		when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> {
			Branch branch = invocation.getArgument(0);
			branch.setBranchId(7L);
			return branch;
		});

		BranchResponse response = branchService.create(request("Colombo Main", "0112000001", "active"));

		assertThat(response.branchId()).isEqualTo(7L);
		assertThat(response.branchCode()).isEqualTo("BR-0007");
		assertThat(response.branchName()).isEqualTo("Colombo Main");
		assertThat(response.status()).isEqualTo("ACTIVE");
		verify(auditLogService).logAction(
			anyString(),
			anyString(),
			anyString(),
			anyString(),
			anyString(),
			anyString()
		);
	}

	@Test
	void rejectsBranchPhoneAlreadyUsedInSystem() {
		when(branchRepository.existsByBranchPhone("0112000001")).thenReturn(true);

		assertThatThrownBy(() -> branchService.create(request("Colombo Main", "0112000001", "ACTIVE")))
			.isInstanceOfSatisfying(DuplicateFieldsException.class, exception ->
				assertThat(exception.getFieldErrors()).containsKey("branchPhone")
			);
		verify(branchRepository, never()).save(any());
	}

	@Test
	void refusesToDeleteBranchThatStillHasOfficers() {
		Branch branch = branch(7L);
		when(branchRepository.findById(7L)).thenReturn(Optional.of(branch));
		when(bankOfficerRepository.existsByBranch_BranchId(7L)).thenReturn(true);

		assertThatThrownBy(() -> branchService.delete(7L))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("linked to officers or customers");
		verify(branchRepository, never()).delete(any());
	}

	private BranchRequest request(String name, String phone, String status) {
		return new BranchRequest(
			name,
			"colombo.main@primecore.com",
			phone,
			"No 1, Main Street, Colombo",
			status
		);
	}

	private Branch branch(Long id) {
		Branch branch = new Branch();
		branch.setBranchId(id);
		branch.setBranchCode("BR-0007");
		branch.setBranchName("Colombo Main");
		branch.setStatus(BranchStatus.ACTIVE);
		return branch;
	}
}
