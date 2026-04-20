package com.bank_web_app.backend.transact.service;

import com.bank_web_app.backend.bankcustomer.entity.Account;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.AccountRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.transact.dto.request.CreateBeneficiaryRequest;
import com.bank_web_app.backend.transact.dto.request.CreateTransactionRequest;
import com.bank_web_app.backend.transact.dto.request.ResendTransactionOtpRequest;
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
	private static final DateTimeFormatter REFERENCE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	private static final String ALPHA_NUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

	private final TransactionRepository transactionRepository;
	private final OtpRecordRepository otpRecordRepository;
	private final BeneficiaryRepository beneficiaryRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final SecureRandom secureRandom;

	public TransactionService(
		TransactionRepository transactionRepository,
		OtpRecordRepository otpRecordRepository,
		BeneficiaryRepository beneficiaryRepository,
		BankCustomerRepository bankCustomerRepository,
		AccountRepository accountRepository,
		UserRepository userRepository,
		PasswordEncoder passwordEncoder
	) {
		this.transactionRepository = transactionRepository;
		this.otpRecordRepository = otpRecordRepository;
		this.beneficiaryRepository = beneficiaryRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.accountRepository = accountRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.secureRandom = new SecureRandom();
	}

	@Transactional
	public TransactionInitiateResponse initiateTransaction(CreateTransactionRequest request) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();

		String senderAccountNo = normalizeAccountNumber(request.senderAccountNo());
		String receiverAccountNo = normalizeAccountNumber(request.receiverAccountNo());

		if (!senderAccountNo.equals(bankCustomer.getAccount().getAccountNumber())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sender account does not belong to logged-in bank customer.");
		}
		if (senderAccountNo.equals(receiverAccountNo)) {
			throw new IllegalArgumentException("Sender and receiver account numbers cannot be the same.");
		}

		Account senderAccount = accountRepository
			.findByAccountNumber(senderAccountNo)
			.orElseThrow(() -> new IllegalArgumentException("Sender account was not found."));
		Account receiverAccount = accountRepository
			.findByAccountNumber(receiverAccountNo)
			.orElseThrow(() -> new IllegalArgumentException("Receiver account was not found."));

		validateActiveAccount(senderAccount, "Sender account is not active.");
		validateActiveAccount(receiverAccount, "Receiver account is not active.");

		String referenceNo = generateUniqueReferenceNo();

		Transaction transaction = new Transaction();
		transaction.setBankCustomer(bankCustomer);
		transaction.setSenderAccountNo(senderAccountNo);
		transaction.setReceiverAccountNo(receiverAccountNo);
		transaction.setReceiverName(request.receiverName().trim());
		transaction.setAmount(request.amount());
		transaction.setRemark(request.remark().trim());
		transaction.setReferenceNo(referenceNo);
		transaction.setStatus(STATUS_PENDING_OTP);
		transaction.setOtpVerified(Boolean.FALSE);
		transaction.setExpenseTrackingEnabled(Boolean.TRUE.equals(request.expenseTrackingEnabled()));
		transaction.setFailureReason(null);

		transaction = transactionRepository.save(transaction);

		String otpCode = generateOtp();
		OtpRecord otpRecord = createOtpRecord(transaction, bankCustomer.getUser().getEmail(), otpCode, 0);

		return new TransactionInitiateResponse(
			transaction.getTransactionId(),
			transaction.getReferenceNo(),
			transaction.getStatus(),
			otpRecord.getSentToEmail(),
			otpRecord.getExpiresAt(),
			"Transaction created. OTP has been issued for verification."
		);
	}

	@Transactional
	public TransactionResponse verifyOtp(VerifyTransactionOtpRequest request) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		Transaction transaction = transactionRepository
			.findByReferenceNoAndBankCustomer_BankCustomerId(request.referenceNo().trim(), bankCustomer.getBankCustomerId())
			.orElseThrow(() -> new IllegalArgumentException("Transaction was not found for this bank customer."));

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

		Account senderAccount = accountRepository
			.findByAccountNumber(transaction.getSenderAccountNo())
			.orElseThrow(() -> new IllegalStateException("Sender account was not found during verification."));
		Account receiverAccount = accountRepository
			.findByAccountNumber(transaction.getReceiverAccountNo())
			.orElseThrow(() -> new IllegalStateException("Receiver account was not found during verification."));

		validateActiveAccount(senderAccount, "Sender account is not active.");
		validateActiveAccount(receiverAccount, "Receiver account is not active.");

		BigDecimal availableBalance = senderAccount.getBalance();
		if (availableBalance == null || availableBalance.compareTo(transaction.getAmount()) < 0) {
			transaction.setStatus(STATUS_FAILED);
			transaction.setOtpVerified(Boolean.FALSE);
			transaction.setFailureReason("Insufficient balance.");
			transactionRepository.save(transaction);
			throw new IllegalArgumentException("Insufficient balance to complete this transaction.");
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

		return toTransactionResponse(transaction);
	}

	@Transactional
	public TransactionInitiateResponse resendOtp(ResendTransactionOtpRequest request) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		Transaction transaction = transactionRepository
			.findByReferenceNoAndBankCustomer_BankCustomerId(request.referenceNo().trim(), bankCustomer.getBankCustomerId())
			.orElseThrow(() -> new IllegalArgumentException("Transaction was not found for this bank customer."));

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
		OtpRecord otpRecord = createOtpRecord(
			transaction,
			bankCustomer.getUser().getEmail(),
			otpCode,
			(previousOtp.getResendCount() == null ? 0 : previousOtp.getResendCount()) + 1
		);

		return new TransactionInitiateResponse(
			transaction.getTransactionId(),
			transaction.getReferenceNo(),
			transaction.getStatus(),
			otpRecord.getSentToEmail(),
			otpRecord.getExpiresAt(),
			"OTP has been reissued for this transaction."
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
			.orElseThrow(() -> new IllegalArgumentException("Transaction was not found for this bank customer."));
		return toTransactionResponse(transaction);
	}

	@Transactional
	public BeneficiaryResponse createBeneficiary(CreateBeneficiaryRequest request) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();

		String accountNo = normalizeAccountNumber(request.beneficiaryAccountNo());
		if (accountNo.equals(bankCustomer.getAccount().getAccountNumber())) {
			throw new IllegalArgumentException("Beneficiary account cannot be the same as sender account.");
		}
		if (accountRepository.findByAccountNumber(accountNo).isEmpty()) {
			throw new IllegalArgumentException("Beneficiary account does not exist.");
		}

		Beneficiary beneficiary = new Beneficiary();
		beneficiary.setBankCustomer(bankCustomer);
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

	private String normalizeAccountNumber(String accountNo) {
		return accountNo == null ? "" : accountNo.replaceAll("\\s+", "").trim();
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
