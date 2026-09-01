package com.bank_web_app.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bank_web_app.backend.auth.dto.response.OfficerActivationResponse;
import com.bank_web_app.backend.auth.entity.PasswordResetToken;
import com.bank_web_app.backend.auth.entity.PasswordResetTokenPurpose;
import com.bank_web_app.backend.auth.repository.PasswordResetTokenRepository;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.common.email.BankOfficerCredentialsEmailService;
import com.bank_web_app.backend.user.entity.Role;
import com.bank_web_app.backend.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OfficerActivationServiceTest {

	@Mock private BankOfficerRepository bankOfficerRepository;
	@Mock private PasswordResetTokenRepository passwordResetTokenRepository;
	@Mock private BankOfficerCredentialsEmailService activationEmailService;

	private OfficerActivationService activationService;

	@BeforeEach
	void setUp() {
		activationService = new OfficerActivationService(
			bankOfficerRepository,
			passwordResetTokenRepository,
			new BCryptPasswordEncoder(),
			activationEmailService
		);
	}

	@Test
	void expiredInvitationCanBeResentWhileAttemptsRemain() {
		BankOfficer officer = pendingOfficer(0);
		PasswordResetToken token = activationToken(officer.getUser(), LocalDateTime.now().minusMinutes(1));
		when(passwordResetTokenRepository.findByResetTokenHash(anyString())).thenReturn(Optional.of(token));
		when(bankOfficerRepository.findByUser_UserId(21L)).thenReturn(Optional.of(officer));

		OfficerActivationResponse response = activationService.inspect("expired-token");

		assertThat(response.status()).isEqualTo("EXPIRED");
		assertThat(response.canResend()).isTrue();
		assertThat(response.remainingResends()).isEqualTo(3);
	}

	@Test
	void thirdResendCreatesAReplacementThreeDayInvitation() {
		BankOfficer officer = pendingOfficer(2);
		PasswordResetToken oldToken = activationToken(officer.getUser(), LocalDateTime.now().minusMinutes(1));
		when(passwordResetTokenRepository.findByResetTokenHash(anyString())).thenReturn(Optional.of(oldToken));
		when(bankOfficerRepository.findByUserIdForActivationUpdate(21L)).thenReturn(Optional.of(officer));
		when(passwordResetTokenRepository.findAllByUser_UserIdAndConsumedAtIsNullOrderByCreatedAtDesc(21L))
			.thenReturn(List.of(oldToken));

		OfficerActivationResponse response = activationService.resend("expired-token");

		ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
		verify(passwordResetTokenRepository).save(tokenCaptor.capture());
		PasswordResetToken replacement = tokenCaptor.getValue();
		assertThat(officer.getActivationResendCount()).isEqualTo(3);
		assertThat(response.remainingResends()).isZero();
		assertThat(response.canResend()).isFalse();
		assertThat(replacement.getPurpose()).isEqualTo(PasswordResetTokenPurpose.OFFICER_ACTIVATION);
		assertThat(replacement.getResetTokenExpiresAt()).isAfter(LocalDateTime.now().plusHours(71));
		verify(activationEmailService).sendActivationEmail(anyString(), anyString(), anyString(), anyString());
	}

	@Test
	void resendIsRejectedAfterThreeRequests() {
		BankOfficer officer = pendingOfficer(3);
		PasswordResetToken token = activationToken(officer.getUser(), LocalDateTime.now().minusMinutes(1));
		when(passwordResetTokenRepository.findByResetTokenHash(anyString())).thenReturn(Optional.of(token));
		when(bankOfficerRepository.findByUserIdForActivationUpdate(21L)).thenReturn(Optional.of(officer));

		assertThatThrownBy(() -> activationService.resend("expired-token"))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception ->
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
			);
		verifyNoInteractions(activationEmailService);
	}

	private BankOfficer pendingOfficer(int resendCount) {
		Role role = new Role();
		role.setRoleName("BANK_OFFICER");

		User user = new User();
		user.setUserId(21L);
		user.setRole(role);
		user.setStatus("PENDING_ACTIVATION");
		user.setEmail("officer@gmail.com");
		user.setFirstName("Nimal");
		user.setUsername("nimal.officer");

		BankOfficer officer = new BankOfficer();
		officer.setUser(user);
		officer.setActivationResendCount(resendCount);
		return officer;
	}

	private PasswordResetToken activationToken(User user, LocalDateTime expiresAt) {
		PasswordResetToken token = new PasswordResetToken();
		token.setUser(user);
		token.setPurpose(PasswordResetTokenPurpose.OFFICER_ACTIVATION);
		token.setOtpHash("hash");
		token.setOtpExpiresAt(expiresAt);
		token.setVerifiedAt(LocalDateTime.now().minusDays(1));
		token.setResetTokenExpiresAt(expiresAt);
		return token;
	}
}
