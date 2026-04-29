package com.bank_web_app.backend.admin.service;

import com.bank_web_app.backend.admin.dto.response.AdminDashboardSummaryResponse;
import com.bank_web_app.backend.admin.repository.BranchRepository;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.transact.repository.TransactionRepository;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

	private static final String ROLE_BANK_CUSTOMER = "BANK_CUSTOMER";
	private static final String ROLE_PUBLIC_CUSTOMER = "PUBLIC_CUSTOMER";

	private final UserRepository userRepository;
	private final BranchRepository branchRepository;
	private final BankOfficerRepository bankOfficerRepository;
	private final TransactionRepository transactionRepository;

	public AdminDashboardService(
		UserRepository userRepository,
		BranchRepository branchRepository,
		BankOfficerRepository bankOfficerRepository,
		TransactionRepository transactionRepository
	) {
		this.userRepository = userRepository;
		this.branchRepository = branchRepository;
		this.bankOfficerRepository = bankOfficerRepository;
		this.transactionRepository = transactionRepository;
	}

	@Transactional(readOnly = true)
	public AdminDashboardSummaryResponse getSummary() {
		long totalUsers = userRepository.countByRole_RoleNameIn(List.of(ROLE_BANK_CUSTOMER, ROLE_PUBLIC_CUSTOMER));
		long totalBranches = branchRepository.count();
		long totalOfficers = bankOfficerRepository.count();
		long totalTransactions = transactionRepository.count();

		return new AdminDashboardSummaryResponse(totalUsers, totalBranches, totalOfficers, totalTransactions);
	}
}
