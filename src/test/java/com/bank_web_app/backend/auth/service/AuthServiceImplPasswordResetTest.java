package com.bank_web_app.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank_web_app.backend.auth.dto.request.ForgotPasswordRequest;
import com.bank_web_app.backend.auth.dto.request.ResetPasswordRequest;
import com.bank_web_app.backend.auth.dto.request.VerifyPasswordResetOtpRequest;
import com.bank_web_app.backend.auth.dto.response.AuthActionResponse;
import com.bank_web_app.backend.auth.entity.PasswordResetToken;
import com.bank_web_app.backend.auth.repository.PasswordResetTokenRepository;
import com.bank_web_app.backend.auth.repository.RefreshTokenRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.common.email.EmailService;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerProfileRepository;
import com.bank_web_app.backend.security.jwt.JwtService;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplPasswordResetTest {

	@Mock private UserRepository userRepository;
	@Mock private BankCustomerRepository bankCustomerRepository;
	@Mock private PublicCustomerProfileRepository publicCustomerProfileRepository;
	@Mock private BankOfficerRepository bankOfficerRepository;
	@Mock private JwtService jwtService;
	@Mock private RefreshTokenRepository refreshTokenRepository;
	@Mock private PasswordResetTokenRepository passwordResetTokenRepository;
	@Mock private EmailService emailService;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private AuthServiceImpl authService;

	@BeforeEach
	void setUp() {
		authService = new AuthServiceImpl(
			userRepository,
			bankCustomerRepository,
			publicCustomerProfileRepository,
			bankOfficerRepository,
			jwtService,
			refreshTokenRepository,
			passwordResetTokenRepository,
			passwordEncoder,
			emailService,
			1_209_600_000L,
			10,
			15,
			60,
			"http://localhost:3000/reset-password"
		);
	}

	@Test
	void sendsOtpToRegisteredEmailWhenUsernameIsProvided() {
		User user = activeUser();
		when(userRepository.findByUsernameIgnoreCase("alice.customer")).thenReturn(Optional.of(user));
		when(passwordResetTokenRepository.findAllByUser_UserIdAndConsumedAtIsNullOrderByCreatedAtDesc(12L))
			.thenReturn(List.of());

		AuthActionResponse response = authService.forgotPassword(new ForgotPasswordRequest("alice.customer"));

		ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
		verify(passwordResetTokenRepository).save(tokenCaptor.capture());
		verify(emailService).sendPlainText(eq("alice@example.com"), anyString(), anyString());
		assertThat(tokenCaptor.getValue().getUser()).isSameAs(user);
		assertThat(tokenCaptor.getValue().getOtpHash()).doesNotContain("123456");
		assertThat(response.resetToken()).isNull();
		assertThat(response.message()).contains("If an active account matches");
	}

	@Test
	void rejectsExpiredOtp() {
		User user = activeUser();
		PasswordResetToken token = token(user, "123456", LocalDateTime.now().minusMinutes(1));
		stubEmailLookup(user);
		when(passwordResetTokenRepository.findAllByUser_UserIdAndConsumedAtIsNullOrderByCreatedAtDesc(12L))
			.thenReturn(List.of(token));

		assertThatThrownBy(() -> authService.verifyPasswordResetOtp(
			new VerifyPasswordResetOtpRequest(user.getEmail(), "123456")
		))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("Invalid, expired, or locked");
		assertThat(token.getConsumedAt()).isNotNull();
	}

	@Test
	void locksOtpAfterFiveIncorrectAttempts() {
		User user = activeUser();
		PasswordResetToken token = token(user, "123456", LocalDateTime.now().plusMinutes(10));
		token.setFailedAttempts(4);
		stubEmailLookup(user);
		when(passwordResetTokenRepository.findAllByUser_UserIdAndConsumedAtIsNullOrderByCreatedAtDesc(12L))
			.thenReturn(List.of(token));

		assertThatThrownBy(() -> authService.verifyPasswordResetOtp(
			new VerifyPasswordResetOtpRequest(user.getEmail(), "000000")
		)).isInstanceOf(ResponseStatusException.class);

		assertThat(token.getFailedAttempts()).isEqualTo(5);
		assertThat(token.getConsumedAt()).isNotNull();
	}

	@Test
	void changesPasswordConsumesResetTokenAndRevokesRefreshTokens() {
		User user = activeUser();
		PasswordResetToken token = token(user, "123456", LocalDateTime.now().plusMinutes(10));
		token.setVerifiedAt(LocalDateTime.now());
		token.setResetTokenHash("stored-hash");
		token.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(15));
		when(passwordResetTokenRepository.findByResetTokenHashAndConsumedAtIsNull(anyString()))
			.thenReturn(Optional.of(token));

		authService.resetPassword(new ResetPasswordRequest("opaque-reset-token", "StrongerPass123", "StrongerPass123"));

		assertThat(passwordEncoder.matches("StrongerPass123", user.getPasswordHash())).isTrue();
		assertThat(token.getConsumedAt()).isNotNull();
		verify(refreshTokenRepository).deleteByUser_UserId(12L);
		verify(userRepository).save(user);
	}

	private User activeUser() {
		User user = new User();
		user.setUserId(12L);
		user.setEmail("alice@example.com");
		user.setUsername("alice.customer");
		user.setFirstName("Alice");
		user.setStatus("ACTIVE");
		return user;
	}

	private PasswordResetToken token(User user, String otp, LocalDateTime expiresAt) {
		PasswordResetToken token = new PasswordResetToken();
		token.setUser(user);
		token.setOtpHash(passwordEncoder.encode(otp));
		token.setOtpExpiresAt(expiresAt);
		token.setFailedAttempts(0);
		return token;
	}

	@Test
	void sendsReplacementActivationLinkForPendingCustomer() {
		User user = activeUser();
		user.setStatus("PENDING_ACTIVATION");
		BankCustomer customer = new BankCustomer();
		customer.setUser(user);
		when(userRepository.findByUsernameIgnoreCase(user.getUsername())).thenReturn(Optional.of(user));
		when(bankCustomerRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.of(customer));
		when(passwordResetTokenRepository.findAllByUser_UserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getUserId())).thenReturn(List.of());

		AuthActionResponse response = authService.forgotPassword(new ForgotPasswordRequest(user.getUsername()));

		verify(emailService).sendPlainText(eq(user.getEmail()), anyString(), anyString());
		assertThat(customer.getActivationResendCount()).isEqualTo(1);
		assertThat(response.message()).contains("three replacement links");
	}

	private void stubEmailLookup(User user) {
		when(userRepository.findByUsernameIgnoreCase(user.getEmail())).thenReturn(Optional.empty());
		when(userRepository.findAllByEmailIgnoreCaseOrderByUserIdAsc(user.getEmail())).thenReturn(List.of(user));
	}
}
