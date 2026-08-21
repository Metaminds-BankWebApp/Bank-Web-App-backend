package com.bank_web_app.backend.spendiq.scheduler;

import com.bank_web_app.backend.spendiq.service.BudgetLimitRolloverService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
	prefix = "app.spendiq",
	name = "budget-rollover-enabled",
	havingValue = "true",
	matchIfMissing = true
)
public class BudgetLimitRolloverScheduler {

	private final BudgetLimitRolloverService budgetLimitRolloverService;

	public BudgetLimitRolloverScheduler(BudgetLimitRolloverService budgetLimitRolloverService) {
		this.budgetLimitRolloverService = budgetLimitRolloverService;
	}

	@Scheduled(
		cron = "${app.spendiq.budget-rollover-cron:0 55 23 L * *}",
		zone = "${app.notifications.time-zone:Asia/Colombo}"
	)
	public void rolloverBudgetsToNextMonth() {
		budgetLimitRolloverService.rolloverBudgetsToNextMonth();
	}
}
