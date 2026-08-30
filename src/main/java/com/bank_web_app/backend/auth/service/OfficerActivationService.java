package com.bank_web_app.backend.auth.service;

import com.bank_web_app.backend.auth.dto.response.OfficerActivationResponse;
import com.bank_web_app.backend.auth.entity.PasswordResetToken;
import com.bank_web_app.backend.auth.entity.PasswordResetTokenPurpose;
import com.bank_web_app.backend.auth.repository.PasswordResetTokenRepository;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.common.email.BankOfficerCredentialsEmailService;
import com.bank_web_app.backend.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OfficerActivationService {

	public static final int MAX_RESENDS = 3;
	public static final int ACTIVATION_EXPIRY_HOURS = 72;
	private static final String ROLE_BANK_OFFICER = "BANK_OFFICER";
	private static final String STATUS_PENDING_ACTIVATION = "PENDING_ACTIVATION";
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final Logger LOGGER = LoggerFactory.getLogger(OfficerActivationService.class);

	private final BankOfficerRepository bankOfficerRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final BankOfficerCredentialsEmailService activationEmailService;

	public OfficerActivationService(
		BankOfficerRepository bankOfficerRepository,
		PasswordResetTokenRepository passwordResetTokenRepository,
		PasswordEncoder passwordEncoder,
		BankOfficerCredentialsEmailService activationEmailService
	) {
		this.bankOfficerRepository = bankOfficerRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.activationEmailService = activationEmailService;
	}

	@Transactional
	public void sendInitialInvitation(BankOfficer officer) {
		User user = requirePendingOfficer(officer);
		String rawToken = createActivationToken(user);
		activationEmailService.sendActivationEmail(user.getEmail(), user.getFirstName(), user.getUsername(), rawToken);
	}

	@Transactional(readOnly = true)
	public OfficerActivationResponse inspect(String rawToken) {
		PasswordResetToken token = findToken(rawToken);
		if (token == null) {
			return response("INVALID", "This activation link is invalid.", 0, false);
		}
		if (!isOfficerActivationToken(token)) {
			return response("NOT_ACTIVATION", "This is not a bank-officer activation link.", 0, false);
		}

		BankOfficer officer = bankOfficerRepository.findByUser_UserId(token.getUser().getUserId()).orElse(null);
		if (officer == null) {
			return response("INVALID", "This activation link is invalid.", 0, false);
		}

		User user = officer.getUser();
		if (!STATUS_PENDING_ACTIVATION.equalsIgnoreCase(safe(user.getStatus()))) {
			return response("ACTIVATED", "This officer account has already been activated. Please sign in.", officer.getActivationResendCount(), false);
		}
		if (officer.getActivationPasswordSetAt() != null) {
			return response("PASSWORD_SET", "Your password has already been set. Sign in to activate your account.", officer.getActivationResendCount(), false);
		}

		boolean valid = token.getConsumedAt() == null
			&& token.getVerifiedAt() != null
			&& token.getResetTokenExpiresAt() != null
			&& token.getResetTokenExpiresAt().isAfter(LocalDateTime.now());
		if (valid) {
			return response("VALID", "This activation link is valid.", officer.getActivationResendCount(), officer.getActivationResendCount() < MAX_RESENDS);
		}

		boolean canResend = officer.getActivationResendCount() < MAX_RESENDS;
		String message = canResend
			? "This activation link has expired or is no longer valid. Request a new link below."
			: "This activation link has expired and the maximum of 3 resend requests has been reached.";
		return response(canResend ? "EXPIRED" : "RESEND_LIMIT_REACHED", message, officer.getActivationResendCount(), canResend);
	}

	@Transactional
	public OfficerActivationResponse resend(String rawToken) {
		PasswordResetToken token = findToken(rawToken);
		if (token == null || !isOfficerActivationToken(token)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This activation link is invalid and cannot be resent.");
		}

		BankOfficer officer = bankOfficerRepository
			.findByUserIdForActivationUpdate(token.getUser().getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "This activation link is invalid and cannot be resent."));
		return resendLocked(officer);
	}

	@Transactional
	public OfficerActivationResponse resendByUserId(Long userId) {
		BankOfficer officer = bankOfficerRepository
			.findByUserIdForActivationUpdate(userId)
			.orElseThrow(() -> new IllegalArgumentException("Bank officer not found."));
		return resendLocked(officer);
	}

	@Transactional
	public void recordPasswordSet(User user, LocalDateTime passwordSetAt) {
		BankOfficer officer = bankOfficerRepository
			.findByUserIdForActivationUpdate(user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bank officer activation profile was not found."));
		officer.setActivationPasswordSetAt(passwordSetAt);
		bankOfficerRepository.save(officer);
	}

	@Transactional(readOnly = true)
	public boolean isReadyForFirstLogin(User user) {
		return isPendingBankOfficer(user)
			&& bankOfficerRepository.findByUser_UserId(user.getUserId())
				.map(officer -> officer.getActivationPasswordSetAt() != null)
				.orElse(false);
	}

	public boolean isOfficerActivationToken(PasswordResetToken token) {
		return token != null
			&& (token.getPurpose() == PasswordResetTokenPurpose.OFFICER_ACTIVATION || isPendingBankOfficer(token.getUser()));
	}

	private OfficerActivationResponse resendLocked(BankOfficer officer) {
		User user = requirePendingOfficer(officer);
		if (officer.getActivationPasswordSetAt() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A password is already set. Sign in to activate the account.");
		}
		if (officer.getActivationResendCount() >= MAX_RESENDS) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Maximum activation resend limit of 3 has been reached.");
		}

		officer.setActivationResendCount(officer.getActivationResendCount() + 1);
		bankOfficerRepository.save(officer);
		String newRawToken = createActivationToken(user);
		activationEmailService.sendActivationEmail(user.getEmail(), user.getFirstName(), user.getUsername(), newRawToken);
		LOGGER.info("Bank officer activation invitation resent for userId={}, attempt={}", user.getUserId(), officer.getActivationResendCount());
		return response("RESENT", "A new activation link has been sent to your registered email address.", officer.getActivationResendCount(), officer.getActivationResendCount() < MAX_RESENDS);
	}

	private User requirePendingOfficer(BankOfficer officer) {
		if (officer == null || officer.getUser() == null || !isPendingBankOfficer(officer.getUser())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending bank officer accounts can request an activation link.");
		}
		return officer.getUser();
	}

	private boolean isPendingBankOfficer(User user) {
		return user != null
			&& user.getRole() != null
			&& ROLE_BANK_OFFICER.equalsIgnoreCase(safe(user.getRole().getRoleName()))
			&& STATUS_PENDING_ACTIVATION.equalsIgnoreCase(safe(user.getStatus()));
	}

	private String createActivationToken(User user) {
		LocalDateTime now = LocalDateTime.now();
		List<PasswordResetToken> existing = passwordResetTokenRepository
			.findAllByUser_UserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getUserId());
		existing.forEach(token -> token.setConsumedAt(now));
		if (!existing.isEmpty()) {
			passwordResetTokenRepository.saveAll(existing);
		}

		String rawToken = generateSecret();
		PasswordResetToken token = new PasswordResetToken();
		token.setUser(user);
		token.setPurpose(PasswordResetTokenPurpose.OFFICER_ACTIVATION);
		token.setOtpHash(passwordEncoder.encode(generateSecret()));
		token.setOtpExpiresAt(now.plusHours(ACTIVATION_EXPIRY_HOURS));
		token.setFailedAttempts(0);
		token.setVerifiedAt(now);
		token.setResetTokenHash(sha256Hex(rawToken));
		token.setResetTokenExpiresAt(now.plusHours(ACTIVATION_EXPIRY_HOURS));
		passwordResetTokenRepository.save(token);
		return rawToken;
	}

	private PasswordResetToken findToken(String rawToken) {
		String normalized = safe(rawToken);
		if (normalized.isBlank()) {
			return null;
		}
		return passwordResetTokenRepository.findByResetTokenHash(sha256Hex(normalized)).orElse(null);
	}

	private OfficerActivationResponse response(String status, String message, int used, boolean canResend) {
		int normalizedUsed = Math.max(0, Math.min(MAX_RESENDS, used));
		return new OfficerActivationResponse(status, message, normalizedUsed, MAX_RESENDS - normalizedUsed, canResend);
	}

	private String generateSecret() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String sha256Hex(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder output = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				output.append(String.format("%02x", b));
			}
			return output.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available.", exception);
		}
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}
}
