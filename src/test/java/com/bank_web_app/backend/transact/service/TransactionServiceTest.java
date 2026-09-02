package com.bank_web_app.backend.transact.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank_web_app.backend.bankcustomer.entity.Account;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.AccountRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.common.email.EmailDeliveryException;
import com.bank_web_app.backend.common.email.EmailService;
import com.bank_web_app.backend.notification.event.NotificationEventPublisher;
import com.bank_web_app.backend.spendiq.service.ExpenseService;
import com.bank_web_app.backend.transact.dto.request.CreateTransactionRequest;
import com.bank_web_app.backend.transact.dto.request.VerifyTransactionOtpRequest;
import com.bank_web_app.backend.transact.dto.response.TransactionInitiateResponse;
import com.bank_web_app.backend.transact.dto.response.TransactionResponse;
import com.bank_web_app.backend.transact.entity.OtpRecord;
import com.bank_web_app.backend.transact.entity.Transaction;
import com.bank_web_app.backend.transact.repository.BeneficiaryRepository;
import com.bank_web_app.backend.transact.repository.OtpRecordRepository;
import com.bank_web_app.backend.transact.repository.TransactionRepository;
import com.bank_web_app.backend.user.entity.Role;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

	@Mock private TransactionRepository transactionRepository;
	@Mock private OtpRecordRepository otpRecordRepository;
	@Mock private BeneficiaryRepository beneficiaryRepository;
	@Mock private BankCustomerRepository bankCustomerRepository;
	@Mock private AccountRepository accountRepository;
	@Mock private UserRepository userRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private EmailService emailService;
	@Mock private ExpenseService expenseService;
	@Mock private NotificationEventPublisher notificationEventPublisher;

	private TransactionService transactionService;
	private BankCustomer customer;
	private Account senderAccount;
	private Account receiverAccount;

	@BeforeEach
	void setUp() {
		transactionService = new TransactionService(
			transactionRepository,
			otpRecordRepository,
			beneficiaryRepository,
			bankCustomerRepository,
			accountRepository,
			userRepository,
			passwordEncoder,
			emailService,
			expenseService,
			notificationEventPublisher,
			true,
			false,
			""
		);

		Role role = new Role();
		role.setRoleName("BANK_CUSTOMER");
		User user = new User();
		user.setUserId(12L);
		user.setUsername("alice");
		user.setEmail("alice@example.com");
		user.setFirstName("Alice");
		user.setRole(role);

		senderAccount = account(1L, "1000000001", "5000.00");
		receiverAccount = account(2L, "2000000002", "1000.00");
		customer = new BankCustomer();
		customer.setBankCustomerId(30L);
		customer.setUser(user);
		customer.setAccount(senderAccount);

		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken("alice", null, List.of())
		);
		when(userRepository.findByEmail("alice")).thenReturn(Optional.empty());
		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
		when(bankCustomerRepository.findByUser_UserId(12L)).thenReturn(Optional.of(customer));
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void initiatesValidTransferAndStoresHashedOtp() {
		stubAccountsForInitiation();
		when(transactionRepository.existsByReferenceNo(anyString())).thenReturn(false);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
			Transaction transaction = invocation.getArgument(0);
			transaction.setTransactionId(90L);
			return transaction;
		});
		when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");
		when(otpRecordRepository.save(any(OtpRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TransactionInitiateResponse response = transactionService.initiateTransaction(
			request(new BigDecimal("1500.00"))
		);

		assertThat(response.transactionId()).isEqualTo(90L);
		assertThat(response.referenceNo()).startsWith("TXN-");
		assertThat(response.status()).isEqualTo("PENDING_OTP");
		assertThat(response.otpAttemptsRemaining()).isEqualTo(3);

		ArgumentCaptor<OtpRecord> otpCaptor = ArgumentCaptor.forClass(OtpRecord.class);
		verify(otpRecordRepository).save(otpCaptor.capture());
		assertThat(otpCaptor.getValue().getOtpCodeHash()).isEqualTo("hashed-otp");
		assertThat(otpCaptor.getValue().getOtpStatus()).isEqualTo("SENT");
		verify(emailService).sendPlainText(eq("alice@example.com"), anyString(), anyString());
	}

	@Test
	void rejectsTransferThatWouldBreakMinimumRemainingBalance() {
		stubAccountsForInitiation();

		assertThatThrownBy(() -> transactionService.initiateTransaction(request(new BigDecimal("4500.00"))))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Minimum balance");
	}

	@Test
	void marksTransactionFailedWhenOtpEmailCannotBeDelivered() {
		stubAccountsForInitiation();
		when(transactionRepository.existsByReferenceNo(anyString())).thenReturn(false);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
			Transaction transaction = invocation.getArgument(0);
			transaction.setTransactionId(90L);
			return transaction;
		});
		when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");
		when(otpRecordRepository.save(any(OtpRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
		doThrow(new EmailDeliveryException("Delivery failed", new IllegalStateException("SMTP unavailable")))
			.when(emailService)
			.sendPlainText(anyString(), anyString(), anyString());

		TransactionInitiateResponse response = transactionService.initiateTransaction(request(new BigDecimal("1500.00")));

		assertThat(response.status()).isEqualTo("FAILED");
		assertThat(response.message()).contains("could not be delivered");
	}

	@Test
	void marksExpiredPendingOtpTransactionAsFailed() {
		Transaction transaction = pendingTransaction(0);
		OtpRecord otpRecord = otp(transaction, LocalDateTime.now().minusMinutes(1));
		when(otpRecordRepository.findExpiredPendingOtps(eq("PENDING_OTP"), eq("SENT"), any(LocalDateTime.class)))
			.thenReturn(List.of(otpRecord));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(otpRecordRepository.save(any(OtpRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

		int expiredCount = transactionService.expirePendingOtpTransactions();

		assertThat(expiredCount).isEqualTo(1);
		assertThat(transaction.getStatus()).isEqualTo("FAILED");
		assertThat(transaction.getFailureReason()).contains("OTP expired");
		assertThat(otpRecord.getOtpStatus()).isEqualTo("EXPIRED");
	}

	@Test
	void correctOtpCompletesTransferAndUpdatesBothBalances() {
		Transaction transaction = pendingTransaction(2);
		OtpRecord otpRecord = otp(transaction, LocalDateTime.now().plusMinutes(4));
		when(transactionRepository.findByReferenceNoAndBankCustomer_BankCustomerId("TXN-100", 30L))
			.thenReturn(Optional.of(transaction));
		when(otpRecordRepository.findTopByTransaction_TransactionIdOrderByCreatedAtDesc(90L))
			.thenReturn(Optional.of(otpRecord));
		when(passwordEncoder.matches("123456", "hashed-otp")).thenReturn(true);
		when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
		when(accountRepository.findByAccountNumber("2000000002")).thenReturn(Optional.of(receiverAccount));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TransactionResponse response = transactionService.verifyOtp(
			new VerifyTransactionOtpRequest("TXN-100", "123456")
		);

		assertThat(response.status()).isEqualTo("SUCCESS");
		assertThat(response.otpVerified()).isTrue();
		assertThat(senderAccount.getBalance()).isEqualByComparingTo("3500.00");
		assertThat(receiverAccount.getBalance()).isEqualByComparingTo("2500.00");
		assertThat(otpRecord.getOtpStatus()).isEqualTo("VERIFIED");
		verify(accountRepository).save(senderAccount);
		verify(accountRepository).save(receiverAccount);
	}

	@Test
	void thirdIncorrectOtpMarksTransactionAsFailed() {
		Transaction transaction = pendingTransaction(2);
		OtpRecord otpRecord = otp(transaction, LocalDateTime.now().plusMinutes(4));
		when(transactionRepository.findByReferenceNoAndBankCustomer_BankCustomerId("TXN-100", 30L))
			.thenReturn(Optional.of(transaction));
		when(otpRecordRepository.findTopByTransaction_TransactionIdOrderByCreatedAtDesc(90L))
			.thenReturn(Optional.of(otpRecord));
		when(passwordEncoder.matches("000000", "hashed-otp")).thenReturn(false);

		assertThatThrownBy(() -> transactionService.verifyOtp(new VerifyTransactionOtpRequest("TXN-100", "000000")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Maximum of 3 OTP attempts");

		assertThat(transaction.getOtpAttemptCount()).isEqualTo(3);
		assertThat(transaction.getStatus()).isEqualTo("FAILED");
		assertThat(transaction.getFailureReason()).contains("3 incorrect attempts");
		assertThat(otpRecord.getOtpStatus()).isEqualTo("FAILED");
	}

	private void stubAccountsForInitiation() {
		when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
		when(accountRepository.findByAccountNumber("2000000002")).thenReturn(Optional.of(receiverAccount));
	}

	private CreateTransactionRequest request(BigDecimal amount) {
		return new CreateTransactionRequest(
			"2000000002",
			"Bob Customer",
			amount,
			"Test transfer",
			false,
			null
		);
	}

	private Transaction pendingTransaction(int attempts) {
		Transaction transaction = new Transaction();
		transaction.setTransactionId(90L);
		transaction.setBankCustomer(customer);
		transaction.setSenderAccountNo("1000000001");
		transaction.setReceiverAccountNo("2000000002");
		transaction.setReceiverName("Bob Customer");
		transaction.setAmount(new BigDecimal("1500.00"));
		transaction.setRemark("Test transfer");
		transaction.setReferenceNo("TXN-100");
		transaction.setStatus("PENDING_OTP");
		transaction.setOtpVerified(false);
		transaction.setOtpAttemptCount(attempts);
		transaction.setExpenseTrackingEnabled(false);
		return transaction;
	}

	private OtpRecord otp(Transaction transaction, LocalDateTime expiresAt) {
		OtpRecord record = new OtpRecord();
		record.setTransaction(transaction);
		record.setOtpCodeHash("hashed-otp");
		record.setOtpStatus("SENT");
		record.setExpiresAt(expiresAt);
		return record;
	}

	private Account account(Long id, String number, String balance) {
		Account account = new Account();
		account.setAccountId(id);
		account.setAccountNumber(number);
		account.setAccountType("SAVINGS");
		account.setBalance(new BigDecimal(balance));
		account.setStatus("ACTIVE");
		return account;
	}
}
