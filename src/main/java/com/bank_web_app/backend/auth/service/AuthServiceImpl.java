package com.bank_web_app.backend.auth.service;

import com.bank_web_app.backend.auth.dto.request.ForgotPasswordRequest;
import com.bank_web_app.backend.auth.dto.request.LoginRequest;
import com.bank_web_app.backend.auth.dto.request.RefreshTokenRequest;
import com.bank_web_app.backend.auth.dto.request.ResetPasswordRequest;
import com.bank_web_app.backend.auth.dto.request.VerifyPasswordResetOtpRequest;
import com.bank_web_app.backend.auth.dto.response.AuthActionResponse;
import com.bank_web_app.backend.auth.dto.response.AuthMeResponse;
import com.bank_web_app.backend.auth.dto.response.LoginResponse;
import com.bank_web_app.backend.auth.entity.PasswordResetToken;
import com.bank_web_app.backend.auth.entity.RefreshToken;
import com.bank_web_app.backend.auth.repository.PasswordResetTokenRepository;
import com.bank_web_app.backend.auth.repository.RefreshTokenRepository;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
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
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthServiceImpl implements AuthService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final int PASSWORD_RESET_OTP_LENGTH = 6;
	private static final int PASSWORD_RESET_MAX_ATTEMPTS = 5;
	private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{10,255}$");
	private static final String PASSWORD_RESET_REQUEST_MESSAGE =
		"If an active account matches those details, a verification code has been sent to its registered email address.";
	private static final String INVALID_RESET_OTP_MESSAGE = "Invalid, expired, or locked verification code.";
	private static final String INVALID_RESET_SESSION_MESSAGE = "This password-reset session is invalid or has expired. Request a new code.";

	private final UserRepository userRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final PublicCustomerProfileRepository publicCustomerProfileRepository;
	private final BankOfficerRepository bankOfficerRepository;
	private final JwtService jwtService;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final long refreshTokenExpirationMs;
	private final int passwordResetOtpExpiryMinutes;
	private final int passwordResetTokenExpiryMinutes;
	private final int passwordResetResendCooldownSeconds;

	public AuthServiceImpl(
		UserRepository userRepository,
		BankCustomerRepository bankCustomerRepository,
		PublicCustomerProfileRepository publicCustomerProfileRepository,
		BankOfficerRepository bankOfficerRepository,
		JwtService jwtService,
		RefreshTokenRepository refreshTokenRepository,
		PasswordResetTokenRepository passwordResetTokenRepository,
		PasswordEncoder passwordEncoder,
		EmailService emailService,
		@Value("${jwt.refresh-token-expiration-ms:1209600000}") long refreshTokenExpirationMs,
		@Value("${app.password-reset.otp-expiry-minutes:10}") int passwordResetOtpExpiryMinutes,
		@Value("${app.password-reset.token-expiry-minutes:15}") int passwordResetTokenExpiryMinutes,
		@Value("${app.password-reset.resend-cooldown-seconds:60}") int passwordResetResendCooldownSeconds
	) {
		this.userRepository = userRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.publicCustomerProfileRepository = publicCustomerProfileRepository;
		this.bankOfficerRepository = bankOfficerRepository;
		this.jwtService = jwtService;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.emailService = emailService;
		this.refreshTokenExpirationMs = refreshTokenExpirationMs;
		this.passwordResetOtpExpiryMinutes = Math.max(1, passwordResetOtpExpiryMinutes);
		this.passwordResetTokenExpiryMinutes = Math.max(1, passwordResetTokenExpiryMinutes);
		this.passwordResetResendCooldownSeconds = Math.max(1, passwordResetResendCooldownSeconds);
	}

	@Override
	@Transactional
	public LoginResponse login(LoginRequest request) {
		String identifier = request.identifier().trim();
		String password = request.password();

		User user = userRepository
			.findByEmailIgnoreCase(identifier)
			.or(() -> userRepository.findByUsernameIgnoreCase(identifier))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials."));

		if (!matchesPasswordAndUpgradeIfNeeded(user, password)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials.");
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
	@Transactional
	public AuthActionResponse forgotPassword(ForgotPasswordRequest request) {
		User user = findUserByIdentifier(request.identifier()).orElse(null);
		if (user == null || "INACTIVE".equalsIgnoreCase(user.getStatus())) {
			return passwordResetRequestResponse();
		}

		LocalDateTime now = LocalDateTime.now();
		List<PasswordResetToken> activeTokens = passwordResetTokenRepository
			.findAllByUser_UserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getUserId());
		if (
			!activeTokens.isEmpty() &&
			activeTokens.getFirst().getCreatedAt().isAfter(now.minusSeconds(passwordResetResendCooldownSeconds))
		) {
			return passwordResetRequestResponse();
		}

		activeTokens.forEach(token -> token.setConsumedAt(now));
		if (!activeTokens.isEmpty()) {
			passwordResetTokenRepository.saveAll(activeTokens);
		}

		String otp = generatePasswordResetOtp();
		PasswordResetToken token = new PasswordResetToken();
		token.setUser(user);
		token.setOtpHash(passwordEncoder.encode(otp));
		token.setOtpExpiresAt(now.plusMinutes(passwordResetOtpExpiryMinutes));
		token.setFailedAttempts(0);
		passwordResetTokenRepository.save(token);

		try {
			emailService.sendPlainText(
				user.getEmail(),
				"Primecore password reset code",
				buildPasswordResetEmail(user, otp)
			);
		} catch (RuntimeException exception) {
			token.setConsumedAt(now);
			passwordResetTokenRepository.save(token);
			LOGGER.error("Password-reset email could not be delivered for userId={}", user.getUserId(), exception);
		}

		return passwordResetRequestResponse();
	}

	@Override
	@Transactional
	public AuthActionResponse verifyPasswordResetOtp(VerifyPasswordResetOtpRequest request) {
		User user = findUserByIdentifier(request.identifier()).orElse(null);
		if (user == null || "INACTIVE".equalsIgnoreCase(user.getStatus())) {
			throw invalidPasswordResetOtp();
		}

		PasswordResetToken token = passwordResetTokenRepository
			.findAllByUser_UserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getUserId())
			.stream()
			.findFirst()
			.orElseThrow(this::invalidPasswordResetOtp);
		LocalDateTime now = LocalDateTime.now();
		if (token.getOtpExpiresAt().isBefore(now) || token.getFailedAttempts() >= PASSWORD_RESET_MAX_ATTEMPTS) {
			token.setConsumedAt(now);
			passwordResetTokenRepository.save(token);
			throw invalidPasswordResetOtp();
		}

		if (!passwordEncoder.matches(request.otp().trim(), token.getOtpHash())) {
			token.setFailedAttempts(token.getFailedAttempts() + 1);
			if (token.getFailedAttempts() >= PASSWORD_RESET_MAX_ATTEMPTS) {
				token.setConsumedAt(now);
			}
			passwordResetTokenRepository.save(token);
			throw invalidPasswordResetOtp();
		}

		String rawResetToken = generateResetToken();
		token.setVerifiedAt(now);
		token.setResetTokenHash(sha256Hex(rawResetToken));
		token.setResetTokenExpiresAt(now.plusMinutes(passwordResetTokenExpiryMinutes));
		passwordResetTokenRepository.save(token);
		return new AuthActionResponse("Verification successful. You can now set a new password.", rawResetToken);
	}

	@Override
	@Transactional
	public AuthActionResponse resetPassword(ResetPasswordRequest request) {
		if (!request.password().equals(request.confirmPassword())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirm password does not match.");
		}
		if (!PASSWORD_PATTERN.matcher(request.password()).matches()) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Password must be at least 10 characters and include uppercase, lowercase, and numbers."
			);
		}

		PasswordResetToken token = passwordResetTokenRepository
			.findByResetTokenHashAndConsumedAtIsNull(sha256Hex(request.resetToken().trim()))
			.orElseThrow(this::invalidPasswordResetSession);
		LocalDateTime now = LocalDateTime.now();
		if (
			token.getVerifiedAt() == null ||
			token.getResetTokenExpiresAt() == null ||
			!token.getResetTokenExpiresAt().isAfter(now)
		) {
			token.setConsumedAt(now);
			passwordResetTokenRepository.save(token);
			throw invalidPasswordResetSession();
		}

		User user = token.getUser();
		if ("INACTIVE".equalsIgnoreCase(user.getStatus())) {
			throw invalidPasswordResetSession();
		}
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		userRepository.save(user);
		refreshTokenRepository.deleteByUser_UserId(user.getUserId());
		token.setConsumedAt(now);
		passwordResetTokenRepository.save(token);

		try {
			emailService.sendPlainText(
				user.getEmail(),
				"Your Primecore password was changed",
				"Your password was changed successfully. If you did not make this change, contact support immediately."
			);
		} catch (RuntimeException exception) {
			LOGGER.error("Password-reset confirmation email could not be delivered for userId={}", user.getUserId(), exception);
		}

		return new AuthActionResponse("Password updated successfully. Please sign in with your new password.", null);
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

	private java.util.Optional<User> findUserByIdentifier(String identifier) {
		String normalizedIdentifier = identifier.trim();
		return userRepository
			.findByEmailIgnoreCase(normalizedIdentifier)
			.or(() -> userRepository.findByUsernameIgnoreCase(normalizedIdentifier));
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

	private String generatePasswordResetOtp() {
		int upperBound = (int) Math.pow(10, PASSWORD_RESET_OTP_LENGTH);
		return String.format(Locale.ROOT, "%0" + PASSWORD_RESET_OTP_LENGTH + "d", SECURE_RANDOM.nextInt(upperBound));
	}

	private String generateResetToken() {
		byte[] randomBytes = new byte[32];
		SECURE_RANDOM.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	private String buildPasswordResetEmail(User user, String otp) {
		String name = safe(user.getFirstName());
		String greeting = name.isBlank() ? "Hello," : "Hello " + name + ",";
		return greeting + "\n\n" +
			"Use this verification code to reset your Primecore password: " + otp + "\n\n" +
			"This code expires in " + passwordResetOtpExpiryMinutes + " minutes. Do not share it with anyone.";
	}

	private AuthActionResponse passwordResetRequestResponse() {
		return new AuthActionResponse(PASSWORD_RESET_REQUEST_MESSAGE, null);
	}

	private ResponseStatusException invalidPasswordResetOtp() {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_RESET_OTP_MESSAGE);
	}

	private ResponseStatusException invalidPasswordResetSession() {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_RESET_SESSION_MESSAGE);
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
