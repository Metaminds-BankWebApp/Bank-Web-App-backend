package com.bank_web_app.backend.auth.service;

import com.bank_web_app.backend.auth.dto.request.ForgotPasswordRequest;
import com.bank_web_app.backend.auth.dto.request.LoginRequest;
import com.bank_web_app.backend.auth.dto.request.RefreshTokenRequest;
import com.bank_web_app.backend.auth.dto.request.ResetPasswordRequest;
import com.bank_web_app.backend.auth.dto.request.VerifyOtpRequest;
import com.bank_web_app.backend.auth.dto.response.AuthActionResponse;
import com.bank_web_app.backend.auth.dto.response.AuthMeResponse;
import com.bank_web_app.backend.auth.dto.response.LoginResponse;
import com.bank_web_app.backend.auth.entity.PasswordResetOtp;
import com.bank_web_app.backend.auth.entity.RefreshToken;
import com.bank_web_app.backend.auth.repository.PasswordResetOtpRepository;
import com.bank_web_app.backend.auth.repository.RefreshTokenRepository;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.common.email.EmailDeliveryException;
import com.bank_web_app.backend.common.email.EmailService;
import com.bank_web_app.backend.security.jwt.JwtService;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerProfile;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerProfileRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
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
public class AuthServiceImpl implements AuthService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final int OTP_LENGTH = 6;
	private static final int RESET_TOKEN_BYTES = 48;
	private static final String OTP_STATUS_SENT = "SENT";
	private static final String OTP_STATUS_VERIFIED = "VERIFIED";
	private static final String OTP_STATUS_USED = "USED";
	private static final String OTP_STATUS_EXPIRED = "EXPIRED";
	private static final String PASSWORD_RESET_REQUEST_MESSAGE =
		"If an account exists for this email, a verification code has been sent.";

	private final UserRepository userRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final PublicCustomerProfileRepository publicCustomerProfileRepository;
	private final BankOfficerRepository bankOfficerRepository;
	private final JwtService jwtService;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordResetOtpRepository passwordResetOtpRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final long refreshTokenExpirationMs;
	private final int passwordResetOtpExpiryMinutes;
	private final boolean passwordResetEmailFailOpenEnabled;
	private final boolean passwordResetOtpPlainLogEnabled;
	private final String passwordResetOverrideRecipientEmail;

	public AuthServiceImpl(
		UserRepository userRepository,
		BankCustomerRepository bankCustomerRepository,
		PublicCustomerProfileRepository publicCustomerProfileRepository,
		BankOfficerRepository bankOfficerRepository,
		JwtService jwtService,
		RefreshTokenRepository refreshTokenRepository,
		PasswordResetOtpRepository passwordResetOtpRepository,
		PasswordEncoder passwordEncoder,
		EmailService emailService,
		@Value("${jwt.refresh-token-expiration-ms:1209600000}") long refreshTokenExpirationMs,
		@Value("${app.auth.password-reset.otp.expiry-minutes:10}") int passwordResetOtpExpiryMinutes,
		@Value("${app.auth.password-reset.otp.fail-open-enabled:true}") boolean passwordResetEmailFailOpenEnabled,
		@Value("${app.auth.password-reset.otp.log-plain-enabled:true}") boolean passwordResetOtpPlainLogEnabled,
		@Value("${app.auth.password-reset.otp.override-recipient-email:}") String passwordResetOverrideRecipientEmail
	) {
		this.userRepository = userRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.publicCustomerProfileRepository = publicCustomerProfileRepository;
		this.bankOfficerRepository = bankOfficerRepository;
		this.jwtService = jwtService;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordResetOtpRepository = passwordResetOtpRepository;
		this.passwordEncoder = passwordEncoder;
		this.emailService = emailService;
		this.refreshTokenExpirationMs = refreshTokenExpirationMs;
		this.passwordResetOtpExpiryMinutes = passwordResetOtpExpiryMinutes;
		this.passwordResetEmailFailOpenEnabled = passwordResetEmailFailOpenEnabled;
		this.passwordResetOtpPlainLogEnabled = passwordResetOtpPlainLogEnabled;
		this.passwordResetOverrideRecipientEmail = passwordResetOverrideRecipientEmail == null
			? ""
			: passwordResetOverrideRecipientEmail.trim();
	}

	@Override
	@Transactional
	public LoginResponse login(LoginRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		String password = request.password();

		User user = userRepository
			.findByEmail(email)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));

		if (!matchesPasswordAndUpgradeIfNeeded(user, password)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
		}

		if ("INACTIVE".equalsIgnoreCase(user.getStatus())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is inactive.");
		}

		return issueTokenPair(user);
	}

	@Override
	@Transactional
	public LoginResponse refresh(RefreshTokenRequest request) {
		String rawRefreshToken = request.refreshToken().trim();
		String tokenHash = sha256Hex(rawRefreshToken);

		RefreshToken existingToken = refreshTokenRepository
			.findByTokenHashAndRevokedFalse(tokenHash)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token."));

		if (existingToken.getExpiresAt().isBefore(LocalDateTime.now())) {
			existingToken.setRevoked(true);
			refreshTokenRepository.save(existingToken);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired.");
		}

		User user = existingToken.getUser();
		if ("INACTIVE".equalsIgnoreCase(user.getStatus())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is inactive.");
		}

		TokenPair rotatedPair = createRefreshToken(user);

		existingToken.setRevoked(true);
		existingToken.setLastUsedAt(LocalDateTime.now());
		existingToken.setReplacedByTokenHash(rotatedPair.refreshTokenHash());
		refreshTokenRepository.save(existingToken);

		return issueTokenResponse(user, rotatedPair.rawRefreshToken());
	}

	@Override
	@Transactional
	public void logout(RefreshTokenRequest request) {
		String rawRefreshToken = request.refreshToken().trim();
		if (rawRefreshToken.isBlank()) {
			return;
		}

		String tokenHash = sha256Hex(rawRefreshToken);
		refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
			if (!token.isRevoked()) {
				token.setRevoked(true);
				token.setLastUsedAt(LocalDateTime.now());
				refreshTokenRepository.save(token);
			}
		});
	}

	@Override
	@Transactional(readOnly = true)
	public AuthMeResponse me() {
		User user = resolveLoggedInUser();
		String fullName = (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
		if (fullName.isBlank()) {
			fullName = user.getEmail();
		}

		Long bankCustomerId = bankCustomerRepository
			.findByUser_UserId(user.getUserId())
			.map(BankCustomer::getBankCustomerId)
			.orElse(null);

		Long publicCustomerId = publicCustomerProfileRepository
			.findByUser_UserId(user.getUserId())
			.map(PublicCustomerProfile::getPublicCustomerId)
			.orElse(null);

		Long officerId = bankOfficerRepository
			.findByUser_UserId(user.getUserId())
			.map(BankOfficer::getOfficerId)
			.orElse(null);

		return new AuthMeResponse(
			user.getUserId(),
			user.getEmail(),
			user.getUsername(),
			fullName,
			user.getRole().getRoleId(),
			user.getRole().getRoleName(),
			bankCustomerId,
			publicCustomerId,
			officerId
		);
	}

	@Override
	@Transactional
	public AuthActionResponse forgotPassword(ForgotPasswordRequest request) {
		String email = normalizeEmail(request.email());
		userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
			expireOpenPasswordResetOtps(user.getUserId());
			String otpCode = generateOtp();
			String recipientEmail = resolvePasswordResetRecipientEmail(user);

			PasswordResetOtp otp = new PasswordResetOtp();
			otp.setUser(user);
			otp.setOtpCodeHash(passwordEncoder.encode(otpCode));
			otp.setSentToEmail(recipientEmail);
			otp.setOtpStatus(OTP_STATUS_SENT);
			otp.setExpiresAt(LocalDateTime.now().plusMinutes(passwordResetOtpExpiryMinutes));
			passwordResetOtpRepository.save(otp);

			sendPasswordResetOtpEmail(user, recipientEmail, otpCode, otp.getExpiresAt());
		});

		return AuthActionResponse.message(PASSWORD_RESET_REQUEST_MESSAGE);
	}

	@Override
	@Transactional
	public AuthActionResponse verifyOtp(VerifyOtpRequest request) {
		String email = normalizeEmail(request.email());
		User user = userRepository
			.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP code."));

		PasswordResetOtp otp = passwordResetOtpRepository
			.findTopByUser_UserIdOrderByCreatedAtDesc(user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP code."));

		LocalDateTime now = LocalDateTime.now();
		if (!OTP_STATUS_SENT.equals(otp.getOtpStatus()) || otp.getExpiresAt().isBefore(now)) {
			if (OTP_STATUS_SENT.equals(otp.getOtpStatus()) && otp.getExpiresAt().isBefore(now)) {
				otp.setOtpStatus(OTP_STATUS_EXPIRED);
				passwordResetOtpRepository.save(otp);
			}
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP code.");
		}

		if (!passwordEncoder.matches(request.otp().trim(), otp.getOtpCodeHash())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP code.");
		}

		String resetToken = generateResetToken();
		otp.setResetTokenHash(sha256Hex(resetToken));
		otp.setOtpStatus(OTP_STATUS_VERIFIED);
		otp.setVerifiedAt(now);
		passwordResetOtpRepository.save(otp);

		return AuthActionResponse.withResetToken("OTP verified. You can now reset your password.", resetToken);
	}

	@Override
	@Transactional
	public AuthActionResponse resetPassword(ResetPasswordRequest request) {
		if (!request.password().equals(request.confirmPassword())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match.");
		}

		String email = normalizeEmail(request.email());
		User user = userRepository
			.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset session."));

		String resetTokenHash = sha256Hex(request.resetToken().trim());
		PasswordResetOtp otp = passwordResetOtpRepository
			.findTopByUser_UserIdAndResetTokenHashAndOtpStatusOrderByCreatedAtDesc(
				user.getUserId(),
				resetTokenHash,
				OTP_STATUS_VERIFIED
			)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset session."));

		LocalDateTime now = LocalDateTime.now();
		if (otp.getExpiresAt().isBefore(now)) {
			otp.setOtpStatus(OTP_STATUS_EXPIRED);
			passwordResetOtpRepository.save(otp);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset session.");
		}

		user.setPasswordHash(passwordEncoder.encode(request.password()));
		userRepository.save(user);

		otp.setOtpStatus(OTP_STATUS_USED);
		otp.setUsedAt(now);
		passwordResetOtpRepository.save(otp);

		revokeUserRefreshTokens(user);

		return AuthActionResponse.message("Password reset successfully.");
	}

	private User resolveLoggedInUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (
			authentication == null ||
			!authentication.isAuthenticated() ||
			authentication instanceof AnonymousAuthenticationToken ||
			authentication.getName() == null ||
			authentication.getName().isBlank()
		) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
		}

		String principal = authentication.getName().trim();
		String normalizedPrincipal = principal.toLowerCase(Locale.ROOT);
		return userRepository
			.findByEmail(normalizedPrincipal)
			.or(() -> userRepository.findByUsername(principal))
			.or(() -> userRepository.findByUsername(normalizedPrincipal))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Logged-in user was not found."));
	}

	private LoginResponse issueTokenPair(User user) {
		TokenPair pair = createRefreshToken(user);
		return issueTokenResponse(user, pair.rawRefreshToken());
	}

	private LoginResponse issueTokenResponse(User user, String refreshToken) {
		String fullName = (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
		if (fullName.isBlank()) {
			fullName = user.getEmail();
		}

		String accessToken = jwtService.generateAccessToken(user);
		long expiresInSeconds = TimeUnit.MILLISECONDS.toSeconds(jwtService.getAccessTokenExpirationMs());

		return new LoginResponse(
			accessToken,
			"Bearer",
			refreshToken,
			expiresInSeconds,
			new LoginResponse.UserInfo(
				String.valueOf(user.getUserId()),
				user.getEmail(),
				fullName,
				user.getRole().getRoleName()
			)
		);
	}

	private void expireOpenPasswordResetOtps(Long userId) {
		List<PasswordResetOtp> openOtps = passwordResetOtpRepository.findAllByUser_UserIdAndOtpStatusIn(
			userId,
			List.of(OTP_STATUS_SENT, OTP_STATUS_VERIFIED)
		);
		for (PasswordResetOtp otp : openOtps) {
			otp.setOtpStatus(OTP_STATUS_EXPIRED);
		}
		passwordResetOtpRepository.saveAll(openOtps);
	}

	private void revokeUserRefreshTokens(User user) {
		List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserAndRevokedFalse(user);
		for (RefreshToken token : activeTokens) {
			token.setRevoked(true);
			token.setLastUsedAt(LocalDateTime.now());
		}
		refreshTokenRepository.saveAll(activeTokens);
	}

	private void sendPasswordResetOtpEmail(User user, String toEmail, String otpCode, LocalDateTime expiresAt) {
		String subject = "PrimeCore password reset OTP";
		String body = buildPasswordResetEmailBody(resolveDisplayName(user), otpCode, expiresAt);
		try {
			emailService.sendPlainText(toEmail, subject, body);
		} catch (EmailDeliveryException ex) {
			if (!passwordResetEmailFailOpenEnabled) {
				throw ex;
			}
			LOGGER.warn("Password reset OTP email delivery failed for user {}: {}", user.getUserId(), ex.getMessage());
			if (passwordResetOtpPlainLogEnabled) {
				LOGGER.warn(
					"DEV password reset OTP fallback - userId={} email={} otpCode={} expiresAt={}",
					user.getUserId(),
					user.getEmail(),
					otpCode,
					expiresAt
				);
			}
		}
	}

	private String buildPasswordResetEmailBody(String name, String otpCode, LocalDateTime expiresAt) {
		String displayName = name == null || name.isBlank() ? "PrimeCore customer" : name;
		return "Hello " + displayName + ",\n\n" +
			"Use this one-time password to reset your PrimeCore account password:\n\n" +
			"OTP: " + otpCode + "\n" +
			"Expires at: " + expiresAt + " (valid for " + passwordResetOtpExpiryMinutes + " minutes)\n\n" +
			"If you did not request this password reset, you can ignore this email. PrimeCore will never ask you to share your OTP.\n\n" +
			"PrimeCore Security Team";
	}

	private String resolvePasswordResetRecipientEmail(User user) {
		if (!passwordResetOverrideRecipientEmail.isBlank()) {
			LOGGER.debug("Using app.auth.password-reset.otp.override-recipient-email for password reset OTP delivery.");
			return passwordResetOverrideRecipientEmail;
		}
		String email = user.getEmail() == null ? "" : user.getEmail().trim();
		if (email.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PASSWORD_RESET_REQUEST_MESSAGE);
		}
		return email;
	}

	private String resolveDisplayName(User user) {
		String fullName = (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
		if (!fullName.isBlank()) {
			return fullName;
		}
		String username = safe(user.getUsername());
		return username.isBlank() ? safe(user.getEmail()) : username;
	}

	private String normalizeEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
	}

	private String generateOtp() {
		int bound = (int) Math.pow(10, OTP_LENGTH);
		int value = SECURE_RANDOM.nextInt(bound);
		return String.format(Locale.ROOT, "%0" + OTP_LENGTH + "d", value);
	}

	private String generateResetToken() {
		byte[] randomBytes = new byte[RESET_TOKEN_BYTES];
		SECURE_RANDOM.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	private TokenPair createRefreshToken(User user) {
		String rawToken = generateRefreshToken();
		String tokenHash = sha256Hex(rawToken);

		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setUser(user);
		refreshToken.setTokenHash(tokenHash);
		refreshToken.setRevoked(false);
		refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(TimeUnit.MILLISECONDS.toSeconds(refreshTokenExpirationMs)));
		refreshTokenRepository.save(refreshToken);

		return new TokenPair(rawToken, tokenHash);
	}

	private String generateRefreshToken() {
		byte[] randomBytes = new byte[64];
		SECURE_RANDOM.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	private String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("Unable to hash refresh token.", ex);
		}
	}

	private boolean matchesPasswordAndUpgradeIfNeeded(User user, String rawPassword) {
		String stored = user.getPasswordHash();
		if (stored == null || stored.isBlank()) {
			return false;
		}

		if (isBcryptHash(stored)) {
			return passwordEncoder.matches(rawPassword, stored);
		}

		boolean matched = stored.equals(rawPassword);
		if (matched) {
			user.setPasswordHash(passwordEncoder.encode(rawPassword));
			userRepository.save(user);
		}

		return matched;
	}

	private boolean isBcryptHash(String value) {
		return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}

	private record TokenPair(String rawRefreshToken, String refreshTokenHash) {}
}
