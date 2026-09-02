package com.bank_web_app.backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank_web_app.backend.admin.entity.Branch;
import com.bank_web_app.backend.admin.repository.BranchRepository;
import com.bank_web_app.backend.auth.repository.PasswordResetTokenRepository;
import com.bank_web_app.backend.auth.repository.RefreshTokenRepository;
import com.bank_web_app.backend.auth.service.OfficerActivationService;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerFinancialRecordRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.creditlens.repository.BankCreditEvaluationRepository;
import com.bank_web_app.backend.notification.event.NotificationEventPublisher;
import com.bank_web_app.backend.notification.repository.NotificationRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.RoleRepository;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminBankOfficerServiceTest {

	@Mock private BankOfficerRepository bankOfficerRepository;
	@Mock private BranchRepository branchRepository;
	@Mock private UserRepository userRepository;
	@Mock private PasswordResetTokenRepository passwordResetTokenRepository;
	@Mock private RefreshTokenRepository refreshTokenRepository;
	@Mock private NotificationRepository notificationRepository;
	@Mock private BankCustomerRepository bankCustomerRepository;
	@Mock private BankCustomerFinancialRecordRepository bankCustomerFinancialRecordRepository;
	@Mock private BankCreditEvaluationRepository bankCreditEvaluationRepository;
	@Mock private AuditLogService auditLogService;
	@Mock private NotificationEventPublisher notificationEventPublisher;
	@Mock private RoleRepository roleRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private OfficerActivationService officerActivationService;

	private AdminBankOfficerService service;

	@BeforeEach
	void setUp() {
		service = new AdminBankOfficerService(
			bankOfficerRepository,
			branchRepository,
			userRepository,
			passwordResetTokenRepository,
			refreshTokenRepository,
			notificationRepository,
			bankCustomerRepository,
			bankCustomerFinancialRecordRepository,
			bankCreditEvaluationRepository,
			auditLogService,
			notificationEventPublisher,
			roleRepository,
			passwordEncoder,
			officerActivationService
		);
	}

	@Test
	void deletesActivationTokensBeforeDeletingPendingOfficerAccount() {
		User user = new User();
		user.setUserId(90L);
		user.setFirstName("Pending");
		user.setLastName("Officer");
		user.setStatus("PENDING_ACTIVATION");

		Branch branch = new Branch();
		branch.setBranchId(2L);
		branch.setBranchName("Main Branch");

		BankOfficer officer = new BankOfficer();
		officer.setOfficerId(28L);
		officer.setEmployeeCode("EMP-BO-00028");
		officer.setUser(user);
		officer.setBranch(branch);

		when(bankOfficerRepository.findByUser_UserId(90L)).thenReturn(Optional.of(officer));

		var response = service.deletePermanently(90L);

		assertThat(response.userId()).isEqualTo(90L);
		assertThat(response.fullName()).isEqualTo("Pending Officer");
		verify(passwordResetTokenRepository).deleteByUser_UserId(90L);

		InOrder deletionOrder = inOrder(passwordResetTokenRepository, bankOfficerRepository, userRepository);
		deletionOrder.verify(passwordResetTokenRepository).deleteByUser_UserId(90L);
		deletionOrder.verify(bankOfficerRepository).delete(officer);
		deletionOrder.verify(userRepository).delete(user);
	}
}
