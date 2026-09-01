package com.bank_web_app.backend.notification.event;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.notification.entity.NotificationSeverity;
import com.bank_web_app.backend.notification.entity.NotificationSource;
import com.bank_web_app.backend.notification.entity.NotificationType;
import com.bank_web_app.backend.notification.service.NotificationCommand;
import com.bank_web_app.backend.notification.service.NotificationCurrentUserService;
import com.bank_web_app.backend.notification.service.NotificationService;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventHandler {

	private static final String ROLE_ADMIN = "ADMIN";

	private final NotificationService notificationService;
	private final NotificationCurrentUserService currentUserService;
	private final UserRepository userRepository;
	private final BankOfficerRepository bankOfficerRepository;
	private final BankCustomerRepository bankCustomerRepository;

	public NotificationEventHandler(
		NotificationService notificationService,
		NotificationCurrentUserService currentUserService,
		UserRepository userRepository,
		BankOfficerRepository bankOfficerRepository,
		BankCustomerRepository bankCustomerRepository
	) {
		this.notificationService = notificationService;
		this.currentUserService = currentUserService;
		this.userRepository = userRepository;
		this.bankOfficerRepository = bankOfficerRepository;
		this.bankCustomerRepository = bankCustomerRepository;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handle(NotificationDomainEvent event) {
			switch (event.type()) {
			case ADMIN_NEW_CUSTOMER -> notifyAdminsAboutNewCustomer(event);
			case PUBLIC_FINANCIAL_DETAILS_REQUIRED -> notifyPublicCustomerToCompleteFinancialDetails(event);
			case BANK_CUSTOMER_ASSIGNED -> notifyAssignedOfficer(event);
			case BRANCH_STATUS_CHANGED -> notifyBranchUsers(event);
			case OFFICER_STATUS_CHANGED -> notifyOfficerStatus(event);
			case LOAN_POLICY_CHANGED -> notifyPolicyChange(event);
			case SPENDIQ_TRANSFER_IMPORTED -> notifyTransferImport(event);
			case SPENDIQ_BUDGET_THRESHOLD -> notifyBudgetThreshold(event);
			case CREDITLENS_EVALUATED -> notifyCreditLensEvaluation(event);
			case LOANSENSE_EVALUATED -> notifyLoanSenseEvaluation(event);
			case TRANSACTION_OTP_ATTEMPTS_EXCEEDED -> notifyAdminsAboutOtpAttemptLimit(event);
		}
	}

	private void notifyPublicCustomerToCompleteFinancialDetails(NotificationDomainEvent event) {
		if (event.recipientUserId() == null) return;
		create(
			event.recipientUserId(),
			NotificationType.FINANCIAL_DETAILS_MISSING,
			NotificationSource.SYSTEM,
			NotificationSeverity.WARNING,
			"Complete your financial details",
			"Please fill in your financial details to complete your profile and receive accurate assessments.",
			"PUBLIC_FINANCIAL_DETAILS",
			Map.of(),
			NotificationService.financialDetailsMissingDeduplicationKey(event.recipientUserId()),
			false
		);
	}

	private void notifyAdminsAboutNewCustomer(NotificationDomainEvent event) {
		String role = value(event, "role", "CUSTOMER").replace('_', ' ').toLowerCase(Locale.ROOT);
		forEachAdminExcept(event.actorUserId(), admin -> create(
			admin.getUserId(),
			NotificationType.NEW_CUSTOMER_SUMMARY,
			NotificationSource.ADMIN,
			NotificationSeverity.INFO,
			"New customer registrations",
			"New " + role + " registrations are ready for review.",
			"ADMIN_USER_MANAGEMENT",
			Map.of("role", value(event, "role", "CUSTOMER")),
			"admin:new-customers:" + LocalDate.now(),
			true
		));
	}

	private void notifyAssignedOfficer(NotificationDomainEvent event) {
		if (event.recipientUserId() == null) return;
		String customerName = value(event, "customerName", "A bank customer");
		create(
			event.recipientUserId(),
			NotificationType.CUSTOMER_ASSIGNED,
			NotificationSource.ONBOARDING,
			NotificationSeverity.INFO,
			"Customer assigned to you",
			customerName + " was added to your customer portfolio.",
			"OFFICER_CUSTOMER_PROFILE",
			metadata(event, "customerId"),
			"officer:assignment:" + event.recipientUserId() + ":" + event.subjectId(),
			false
		);
	}

	private void notifyBranchUsers(NotificationDomainEvent event) {
		if (event.subjectId() == null) return;
		String branchName = value(event, "branchName", "Your branch");
		String status = value(event, "status", "UPDATED");
		NotificationSeverity severity = "ACTIVE".equalsIgnoreCase(status)
			? NotificationSeverity.SUCCESS
			: NotificationSeverity.WARNING;

		for (BankOfficer officer : bankOfficerRepository.findAllByBranch_BranchId(event.subjectId())) {
			createBranchStatusNotification(officer.getUser().getUserId(), branchName, status, severity, event.subjectId());
		}
		for (BankCustomer customer : bankCustomerRepository.findAllByBranch_BranchId(event.subjectId())) {
			createBranchStatusNotification(customer.getUser().getUserId(), branchName, status, severity, event.subjectId());
		}
	}

	private void createBranchStatusNotification(
		Long recipientUserId,
		String branchName,
		String status,
		NotificationSeverity severity,
		Long branchId
	) {
		create(
			recipientUserId,
			NotificationType.BRANCH_STATUS_CHANGED,
			NotificationSource.ONBOARDING,
			severity,
			"Branch status changed",
			branchName + " is now " + status.replace('_', ' ').toLowerCase(Locale.ROOT) + ".",
			"DASHBOARD",
			Map.of("branchId", String.valueOf(branchId), "status", status),
			"branch-status:" + branchId + ":" + status + ":" + LocalDate.now(),
			false
		);
	}

	private void notifyOfficerStatus(NotificationDomainEvent event) {
		if (event.recipientUserId() == null) return;
		String status = value(event, "status", "UPDATED");
		create(
			event.recipientUserId(),
			NotificationType.OFFICER_ACCOUNT_STATUS_CHANGED,
			NotificationSource.ONBOARDING,
			"ACTIVE".equalsIgnoreCase(status) ? NotificationSeverity.SUCCESS : NotificationSeverity.WARNING,
			"Account status changed",
			"Your bank officer account is now " + status.replace('_', ' ').toLowerCase(Locale.ROOT) + ".",
			"DASHBOARD",
			Map.of("status", status),
			"officer-status:" + event.recipientUserId() + ":" + status + ":" + LocalDate.now(),
			false
		);
	}

	private void notifyPolicyChange(NotificationDomainEvent event) {
		Long currentActorId = event.actorUserId();
		if (currentActorId == null) {
			currentActorId = currentUserService.resolveOptionalUser().map(User::getUserId).orElse(null);
		}
		String loanType = value(event, "loanType", "Loan");
		forEachAdminExcept(currentActorId, admin -> create(
			admin.getUserId(),
			NotificationType.LOAN_POLICY_CHANGED,
			NotificationSource.ADMIN,
			NotificationSeverity.INFO,
			"Loan policy changed",
			loanType.replace('_', ' ') + " policy settings were updated by another administrator.",
			"ADMIN_LOAN_POLICY",
			metadata(event, "policyId", "loanType"),
			"admin:policy:" + event.subjectId() + ":" + LocalDate.now(),
			false
		));

		for (BankCustomer customer : bankCustomerRepository.findAll()) {
			create(
				customer.getUser().getUserId(),
				NotificationType.LOAN_POLICY_CHANGED,
				NotificationSource.LOANSENSE,
				NotificationSeverity.WARNING,
				"Loan policy may affect eligibility",
				loanType.replace('_', ' ') + " policy settings changed. Review your latest LoanSense result.",
				"LOANSENSE_CURRENT",
				metadata(event, "policyId", "loanType"),
				"customer:policy:" + customer.getUser().getUserId() + ":" + event.subjectId() + ":" + LocalDate.now(),
				false
			);
		}
	}

	private void notifyTransferImport(NotificationDomainEvent event) {
		if (event.recipientUserId() == null) return;
		String reference = value(event, "referenceNo", String.valueOf(event.subjectId()));
		create(
			event.recipientUserId(),
			NotificationType.SPENDIQ_TRANSFER_IMPORTED,
			NotificationSource.SPENDIQ,
			NotificationSeverity.SUCCESS,
			"Transfer recorded in SpendIQ",
			"Your successful transfer was automatically added as a SpendIQ expense.",
			"SPENDIQ_EXPENSE",
			metadata(event, "expenseId", "referenceNo"),
			"spendiq:transfer:" + reference,
			false
		);
	}

	private void notifyBudgetThreshold(NotificationDomainEvent event) {
		if (event.recipientUserId() == null) return;
		String threshold = value(event, "threshold", "80");
		boolean exceeded = "100".equals(threshold);
		create(
			event.recipientUserId(),
			NotificationType.SPENDIQ_BUDGET_THRESHOLD,
			NotificationSource.SPENDIQ,
			exceeded ? NotificationSeverity.ALERT : NotificationSeverity.WARNING,
			exceeded ? "Category budget exceeded" : "Category budget reached 80%",
			value(event, "categoryName", "A category") + " has reached the " + threshold + "% budget threshold.",
			"SPENDIQ_BUDGET",
			metadata(event, "categoryId", "month", "year"),
			"spendiq:budget:" + event.recipientUserId() + ":" + value(event, "categoryId", "all") + ":" +
				value(event, "year", String.valueOf(YearMonth.now().getYear())) + "-" +
				value(event, "month", String.valueOf(YearMonth.now().getMonthValue())) + ":" + threshold,
			false
		);
	}

	private void notifyCreditLensEvaluation(NotificationDomainEvent event) {
		if (event.recipientUserId() != null) {
			create(
				event.recipientUserId(),
				NotificationType.CREDITLENS_RESULT_AVAILABLE,
				NotificationSource.CREDITLENS,
				severityForRisk(value(event, "riskLevel", "MEDIUM")),
				"CreditLens result available",
				"A new officer-generated CreditLens evaluation is ready to review.",
				"CREDITLENS_RESULT",
				metadata(event, "evaluationId", "customerId", "riskLevel"),
				"creditlens:customer:" + event.subjectId(),
				false
			);
		}

		Long officerUserId = parseLong(event.metadata().get("officerUserId"));
		if (officerUserId != null) {
			create(
				officerUserId,
				NotificationType.CREDITLENS_PORTFOLIO_ATTENTION,
				NotificationSource.CREDITLENS,
				severityForRisk(value(event, "riskLevel", "MEDIUM")),
				"CreditLens portfolio attention",
				"Your CreditLens portfolio has new or changed evaluation results.",
				"OFFICER_CREDITLENS_DASHBOARD",
				metadata(event, "customerId", "riskLevel"),
				"creditlens:officer:" + officerUserId + ":" + LocalDate.now(),
				true
			);
		}
	}

	private void notifyLoanSenseEvaluation(NotificationDomainEvent event) {
		if (event.recipientUserId() != null) {
			String status = value(event, "overallStatus", "UPDATED");
			create(
				event.recipientUserId(),
				NotificationType.LOANSENSE_RESULT_AVAILABLE,
				NotificationSource.LOANSENSE,
				"NOT_ELIGIBLE".equals(status) ? NotificationSeverity.ALERT : NotificationSeverity.INFO,
				"LoanSense result available",
				"A new officer-generated LoanSense evaluation is ready to review.",
				"LOANSENSE_RESULT",
				metadata(event, "evaluationId", "customerId", "overallStatus"),
				"loansense:customer:" + event.subjectId(),
				false
			);
		}

		Long officerUserId = parseLong(event.metadata().get("officerUserId"));
		if (officerUserId != null) {
			create(
				officerUserId,
				NotificationType.LOANSENSE_PORTFOLIO_ATTENTION,
				NotificationSource.LOANSENSE,
				NotificationSeverity.WARNING,
				"LoanSense portfolio attention",
				"Your LoanSense portfolio has new or changed eligibility results.",
				"OFFICER_LOANSENSE_DASHBOARD",
				metadata(event, "customerId", "overallStatus"),
				"loansense:officer:" + officerUserId + ":" + LocalDate.now(),
				true
			);
		}
	}

	private void notifyAdminsAboutOtpAttemptLimit(NotificationDomainEvent event) {
		String customerName = value(event, "customerName", "A bank customer");
		String referenceNo = value(event, "referenceNo", "the transaction");
		String accountNumber = value(event, "accountNumber", "");
		String attemptCount = value(event, "attemptCount", "the allowed");
		String accountSuffix = accountNumber.isBlank() ? "" : " for account ending " + accountNumber;
		forEachAdminExcept(event.actorUserId(), admin -> create(
			admin.getUserId(),
			NotificationType.TRANSACTION_OTP_ATTEMPTS_EXCEEDED,
			NotificationSource.TRANSACT,
			NotificationSeverity.ALERT,
			"Transaction OTP attempt limit reached",
			customerName + " entered an incorrect transaction OTP " + attemptCount + " times" + accountSuffix + ". A bank officer reviewed and escalated transaction " + referenceNo + ".",
			"ADMIN_USER_MANAGEMENT",
			metadata(event, "customerUserId", "customerId", "referenceNo", "accountNumber", "attemptCount"),
			"admin:transaction-otp-limit:" + referenceNo,
			false
		));
	}

	private void forEachAdminExcept(Long excludedUserId, java.util.function.Consumer<User> consumer) {
		List<User> admins = userRepository.findAllByRole_RoleNameOrderByUpdatedAtDesc(ROLE_ADMIN);
		admins.stream()
			.filter(admin -> excludedUserId == null || !excludedUserId.equals(admin.getUserId()))
			.forEach(consumer);
	}

	private void create(
		Long recipientUserId,
		NotificationType type,
		NotificationSource source,
		NotificationSeverity severity,
		String title,
		String message,
		String actionKey,
		Map<String, String> metadata,
		String deduplicationKey,
		boolean aggregate
	) {
		notificationService.createOrRefresh(
			new NotificationCommand(
				recipientUserId,
				type,
				source,
				severity,
				title,
				message,
				actionKey,
				metadata,
				deduplicationKey,
				aggregate
			)
		);
	}

	private Map<String, String> metadata(NotificationDomainEvent event, String... allowedKeys) {
		Map<String, String> result = new LinkedHashMap<>();
		for (String key : allowedKeys) {
			String value = event.metadata().get(key);
			if (value != null && !value.isBlank()) result.put(key, value);
		}
		return result;
	}

	private String value(NotificationDomainEvent event, String key, String fallback) {
		String value = event.metadata().get(key);
		return value == null || value.isBlank() ? fallback : value.trim();
	}

	private NotificationSeverity severityForRisk(String riskLevel) {
		return switch (riskLevel.toUpperCase(Locale.ROOT)) {
			case "HIGH" -> NotificationSeverity.ALERT;
			case "MEDIUM" -> NotificationSeverity.WARNING;
			default -> NotificationSeverity.INFO;
		};
	}

	private Long parseLong(String value) {
		try {
			return value == null ? null : Long.valueOf(value);
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}
