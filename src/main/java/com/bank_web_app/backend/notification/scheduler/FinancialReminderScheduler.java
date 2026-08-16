package com.bank_web_app.backend.notification.scheduler;

import com.bank_web_app.backend.notification.entity.NotificationSeverity;
import com.bank_web_app.backend.notification.entity.NotificationSource;
import com.bank_web_app.backend.notification.entity.NotificationType;
import com.bank_web_app.backend.notification.service.NotificationCommand;
import com.bank_web_app.backend.notification.service.NotificationService;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerFinancialRecord;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerProfile;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerFinancialRecordRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerProfileRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
	prefix = "app.notifications",
	name = "scheduling-enabled",
	havingValue = "true",
	matchIfMissing = true
)
public class FinancialReminderScheduler {

	private static final String CURRENT_RECORD_STATUS = "CURRENT";

	private final PublicCustomerProfileRepository publicCustomerProfileRepository;
	private final PublicCustomerFinancialRecordRepository financialRecordRepository;
	private final NotificationService notificationService;

	public FinancialReminderScheduler(
		PublicCustomerProfileRepository publicCustomerProfileRepository,
		PublicCustomerFinancialRecordRepository financialRecordRepository,
		NotificationService notificationService
	) {
		this.publicCustomerProfileRepository = publicCustomerProfileRepository;
		this.financialRecordRepository = financialRecordRepository;
		this.notificationService = notificationService;
	}

	@Scheduled(
		cron = "${app.notifications.financial-reminder-cron:0 0 9 * * *}",
		zone = "${app.notifications.time-zone:Asia/Colombo}"
	)
	@Transactional
	public void createDueFinancialReminders() {
		LocalDate today = LocalDate.now();
		for (PublicCustomerProfile profile : publicCustomerProfileRepository.findAll()) {
			createReminderWhenDue(profile, today);
		}
	}

	private void createReminderWhenDue(PublicCustomerProfile profile, LocalDate today) {
		if (profile.getUser() == null || profile.getUser().getUserId() == null) return;

		PublicCustomerFinancialRecord currentRecord = financialRecordRepository
			.findByPublicCustomer_PublicCustomerIdAndRecordStatus(profile.getPublicCustomerId(), CURRENT_RECORD_STATUS)
			.orElse(null);
		if (currentRecord == null) {
			createMissingFinancialDetailsReminder(profile.getUser().getUserId());
			return;
		}

		LocalDateTime baseDateTime = currentRecord.getUpdatedAt();
		if (baseDateTime == null) return;

		LocalDate dueDate = baseDateTime.plusMonths(1).toLocalDate();
		if (dueDate.isAfter(today)) return;

		notificationService.createOrRefresh(
			new NotificationCommand(
				profile.getUser().getUserId(),
				NotificationType.MONTHLY_FINANCIAL_REVIEW_DUE,
				NotificationSource.SYSTEM,
				NotificationSeverity.WARNING,
				"Monthly financial review due",
				"Review or update your financial details for the new monthly cycle.",
				"PUBLIC_FINANCIAL_DETAILS",
				Map.of("dueDate", dueDate.toString()),
				"public-financial-reminder:" + profile.getUser().getUserId() + ":" + dueDate,
				false
			)
		);
	}

	private void createMissingFinancialDetailsReminder(Long recipientUserId) {
		notificationService.createOrRefresh(
			new NotificationCommand(
				recipientUserId,
				NotificationType.FINANCIAL_DETAILS_MISSING,
				NotificationSource.SYSTEM,
				NotificationSeverity.WARNING,
				"Complete your financial details",
				"Please fill in your financial details to complete your profile and receive accurate assessments.",
				"PUBLIC_FINANCIAL_DETAILS",
				Map.of(),
				NotificationService.financialDetailsMissingDeduplicationKey(recipientUserId),
				false
			)
		);
	}
}
