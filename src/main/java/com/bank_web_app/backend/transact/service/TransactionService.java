package com.bank_web_app.backend.transact.service;

import com.bank_web_app.backend.bankcustomer.entity.Account;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.AccountRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.common.email.EmailDeliveryException;
import com.bank_web_app.backend.common.email.EmailService;
import com.bank_web_app.backend.spendiq.service.ExpenseService;
import com.bank_web_app.backend.transact.dto.request.CreateBeneficiaryRequest;
import com.bank_web_app.backend.transact.dto.request.CreateTransactionRequest;
import com.bank_web_app.backend.transact.dto.request.ResendTransactionOtpRequest;
import com.bank_web_app.backend.transact.dto.request.UpdateBeneficiaryRequest;
import com.bank_web_app.backend.transact.dto.request.VerifyTransactionOtpRequest;
import com.bank_web_app.backend.transact.dto.response.BeneficiaryResponse;
import com.bank_web_app.backend.transact.dto.response.TransactionInitiateResponse;
import com.bank_web_app.backend.transact.dto.response.TransactionResponse;
import com.bank_web_app.backend.transact.entity.Beneficiary;
import com.bank_web_app.backend.transact.entity.OtpRecord;
import com.bank_web_app.backend.transact.entity.Transaction;
import com.bank_web_app.backend.transact.repository.BeneficiaryRepository;
import com.bank_web_app.backend.transact.repository.OtpRecordRepository;
import com.bank_web_app.backend.transact.repository.TransactionRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransactionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TransactionService.class);

	private static final String ROLE_BANK_CUSTOMER = "BANK_CUSTOMER";
	private static final String STATUS_PENDING_OTP = "PENDING_OTP";
	private static final String STATUS_SUCCESS = "SUCCESS";
	private static final String STATUS_FAILED = "FAILED";
	private static final String OTP_STATUS_SENT = "SENT";
	private static final String OTP_STATUS_VERIFIED = "VERIFIED";
	private static final String OTP_STATUS_EXPIRED = "EXPIRED";
	private static final String OTP_STATUS_FAILED = "FAILED";
	private static final int OTP_LENGTH = 6;
	private static final int OTP_EXPIRY_MINUTES = 5;
	private static final BigDecimal MAX_TRANSFER_AMOUNT = new BigDecimal("100000.00");
	private static final BigDecimal MINIMUM_REMAINING_BALANCE = new BigDecimal("1000.00");
	private static final DateTimeFormatter REFERENCE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	private static final String ALPHA_NUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

	private final TransactionRepository transactionRepository;
	private final OtpRecordRepository otpRecordRepository;
	private final BeneficiaryRepository beneficiaryRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final ExpenseService expenseService;
	private final SecureRandom secureRandom;
	private final boolean otpEmailFailOpenEnabled;
	private final boolean otpPlainLogEnabled;
	private final String otpOverrideRecipientEmail;

	public TransactionService(
		TransactionRepository transactionRepository,
		OtpRecordRepository otpRecordRepository,
		BeneficiaryRepository beneficiaryRepository,
		BankCustomerRepository bankCustomerRepository,
		AccountRepository accountRepository,
		UserRepository userRepository,
		PasswordEncoder passwordEncoder,
		EmailService emailService,
		ExpenseService expenseService,
		@Value("${app.transact.otp.fail-open-enabled:true}") boolean otpEmailFailOpenEnabled,
		@Value("${app.transact.otp.log-plain-enabled:true}") boolean otpPlainLogEnabled,
		@Value("${app.transact.otp.override-recipient-email:}") String otpOverrideRecipientEmail
	) {
		this.transactionRepository = transactionRepository;
		this.otpRecordRepository = otpRecordRepository;
		this.beneficiaryRepository = beneficiaryRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.accountRepository = accountRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.emailService = emailService;
		this.expenseService = expenseService;
		this.secureRandom = new SecureRandom();
		this.otpEmailFailOpenEnabled = otpEmailFailOpenEnabled;
		this.otpPlainLogEnabled = otpPlainLogEnabled;
		this.otpOverrideRecipientEmail = otpOverrideRecipientEmail == null ? "" : otpOverrideRecipientEmail.trim();
	}

	@Transactional
	public TransactionInitiateResponse initiateTransaction(CreateTransactionRequest request) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();

		Account senderAccount = resolveSenderAccountForBankCustomer(bankCustomer);
		String senderAccountNo = normalizeAccountNumber(senderAccount.getAccountNumber());
		String receiverAccountNo = normalizeAccountNumber(request.receiverAccountNo());
		String receiverName = request.receiverName().trim();
		String remark = request.remark().trim();

		if (senderAccountNo.equals(receiverAccountNo)) {
			throw new IllegalArgumentException("Sender and receiver account numbers cannot be the same.");
		}

		Account receiverAccount = accountRepository
			.findByAccountNumber(receiverAccountNo)
			.orElseThrow(() -> new IllegalArgumentException("Account number is invalid"));

		validateActiveAccount(senderAccount, "Sender account is not active.");
		validateActiveAccount(receiverAccount, "Receiver account is not active.");
		validateTransferAmount(request.amount());
		requireSufficientBalanceAndMinimumRemaining(senderAccount, request.amount());

		String referenceNo = generateUniqueReferenceNo();

		Transaction transaction = new Transaction();
		transaction.setBankCustomer(bankCustomer);
		transaction.setSenderAccountNo(senderAccountNo);
		transaction.setReceiverAccountNo(receiverAccountNo);
		transaction.setReceiverName(receiverName);
		transaction.setAmount(request.amount());
		transaction.setRemark(remark);
		transaction.setReferenceNo(referenceNo);
		transaction.setStatus(STATUS_PENDING_OTP);
		transaction.setOtpVerified(Boolean.FALSE);
		transaction.setExpenseTrackingEnabled(Boolean.TRUE.equals(request.expenseTrackingEnabled()));
		transaction.setFailureReason(null);

		transaction = transactionRepository.save(transaction);

		String otpCode = generateOtp();
		String otpRecipientEmail = resolveOtpRecipientEmail(bankCustomer);
		OtpRecord otpRecord = createOtpRecord(transaction, otpRecipientEmail, otpCode, 0);
		boolean otpEmailSent = sendTransferOtpEmail(
			bankCustomer,
			transaction,
			otpRecipientEmail,
			otpCode,
			otpRecord.getExpiresAt(),
			false
		);
		String responseMessage = otpEmailSent
			? "Transaction created. OTP has been issued for verification."
			: "Transaction created. OTP email failed; use development OTP from backend logs.";

		return new TransactionInitiateResponse(
			transaction.getTransactionId(),
			transaction.getReferenceNo(),
			transaction.getStatus(),
			otpRecord.getSentToEmail(),
			otpRecord.getExpiresAt(),
			responseMessage
		);
	}

	@Transactional
	public TransactionResponse verifyOtp(VerifyTransactionOtpRequest request) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		Transaction transaction = transactionRepository
			.findByReferenceNoAndBankCustomer_BankCustomerId(request.referenceNo().trim(), bankCustomer.getBankCustomerId())
			.orElseThrow(
				() ->
					new IllegalArgumentException(
						"Transaction not found for logged-in bank customer. Use the referenceNo returned by /transactions/initiate."
					)
			);

		if (!STATUS_PENDING_OTP.equals(transaction.getStatus())) {
			throw new IllegalArgumentException("Only transactions in PENDING_OTP status can be verified.");
		}

		OtpRecord otpRecord = otpRecordRepository
			.findTopByTransaction_TransactionIdOrderByCreatedAtDesc(transaction.getTransactionId())
			.orElseThrow(() -> new IllegalArgumentException("No OTP record found for this transaction."));

		LocalDateTime now = LocalDateTime.now();
		if (otpRecord.getExpiresAt().isBefore(now)) {
			otpRecord.setOtpStatus(OTP_STATUS_EXPIRED);
			otpRecordRepository.save(otpRecord);
			throw new IllegalArgumentException("OTP has expired. Please request a resend.");
		}

		if (!passwordEncoder.matches(request.otpCode().trim(), otpRecord.getOtpCodeHash())) {
			otpRecord.setOtpStatus(OTP_STATUS_FAILED);
			otpRecordRepository.save(otpRecord);
			throw new IllegalArgumentException("Invalid OTP code.");
		}

		Account senderAccount = resolveSenderAccountForBankCustomer(bankCustomer);
		String senderAccountNo = normalizeAccountNumber(senderAccount.getAccountNumber());
		if (!senderAccountNo.equals(normalizeAccountNumber(transaction.getSenderAccountNo()))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sender account does not belong to logged-in bank customer.");
		}
		Account receiverAccount = accountRepository
			.findByAccountNumber(transaction.getReceiverAccountNo())
			.orElseThrow(() -> new IllegalStateException("Receiver account was not found during verification."));

		validateActiveAccount(senderAccount, "Sender account is not active.");
		validateActiveAccount(receiverAccount, "Receiver account is not active.");

		BigDecimal availableBalance;
		try {
			validateTransferAmount(transaction.getAmount());
			availableBalance = requireSufficientBalanceAndMinimumRemaining(senderAccount, transaction.getAmount());
		} catch (IllegalArgumentException ex) {
			transaction.setStatus(STATUS_FAILED);
			transaction.setOtpVerified(Boolean.FALSE);
			transaction.setFailureReason(toFailureReason(ex.getMessage()));
			transactionRepository.save(transaction);
			throw ex;
		}

		senderAccount.setBalance(availableBalance.subtract(transaction.getAmount()));
		receiverAccount.setBalance(receiverAccount.getBalance().add(transaction.getAmount()));
		accountRepository.save(senderAccount);
		accountRepository.save(receiverAccount);

		otpRecord.setOtpStatus(OTP_STATUS_VERIFIED);
		otpRecord.setVerifiedAt(now);
		otpRecordRepository.save(otpRecord);

		transaction.setStatus(STATUS_SUCCESS);
		transaction.setOtpVerified(Boolean.TRUE);
		transaction.setFailureReason(null);
		transaction.setTransactionDate(now);
		transaction = transactionRepository.save(transaction);

		if (Boolean.TRUE.equals(transaction.getExpenseTrackingEnabled())) {
			// Expense tracking integration point after transfer is confirmed as SUCCESS.
			trackExpenseForSuccessfulTransaction(bankCustomer, transaction);
		}

		return toTransactionResponse(transaction);
	}

	@Transactional
	public TransactionInitiateResponse resendOtp(ResendTransactionOtpRequest request) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		Transaction transaction = transactionRepository
			.findByReferenceNoAndBankCustomer_BankCustomerId(request.referenceNo().trim(), bankCustomer.getBankCustomerId())
			.orElseThrow(
				() ->
					new IllegalArgumentException(
						"Transaction not found for logged-in bank customer. Use the referenceNo returned by /transactions/initiate."
					)
			);

		if (!STATUS_PENDING_OTP.equals(transaction.getStatus())) {
			throw new IllegalArgumentException("OTP can only be resent for PENDING_OTP transactions.");
		}

		OtpRecord previousOtp = otpRecordRepository
			.findTopByTransaction_TransactionIdOrderByCreatedAtDesc(transaction.getTransactionId())
			.orElseThrow(() -> new IllegalArgumentException("No OTP record found for this transaction."));

		if (OTP_STATUS_SENT.equals(previousOtp.getOtpStatus())) {
			previousOtp.setOtpStatus(OTP_STATUS_EXPIRED);
			otpRecordRepository.save(previousOtp);
		}

		String otpCode = generateOtp();
		String otpRecipientEmail = resolveOtpRecipientEmail(bankCustomer);
		OtpRecord otpRecord = createOtpRecord(
			transaction,
			otpRecipientEmail,
			otpCode,
			(previousOtp.getResendCount() == null ? 0 : previousOtp.getResendCount()) + 1
		);
		boolean otpEmailSent = sendTransferOtpEmail(
			bankCustomer,
			transaction,
			otpRecipientEmail,
			otpCode,
			otpRecord.getExpiresAt(),
			true
		);
		String responseMessage = otpEmailSent
			? "OTP has been reissued for this transaction."
			: "OTP reissued, but email failed; use development OTP from backend logs.";

		return new TransactionInitiateResponse(
			transaction.getTransactionId(),
			transaction.getReferenceNo(),
			transaction.getStatus(),
			otpRecord.getSentToEmail(),
			otpRecord.getExpiresAt(),
			responseMessage
		);
	}

	@Transactional(readOnly = true)
	public List<TransactionResponse> getHistory() {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		return transactionRepository
			.findAllByBankCustomer_BankCustomerIdOrderByTransactionDateDesc(bankCustomer.getBankCustomerId())
			.stream()
			.map(this::toTransactionResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public TransactionResponse getByReferenceNo(String referenceNo) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		Transaction transaction = transactionRepository
			.findByReferenceNoAndBankCustomer_BankCustomerId(referenceNo.trim(), bankCustomer.getBankCustomerId())
			.orElseThrow(
				() ->
					new IllegalArgumentException(
						"Transaction not found for logged-in bank customer. Use the referenceNo returned by /transactions/initiate."
					)
			);
		return toTransactionResponse(transaction);
	}

	@Transactional
	public BeneficiaryResponse createBeneficiary(CreateBeneficiaryRequest request) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		Long bankCustomerId = bankCustomer.getBankCustomerId();

		String accountNo = normalizeAccountNumber(request.beneficiaryAccountNo());
		if (accountNo.equals(normalizeAccountNumber(bankCustomer.getAccount().getAccountNumber()))) {
			throw new IllegalArgumentException("Beneficiary account cannot be the same as sender account.");
		}
		Account beneficiaryAccount = accountRepository
			.findByAccountNumber(accountNo)
			.orElseThrow(() -> new IllegalArgumentException("Account number not found"));
		validateActiveAccount(beneficiaryAccount, "Beneficiary account is not active.");
		ensureNoDuplicateBeneficiary(bankCustomerId, accountNo, null);

		Beneficiary beneficiary = new Beneficiary();
		beneficiary.setBankCustomer(bankCustomer);
		beneficiary.setBeneficiaryAccountNo(accountNo);
		beneficiary.setNickName(request.nickName().trim());
		beneficiary.setRemark(request.remark().trim());
		beneficiary = beneficiaryRepository.save(beneficiary);

		return toBeneficiaryResponse(beneficiary);
	}

	@Transactional
	public BeneficiaryResponse updateBeneficiary(Long beneficiaryId, UpdateBeneficiaryRequest request) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		Long bankCustomerId = bankCustomer.getBankCustomerId();
		Beneficiary beneficiary = beneficiaryRepository
			.findByBeneficiaryIdAndBankCustomer_BankCustomerId(beneficiaryId, bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Beneficiary was not found for this bank customer."));

		String accountNo = normalizeAccountNumber(request.beneficiaryAccountNo());
		if (accountNo.equals(normalizeAccountNumber(bankCustomer.getAccount().getAccountNumber()))) {
			throw new IllegalArgumentException("Beneficiary account cannot be the same as sender account.");
		}
		Account beneficiaryAccount = accountRepository
			.findByAccountNumber(accountNo)
			.orElseThrow(() -> new IllegalArgumentException("Account number not found"));
		validateActiveAccount(beneficiaryAccount, "Beneficiary account is not active.");
		ensureNoDuplicateBeneficiary(bankCustomerId, accountNo, beneficiaryId);

		beneficiary.setBeneficiaryAccountNo(accountNo);
		beneficiary.setNickName(request.nickName().trim());
		beneficiary.setRemark(request.remark().trim());
		beneficiary = beneficiaryRepository.save(beneficiary);

		return toBeneficiaryResponse(beneficiary);
	}

	@Transactional(readOnly = true)
	public List<BeneficiaryResponse> getBeneficiaries() {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		return beneficiaryRepository
			.findAllByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomer.getBankCustomerId())
			.stream()
			.map(this::toBeneficiaryResponse)
			.toList();
	}

	@Transactional
	public void deleteBeneficiary(Long beneficiaryId) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		Beneficiary beneficiary = beneficiaryRepository
			.findByBeneficiaryIdAndBankCustomer_BankCustomerId(beneficiaryId, bankCustomer.getBankCustomerId())
			.orElseThrow(() -> new IllegalArgumentException("Beneficiary was not found for this bank customer."));
		beneficiaryRepository.delete(beneficiary);
	}

	private OtpRecord createOtpRecord(Transaction transaction, String sentToEmail, String plainOtpCode, int resendCount) {
		OtpRecord otpRecord = new OtpRecord();
		otpRecord.setTransaction(transaction);
		otpRecord.setOtpCodeHash(passwordEncoder.encode(plainOtpCode));
		otpRecord.setSentToEmail(sentToEmail);
		otpRecord.setOtpStatus(OTP_STATUS_SENT);
		otpRecord.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
		otpRecord.setResendCount(resendCount);
		return otpRecordRepository.save(otpRecord);
	}

	private boolean sendTransferOtpEmail(
		BankCustomer bankCustomer,
		Transaction transaction,
		String toEmail,
		String otpCode,
		LocalDateTime expiresAt,
		boolean resend
	) {
		String subject = resend
			? "Primecore transfer OTP (resent)"
			: "Primecore transfer OTP";
		String customerName = resolveDisplayName(bankCustomer.getUser());
		String body = buildOtpEmailBody(customerName, otpCode, transaction, expiresAt, resend);
		try {
			emailService.sendPlainText(toEmail, subject, body);
			return true;
		} catch (EmailDeliveryException ex) {
			if (!otpEmailFailOpenEnabled) {
				throw ex;
			}
			LOGGER.warn("OTP email delivery failed for transaction reference {}: {}", transaction.getReferenceNo(), ex.getMessage());
			if (otpPlainLogEnabled) {
				LOGGER.info(
					"DEV OTP fallback - transactionRef={} transactionId={} otpCode={} expiresAt={}",
					transaction.getReferenceNo(),
					transaction.getTransactionId(),
					otpCode,
					expiresAt
				);
			}
			return false;
		}
	}

	private String resolveOtpRecipientEmail(BankCustomer bankCustomer) {
		if (!otpOverrideRecipientEmail.isBlank()) {
			LOGGER.debug("Using APP_TRANSACT_OTP_OVERRIDE_RECIPIENT_EMAIL for OTP delivery.");
			return otpOverrideRecipientEmail;
		}
		if (bankCustomer == null || bankCustomer.getUser() == null) {
			throw new IllegalArgumentException("Logged-in bank customer email is required for OTP delivery.");
		}
		String customerEmail = bankCustomer.getUser().getEmail() == null ? "" : bankCustomer.getUser().getEmail().trim();
		if (customerEmail.isBlank()) {
			throw new IllegalArgumentException("Logged-in bank customer email is required for OTP delivery.");
		}
		if (customerEmail.toLowerCase(Locale.ROOT).endsWith(".local")) {
			LOGGER.warn(
				"OTP recipient email {} appears non-routable (.local). Configure APP_TRANSACT_OTP_OVERRIDE_RECIPIENT_EMAIL for local testing.",
				customerEmail
			);
		}
		return customerEmail;
	}

	private String buildOtpEmailBody(
		String customerName,
		String otpCode,
		Transaction transaction,
		LocalDateTime expiresAt,
		boolean resend
	) {
		String greeting = customerName.isBlank() ? "Customer" : customerName;
		String transferAmount = transaction.getAmount() == null ? "0.00" : transaction.getAmount().toPlainString();
		String receiverAccountNo = transaction.getReceiverAccountNo() == null ? "" : transaction.getReceiverAccountNo();
		String receiverName = transaction.getReceiverName() == null ? "" : transaction.getReceiverName();
		String referenceNo = transaction.getReferenceNo() == null ? "" : transaction.getReferenceNo();

		StringBuilder sb = new StringBuilder();
		sb.append("Dear ").append(greeting).append(",\n\n");
		if (resend) {
			sb.append("Your new OTP for the transfer request is below.\n\n");
		} else {
			sb.append("Use the OTP below to verify your transfer request.\n\n");
		}
		sb.append("OTP: ").append(otpCode).append('\n');
		sb.append("Expires at: ").append(expiresAt).append(" (valid for ").append(OTP_EXPIRY_MINUTES).append(" minutes)\n\n");
		sb.append("Transaction details:\n");
		sb.append("- Transaction ID: ").append(transaction.getTransactionId()).append('\n');
		sb.append("- Reference No: ").append(referenceNo).append('\n');
		sb.append("- Receiver account: ").append(receiverAccountNo).append('\n');
		sb.append("- Receiver name: ").append(receiverName).append('\n');
		sb.append("- Amount: Rs. ").append(transferAmount).append("\n\n");
		sb.append("If you did not request this transfer, contact Primecore support immediately.\n\n");
		sb.append("Primecore Transact");
		return sb.toString();
	}

	private String resolveDisplayName(User user) {
		if (user == null) {
			return "";
		}
		String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
		String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
		String fullName = (firstName + " " + lastName).trim();
		if (!fullName.isBlank()) {
			return fullName;
		}
		String username = user.getUsername() == null ? "" : user.getUsername().trim();
		if (!username.isBlank()) {
			return username;
		}
		return user.getEmail() == null ? "" : user.getEmail().trim();
	}

	private String generateUniqueReferenceNo() {
		for (int i = 0; i < 20; i += 1) {
			String candidate = "TXN-" + LocalDateTime.now().format(REFERENCE_TIME_FORMAT) + "-" + randomAlphaNumeric(6);
			if (!transactionRepository.existsByReferenceNo(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("Unable to generate unique transaction reference number.");
	}

	private String randomAlphaNumeric(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i += 1) {
			sb.append(ALPHA_NUM.charAt(secureRandom.nextInt(ALPHA_NUM.length())));
		}
		return sb.toString();
	}

	private String generateOtp() {
		int value = secureRandom.nextInt(1_000_000);
		return String.format(Locale.ROOT, "%0" + OTP_LENGTH + "d", value);
	}

	private void validateActiveAccount(Account account, String message) {
		String status = account.getStatus() == null ? "" : account.getStatus().trim().toUpperCase(Locale.ROOT);
		if (!"ACTIVE".equals(status)) {
			throw new IllegalArgumentException(message);
		}
	}

	private void validateTransferAmount(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Amount must be greater than 0.");
		}
		if (amount.compareTo(MAX_TRANSFER_AMOUNT) > 0) {
			throw new IllegalArgumentException("Transaction amount must not exceed Rs. 100,000.00.");
		}
	}

	private BigDecimal requireSufficientBalanceAndMinimumRemaining(Account senderAccount, BigDecimal amount) {
		BigDecimal availableBalance = senderAccount.getBalance();
		if (availableBalance == null || availableBalance.compareTo(amount) < 0) {
			throw new IllegalArgumentException("Insufficient balance to complete this transaction.");
		}
		BigDecimal remainingBalance = availableBalance.subtract(amount);
		if (remainingBalance.compareTo(MINIMUM_REMAINING_BALANCE) < 0) {
			throw new IllegalArgumentException("Minimum balance of Rs. 1,000.00 must remain after transfer.");
		}
		return availableBalance;
	}

	private String toFailureReason(String message) {
		if (message == null || message.isBlank()) {
			return "Transaction failed.";
		}
		String trimmed = message.trim();
		return trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
	}

	private String normalizeAccountNumber(String accountNo) {
		return accountNo == null ? "" : accountNo.replaceAll("\\s+", "").trim();
	}

	private Account resolveSenderAccountForBankCustomer(BankCustomer bankCustomer) {
		if (bankCustomer == null || bankCustomer.getAccount() == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sender account is not linked to logged-in bank customer.");
		}
		Long senderAccountId = bankCustomer.getAccount().getAccountId();
		if (senderAccountId == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sender account is not linked to logged-in bank customer.");
		}
		Account senderAccount = accountRepository
			.findById(senderAccountId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Sender account is not linked to logged-in bank customer."));
		String senderAccountNo = normalizeAccountNumber(senderAccount.getAccountNumber());
		if (senderAccountNo.isBlank()) {
			throw new IllegalStateException("Sender account number is invalid for logged-in bank customer.");
		}
		return senderAccount;
	}

	private void ensureNoDuplicateBeneficiary(Long bankCustomerId, String accountNo, Long beneficiaryIdToIgnore) {
		boolean duplicateExists = beneficiaryIdToIgnore == null
			? beneficiaryRepository.existsByBankCustomer_BankCustomerIdAndBeneficiaryAccountNo(bankCustomerId, accountNo)
			: beneficiaryRepository.existsByBankCustomer_BankCustomerIdAndBeneficiaryAccountNoAndBeneficiaryIdNot(
				bankCustomerId,
				accountNo,
				beneficiaryIdToIgnore
			);
		if (duplicateExists) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Beneficiary already added");
		}
	}

	private void trackExpenseForSuccessfulTransaction(BankCustomer bankCustomer, Transaction transaction) {
		try {
			expenseService.trackTransactExpenseForBankCustomer(
				bankCustomer,
				transaction.getReferenceNo(),
				transaction.getAmount(),
				transaction.getTransactionDate()
			);
		} catch (RuntimeException ex) {
			LOGGER.warn("SpendIQ tracking failed for transaction reference {}: {}", transaction.getReferenceNo(), ex.getMessage());
		}
	}

	private TransactionResponse toTransactionResponse(Transaction transaction) {
		return new TransactionResponse(
			transaction.getTransactionId(),
			transaction.getBankCustomer().getBankCustomerId(),
			transaction.getSenderAccountNo(),
			transaction.getReceiverAccountNo(),
			transaction.getReceiverName(),
			transaction.getAmount(),
			transaction.getRemark(),
			transaction.getReferenceNo(),
			transaction.getStatus(),
			transaction.getOtpVerified(),
			transaction.getExpenseTrackingEnabled(),
			transaction.getFailureReason(),
			transaction.getTransactionDate()
		);
	}

	private BeneficiaryResponse toBeneficiaryResponse(Beneficiary beneficiary) {
		return new BeneficiaryResponse(
			beneficiary.getBeneficiaryId(),
			beneficiary.getBankCustomer().getBankCustomerId(),
			beneficiary.getBeneficiaryAccountNo(),
			beneficiary.getNickName(),
			beneficiary.getRemark(),
			beneficiary.getCreatedAt()
		);
	}

	private BankCustomer resolveLoggedInBankCustomer() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (
			authentication == null ||
			!authentication.isAuthenticated() ||
			authentication instanceof AnonymousAuthenticationToken ||
			authentication.getName() == null ||
			authentication.getName().isBlank()
		) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bank customer authentication is required.");
		}

		String principal = authentication.getName().trim();
		String normalizedPrincipal = principal.toLowerCase(Locale.ROOT);
		User user = userRepository
			.findByEmail(normalizedPrincipal)
			.or(() -> userRepository.findByUsername(principal))
			.or(() -> userRepository.findByUsername(normalizedPrincipal))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Logged-in user was not found."));

		String roleName = user.getRole() == null || user.getRole().getRoleName() == null
			? ""
			: user.getRole().getRoleName().trim().toUpperCase(Locale.ROOT);
		if (!ROLE_BANK_CUSTOMER.equals(roleName)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Logged-in user is not a bank customer.");
		}

		return bankCustomerRepository
			.findByUser_UserId(user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Bank customer profile was not found for logged-in user."));
	}
}
