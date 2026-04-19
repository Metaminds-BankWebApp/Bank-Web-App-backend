package com.bank_web_app.backend.admin.service;

import com.bank_web_app.backend.admin.dto.request.BranchRequest;
import com.bank_web_app.backend.admin.dto.response.BranchResponse;
import com.bank_web_app.backend.admin.entity.Branch;
import com.bank_web_app.backend.admin.repository.BranchRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BranchService {

	private final BranchRepository branchRepository;

	public BranchService(BranchRepository branchRepository) {
		this.branchRepository = branchRepository;
	}

	@Transactional
	public BranchResponse create(BranchRequest request) {
		String code = normalizeRequired(request.branchCode(), "Branch code is required.");
		if (branchRepository.existsByBranchCode(code)) {
			throw new IllegalArgumentException("Branch code is already in use.");
		}

		Branch branch = new Branch();
		branch.setBranchCode(code);
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

		String code = normalizeRequired(request.branchCode(), "Branch code is required.");
		branchRepository.findByBranchCode(code).ifPresent(existing -> {
			if (!existing.getBranchId().equals(branchId)) {
				throw new IllegalArgumentException("Branch code is already in use.");
			}
		});

		branch.setBranchCode(code);
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
		return branchRepository.findById(branchId).orElseThrow(() -> new IllegalArgumentException("Branch not found."));
	}

	private BranchResponse toResponse(Branch branch) {
		return new BranchResponse(
			branch.getBranchId(),
			branch.getBranchCode(),
			branch.getBranchName(),
			branch.getBranchEmail(),
			branch.getBranchPhone(),
			branch.getAddress(),
			branch.getStatus(),
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

	private String normalizeStatus(String value) {
		String normalized = normalizeOptional(value);
		if (normalized == null || normalized.isBlank()) {
			return "ACTIVE";
		}
		normalized = normalized.toUpperCase(Locale.ROOT);
		if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
			throw new IllegalArgumentException("Status must be ACTIVE or INACTIVE.");
		}
		return normalized;
	}
}

