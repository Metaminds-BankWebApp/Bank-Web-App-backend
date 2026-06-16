package com.bank_web_app.backend.admin.service;

import com.bank_web_app.backend.admin.dto.response.AdminDashboardSummaryResponse;
import com.bank_web_app.backend.admin.dto.response.AdminMonthlyUserGrowthPointResponse;
import com.bank_web_app.backend.admin.dto.response.AdminMonthlyUserGrowthResponse;
import com.bank_web_app.backend.admin.dto.response.AdminRecentActionResponse;
import com.bank_web_app.backend.admin.repository.BranchRepository;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.transact.repository.TransactionRepository;
import com.bank_web_app.backend.user.repository.MonthlyUserGrowthRoleCountProjection;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

	private static final String ROLE_BANK_CUSTOMER = "BANK_CUSTOMER";
	private static final String ROLE_PUBLIC_CUSTOMER = "PUBLIC_CUSTOMER";
	private static final String ROLE_ADMIN = "ADMIN";
	private static final int DEFAULT_MONTH_WINDOW = 6;
	private static final int MIN_MONTH_WINDOW = 1;
	private static final int MAX_MONTH_WINDOW = 24;

	private final UserRepository userRepository;
	private final BranchRepository branchRepository;
	private final BankOfficerRepository bankOfficerRepository;
	private final TransactionRepository transactionRepository;
	private final AuditLogService auditLogService;

	public AdminDashboardService(
		UserRepository userRepository,
		BranchRepository branchRepository,
		BankOfficerRepository bankOfficerRepository,
		TransactionRepository transactionRepository,
		AuditLogService auditLogService
	) {
		this.userRepository = userRepository;
		this.branchRepository = branchRepository;
		this.bankOfficerRepository = bankOfficerRepository;
		this.transactionRepository = transactionRepository;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public AdminDashboardSummaryResponse getSummary() {
		long totalUsers = userRepository.countByRole_RoleNameIn(List.of(ROLE_BANK_CUSTOMER, ROLE_PUBLIC_CUSTOMER));
		long totalBranches = branchRepository.count();
		long totalOfficers = bankOfficerRepository.count();
		long totalTransactions = transactionRepository.count();

		return new AdminDashboardSummaryResponse(totalUsers, totalBranches, totalOfficers, totalTransactions);
	}

	@Transactional(readOnly = true)
	public List<AdminRecentActionResponse> getRecentActions(Integer hours, Integer limit) {
		return auditLogService.getRecentActionsByActorRole(hours, limit, ROLE_ADMIN);
	}

	@Transactional(readOnly = true)
	public AdminMonthlyUserGrowthResponse getMonthlyUserGrowth(Integer months) {
		int monthWindow = sanitizeMonthWindow(months);
		YearMonth latestMonth = YearMonth.now();
		YearMonth startMonth = latestMonth.minusMonths(monthWindow - 1L);
		LocalDateTime startDateTime = startMonth.atDay(1).atStartOfDay();

		List<MonthlyUserGrowthRoleCountProjection> rows = userRepository.findMonthlyUserGrowthByRolesFromDate(
			List.of(ROLE_BANK_CUSTOMER, ROLE_PUBLIC_CUSTOMER),
			startDateTime
		);

		Map<YearMonth, Long> bankCountsByMonth = new HashMap<>();
		Map<YearMonth, Long> publicCountsByMonth = new HashMap<>();

		for (MonthlyUserGrowthRoleCountProjection row : rows) {
			if (row.getYearValue() == null || row.getMonthValue() == null || row.getUserCount() == null) {
				continue;
			}

			YearMonth month = YearMonth.of(row.getYearValue(), row.getMonthValue());
			String roleName = row.getRoleName();

			if (ROLE_BANK_CUSTOMER.equals(roleName)) {
				bankCountsByMonth.put(month, row.getUserCount());
			} else if (ROLE_PUBLIC_CUSTOMER.equals(roleName)) {
				publicCountsByMonth.put(month, row.getUserCount());
			}
		}

		List<AdminMonthlyUserGrowthPointResponse> points = new ArrayList<>(monthWindow);
		for (int i = 0; i < monthWindow; i++) {
			YearMonth month = startMonth.plusMonths(i);
			long bankUsers = bankCountsByMonth.getOrDefault(month, 0L);
			long publicUsers = publicCountsByMonth.getOrDefault(month, 0L);
			long totalUsers = bankUsers + publicUsers;
			String label = month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ROOT);

			points.add(
				new AdminMonthlyUserGrowthPointResponse(
					month.getYear(),
					month.getMonthValue(),
					label,
					totalUsers,
					bankUsers,
					publicUsers
				)
			);
		}

		return new AdminMonthlyUserGrowthResponse(monthWindow, points);
	}

	private int sanitizeMonthWindow(Integer months) {
		if (months == null) {
			return DEFAULT_MONTH_WINDOW;
		}
		return Math.min(MAX_MONTH_WINDOW, Math.max(MIN_MONTH_WINDOW, months));
	}
}
