package com.bank_web_app.backend.auth.service;

import com.bank_web_app.backend.auth.dto.request.LoginRequest;
import com.bank_web_app.backend.auth.dto.request.RefreshTokenRequest;
import com.bank_web_app.backend.auth.dto.response.LoginResponse;
import com.bank_web_app.backend.auth.entity.RefreshToken;
import com.bank_web_app.backend.auth.repository.RefreshTokenRepository;
import com.bank_web_app.backend.security.jwt.JwtService;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthServiceImpl implements AuthService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final JwtService jwtService;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final long refreshTokenExpirationMs;

	public AuthServiceImpl(
		UserRepository userRepository,
		JwtService jwtService,
		RefreshTokenRepository refreshTokenRepository,
		PasswordEncoder passwordEncoder,
		@Value("${jwt.refresh-token-expiration-ms:1209600000}") long refreshTokenExpirationMs
	) {
		this.userRepository = userRepository;
		this.jwtService = jwtService;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenExpirationMs = refreshTokenExpirationMs;
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
