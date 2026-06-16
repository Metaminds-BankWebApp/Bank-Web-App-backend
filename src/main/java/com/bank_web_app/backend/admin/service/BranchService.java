package com.bank_web_app.backend.admin.service;

import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank_web_app.backend.admin.dto.request.BranchRequest;
import com.bank_web_app.backend.admin.dto.response.BranchResponse;
import com.bank_web_app.backend.admin.entity.Branch;
import com.bank_web_app.backend.admin.entity.BranchStatus;
import com.bank_web_app.backend.admin.repository.BranchRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;

@Service
public class BranchService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BranchService.class);

	private final BranchRepository branchRepository;
	private final BankOfficerRepository bankOfficerRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final AuditLogService auditLogService;

	public BranchService(
		BranchRepository branchRepository,
		BankOfficerRepository bankOfficerRepository,
		BankCustomerRepository bankCustomerRepository,
		AuditLogService auditLogService
	) {
		this.branchRepository = branchRepository;
		this.bankOfficerRepository = bankOfficerRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public BranchResponse create(BranchRequest request) {
		Branch branch = new Branch();
		branch.setBranchCode(generateBranchCode());
		branch.setBranchName(normalizeRequired(request.branchName(), "Branch name is required."));
		branch.setBranchEmail(normalizeOptional(request.branchEmail()));
		branch.setBranchPhone(normalizeOptional(request.branchPhone()));
		branch.setAddress(normalizeOptional(request.address()));
		branch.setStatus(normalizeStatus(request.status()));

		Branch saved = branchRepository.save(branch);
		BranchResponse response = toResponse(saved);
		auditLogService.logAction(
			"BRANCH_CREATED",
			"Approved Branch Creation: \"" + safe(saved.getBranchName()) + "\"",
			"BRANCH",
			safe(saved.getBranchCode()),
			"Created branch with status " + safe(saved.getStatus() == null ? null : saved.getStatus().name()) + ".",
			"SUCCESS"
		);
		return response;
	}

	@Transactional(readOnly = true)
	public List<BranchResponse> getAll() {
		return branchRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public BranchResponse getById(Long branchId) {
		return toResponse(findBranch(branchId));
	}

	@Transactional
	public BranchResponse update(Long branchId, BranchRequest request) {
		Branch branch = findBranch(branchId);

		branch.setBranchName(normalizeRequired(request.branchName(), "Branch name is required."));
		branch.setBranchEmail(normalizeOptional(request.branchEmail()));
		branch.setBranchPhone(normalizeOptional(request.branchPhone()));
		branch.setAddress(normalizeOptional(request.address()));
		branch.setStatus(normalizeStatus(request.status()));

		Branch saved = branchRepository.save(branch);
		BranchResponse response = toResponse(saved);
		auditLogService.logAction(
			"BRANCH_UPDATED",
			"Updated Branch Details: \"" + safe(saved.getBranchName()) + "\"",
			"BRANCH",
			safe(saved.getBranchCode()),
			"Updated branch profile details and status.",
			"INFO"
		);
		return response;
	}

	@Transactional
	public BranchResponse updateStatus(Long branchId, String status) {
		Branch branch = findBranch(branchId);
		branch.setStatus(normalizeStatus(status));
		Branch saved = branchRepository.save(branch);
		BranchResponse response = toResponse(saved);
		String normalizedStatus = safe(saved.getStatus() == null ? null : saved.getStatus().name());
		auditLogService.logAction(
			"BRANCH_STATUS_CHANGED",
			"Changed Branch Status: \"" + safe(saved.getBranchName()) + "\" -> " + normalizedStatus,
			"BRANCH",
			safe(saved.getBranchCode()),
			"Branch status changed to " + normalizedStatus + ".",
			"ACTIVE".equals(normalizedStatus) ? "SUCCESS" : "WARNING"
		);
		return response;
	}

	@Transactional
	public BranchResponse delete(Long branchId) {
		Branch branch = findBranch(branchId);
		if (
			bankOfficerRepository.existsByBranch_BranchId(branchId) ||
			bankCustomerRepository.existsByBranch_BranchId(branchId)
		) {
			throw new IllegalArgumentException(
				"This branch cannot be deleted because it is linked to officers or customers."
			);
		}

		BranchResponse response = toResponse(branch);
		branchRepository.delete(branch);
		auditLogService.logAction(
			"BRANCH_DELETED",
			"Deleted Branch Permanently: \"" + safe(response.branchName()) + "\"",
			"BRANCH",
			safe(response.branchCode()),
			"Branch was deleted permanently from the system.",
			"WARNING"
		);
		return response;
	}

	private Branch findBranch(Long branchId) {
		return branchRepository.findById(branchId)
			.orElseThrow(() -> new IllegalArgumentException("Branch not found."));
	}

	private String generateBranchCode() {
		long nextValue = resolveNextBranchCodeValue();
		String candidate = String.format("BR-%04d", nextValue);
		while (branchRepository.existsByBranchCode(candidate)) {
			nextValue++;
			candidate = String.format("BR-%04d", nextValue);
		}
		return candidate;
	}

	private long resolveNextBranchCodeValue() {
		try {
			Long maxValue = branchRepository.findMaxBranchCodeNumericValue();
			if (maxValue != null && maxValue > 0) {
				return maxValue + 1L;
			}
		} catch (Exception ex) {
			LOGGER.warn("Failed to resolve max branch code from DB. Falling back to count strategy.", ex);
		}

		long count = branchRepository.count();
		return count + 1L;
	}

	private BranchResponse toResponse(Branch branch) {
		Long branchId = branch.getBranchId();
		long officerCount = branchId == null ? 0L : bankOfficerRepository.countByBranch_BranchId(branchId);
		long customerCount = branchId == null ? 0L : bankCustomerRepository.countByBranch_BranchId(branchId);

		return new BranchResponse(
			branchId,
			branch.getBranchCode(),
			branch.getBranchName(),
			branch.getBranchEmail(),
			branch.getBranchPhone(),
			branch.getAddress(),
			branch.getStatus() == null ? null : branch.getStatus().name(),
			branch.getUpdatedAt() == null ? null : branch.getUpdatedAt().toString(),
			officerCount,
			customerCount
		);
	}

	private String normalizeRequired(String value, String message) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}

	private String normalizeOptional(String value) {
		return value == null ? null : value.trim();
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}

	private BranchStatus normalizeStatus(String value) {
		String normalized = normalizeOptional(value);
		if (normalized == null || normalized.isBlank()) {
			return BranchStatus.ACTIVE;
		}

		normalized = normalized.toUpperCase(Locale.ROOT);

		try {
			return BranchStatus.valueOf(normalized);
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Status must be ACTIVE, INACTIVE, or MAINTENANCE.");
		}
	}
}
