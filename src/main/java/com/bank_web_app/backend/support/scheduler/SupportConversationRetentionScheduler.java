package com.bank_web_app.backend.support.scheduler;

import com.bank_web_app.backend.support.service.SupportConversationService;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
	prefix = "app.support.retention",
	name = "scheduling-enabled",
	havingValue = "true",
	matchIfMissing = true
)
public class SupportConversationRetentionScheduler {

	private final SupportConversationService supportConversationService;
	private final int retentionDays;

	public SupportConversationRetentionScheduler(
		SupportConversationService supportConversationService,
		@Value("${app.support.retention.days:30}") int retentionDays
	) {
		this.supportConversationService = supportConversationService;
		this.retentionDays = Math.max(1, retentionDays);
	}

	@Scheduled(
		cron = "${app.support.retention.cleanup-cron:0 15 3 * * *}",
		zone = "${app.support.retention.time-zone:Asia/Colombo}"
	)
	public void removeClosedConversationsAfterRetentionPeriod() {
		supportConversationService.permanentlyDeleteClosedInactiveConversationsBefore(
			LocalDateTime.now().minusDays(retentionDays)
		);
	}
}
