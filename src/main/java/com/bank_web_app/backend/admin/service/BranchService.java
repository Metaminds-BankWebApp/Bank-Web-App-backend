package com.bank_web_app.backend.admin.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank_web_app.backend.admin.dto.request.BranchRequest;
import com.bank_web_app.backend.admin.dto.response.BranchResponse;
import com.bank_web_app.backend.admin.entity.Branch;
import com.bank_web_app.backend.admin.entity.BranchStatus;
import com.bank_web_app.backend.admin.repository.BranchRepository;

@Service
public class BranchService {

	private final BranchRepository branchRepository;

	public BranchService(BranchRepository branchRepository) {
		this.branchRepository = branchRepository;
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

		return toResponse(branchRepository.save(branch));
	}

	@Transactional(readOnly = true)
	public List<BranchResponse> getAll() {
		return branchRepository.findAll().stream().map(this::toResponse).toList();
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

		return toResponse(branchRepository.save(branch));
	}

	@Transactional
	public BranchResponse updateStatus(Long branchId, String status) {
		Branch branch = findBranch(branchId);
		branch.setStatus(normalizeStatus(status));
		return toResponse(branchRepository.save(branch));
	}

	private Branch findBranch(Long branchId) {
		return branchRepository.findById(branchId)
			.orElseThrow(() -> new IllegalArgumentException("Branch not found."));
	}

	private String generateBranchCode() {
		Long nextValue = branchRepository.getNextBranchCodeSequence();
		return String.format("BR-%04d", nextValue);
	}

	private BranchResponse toResponse(Branch branch) {
		return new BranchResponse(
			branch.getBranchId(),
			branch.getBranchCode(),
			branch.getBranchName(),
			branch.getBranchEmail(),
			branch.getBranchPhone(),
			branch.getAddress(),
			branch.getStatus() == null ? null : branch.getStatus().name(),
			branch.getUpdatedAt() == null ? null : branch.getUpdatedAt().toString()
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