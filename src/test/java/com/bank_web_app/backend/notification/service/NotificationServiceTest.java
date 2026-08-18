package com.bank_web_app.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank_web_app.backend.notification.entity.Notification;
import com.bank_web_app.backend.notification.entity.NotificationSeverity;
import com.bank_web_app.backend.notification.entity.NotificationSource;
import com.bank_web_app.backend.notification.entity.NotificationType;
import com.bank_web_app.backend.notification.repository.NotificationRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private NotificationCurrentUserService currentUserService;

	private NotificationService notificationService;

	@BeforeEach
	void setUp() {
		notificationService = new NotificationService(notificationRepository, userRepository, currentUserService);
	}

	@Test
	void createsAnOwnedUnreadNotification() {
		User recipient = user(12L);
		when(userRepository.findById(12L)).thenReturn(Optional.of(recipient));
		when(notificationRepository.findByRecipient_UserIdAndDeduplicationKey(12L, "credit:42"))
			.thenReturn(Optional.empty());
		when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Notification created = notificationService.createOrRefresh(command(false));

		assertThat(created.getRecipient()).isSameAs(recipient);
		assertThat(created.getAffectedCount()).isEqualTo(1);
		assertThat(created.getReadAt()).isNull();
		assertThat(created.getActionMetadata()).contains("evaluationId");
	}

	@Test
	void refreshesAnAggregatedNotificationAndMakesItUnreadAgain() {
		User recipient = user(12L);
		Notification existing = new Notification();
		existing.setRecipient(recipient);
		existing.setAffectedCount(2);
		existing.setReadAt(LocalDateTime.now());
		existing.setDismissedAt(LocalDateTime.now());

		when(userRepository.findById(12L)).thenReturn(Optional.of(recipient));
		when(notificationRepository.findByRecipient_UserIdAndDeduplicationKey(12L, "credit:42"))
			.thenReturn(Optional.of(existing));
		when(notificationRepository.save(existing)).thenReturn(existing);

		Notification refreshed = notificationService.createOrRefresh(command(true));

		assertThat(refreshed.getAffectedCount()).isEqualTo(3);
		assertThat(refreshed.getReadAt()).isNull();
		assertThat(refreshed.getDismissedAt()).isNull();
	}

	@Test
	void permanentlyDeletesAnOwnedNotification() {
		User recipient = user(12L);
		Notification reminder = new Notification();
		reminder.setRecipient(recipient);
		reminder.setType(NotificationType.FINANCIAL_DETAILS_MISSING);

		when(currentUserService.resolveRequiredUser()).thenReturn(recipient);
		when(notificationRepository.findByNotificationIdAndRecipient_UserIdAndDismissedAtIsNull(5L, 12L))
			.thenReturn(Optional.of(reminder));

		notificationService.deleteMyNotification(5L);

		verify(notificationRepository).delete(reminder);
	}

	@Test
	void resolvesMissingFinancialDetailsNotificationAfterCompletion() {
		Notification reminder = new Notification();
		when(notificationRepository.findAllByRecipient_UserIdAndTypeAndDismissedAtIsNull(
			12L,
			NotificationType.FINANCIAL_DETAILS_MISSING
		)).thenReturn(List.of(reminder));

		notificationService.resolveFinancialDetailsMissing(12L);

		assertThat(reminder.getDismissedAt()).isNotNull();
		verify(notificationRepository).saveAll(List.of(reminder));
	}

	@Test
	void clearsOnlyTheLoggedInUsersNotifications() {
		User recipient = user(12L);
		when(currentUserService.resolveRequiredUser()).thenReturn(recipient);

		notificationService.clearAllMyNotifications();

		verify(notificationRepository).deleteByRecipient_UserId(12L);
	}

	private NotificationCommand command(boolean aggregate) {
		return new NotificationCommand(
			12L,
			NotificationType.CREDITLENS_RESULT_AVAILABLE,
			NotificationSource.CREDITLENS,
			NotificationSeverity.WARNING,
			"Credit result available",
			"Open the latest result.",
			"CREDITLENS_RESULT",
			Map.of("evaluationId", "42"),
			"credit:42",
			aggregate
		);
	}

	private User user(Long userId) {
		User user = new User();
		user.setUserId(userId);
		return user;
	}
}
