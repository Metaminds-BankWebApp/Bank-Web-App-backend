package com.bank_web_app.backend.transact.scheduler;

import com.bank_web_app.backend.transact.service.TransactionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Ensures unused transfer OTPs cannot leave transactions in PENDING_OTP status.
@Component
@ConditionalOnProperty(
	prefix = "app.transact.otp",
	name = "expiry-scheduling-enabled",
	havingValue = "true",
	matchIfMissing = true
)
public class TransactionOtpExpiryScheduler {

	private final TransactionService transactionService;

	public TransactionOtpExpiryScheduler(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@Scheduled(fixedDelayString = "${app.transact.otp.expiry-check-delay-ms:30000}")
	public void failExpiredOtpTransactions() {
		transactionService.expirePendingOtpTransactions();
	}
}
