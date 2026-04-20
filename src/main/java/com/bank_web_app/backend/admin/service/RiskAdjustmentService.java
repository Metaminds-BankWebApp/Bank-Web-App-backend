package com.bank_web_app.backend.admin.service;

import com.bank_web_app.backend.admin.dto.request.RiskAdjustmentUpdateRequest;
import com.bank_web_app.backend.admin.dto.response.RiskAdjustmentResponse;
import com.bank_web_app.backend.admin.entity.RiskAdjustment;
import com.bank_web_app.backend.admin.repository.RiskAdjustmentRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskAdjustmentService {

	private static final Set<String> SUPPORTED_RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");

	private final RiskAdjustmentRepository riskAdjustmentRepository;

	public RiskAdjustmentService(RiskAdjustmentRepository riskAdjustmentRepository) {
		this.riskAdjustmentRepository = riskAdjustmentRepository;
	}

	@Transactional(readOnly = true)
	public List<RiskAdjustmentResponse> getAll() {
		return riskAdjustmentRepository.findAllByOrderByRiskLevelAsc().stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public RiskAdjustmentResponse getById(Long adjustmentId) {
		return toResponse(findAdjustment(adjustmentId));
	}

	@Transactional
	public RiskAdjustmentResponse update(Long adjustmentId, RiskAdjustmentUpdateRequest request) {
		RiskAdjustment adjustment = findAdjustment(adjustmentId);
		String riskLevel = normalizeRiskLevel(request.riskLevel());

		riskAdjustmentRepository.findByRiskLevel(riskLevel).ifPresent(existing -> {
			if (!existing.getAdjustmentId().equals(adjustmentId)) {
				throw new IllegalArgumentException("Risk level is already configured.");
			}
		});

		adjustment.setRiskLevel(riskLevel);
		adjustment.setMultiplier(normalizeMultiplier(request.multiplier()));
		adjustment.setDescription(normalizeOptionalText(request.description()));
		return toResponse(riskAdjustmentRepository.save(adjustment));
	}

	private RiskAdjustment findAdjustment(Long adjustmentId) {
		return riskAdjustmentRepository
			.findById(adjustmentId)
			.orElseThrow(() -> new IllegalArgumentException("Risk adjustment not found."));
	}

	private RiskAdjustmentResponse toResponse(RiskAdjustment adjustment) {
		return new RiskAdjustmentResponse(
			adjustment.getAdjustmentId(),
			adjustment.getRiskLevel(),
			toRiskLabel(adjustment.getRiskLevel()),
			adjustment.getMultiplier(),
			adjustment.getDescription(),
			adjustment.getUpdatedAt() == null ? null : adjustment.getUpdatedAt().toString()
		);
	}

	private String normalizeRiskLevel(String value) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("Risk level is required.");
		}
		String normalized = value.trim().toUpperCase(Locale.ROOT);
		if (!SUPPORTED_RISK_LEVELS.contains(normalized)) {
			throw new IllegalArgumentException("Risk level must be LOW, MEDIUM, or HIGH.");
		}
		return normalized;
	}

	private BigDecimal normalizeMultiplier(BigDecimal value) {
		if (value == null) {
			throw new IllegalArgumentException("Multiplier is required.");
		}
		if (value.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Multiplier must be greater than 0.");
		}
		return value;
	}

	private String normalizeOptionalText(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isBlank() ? null : normalized;
	}

	private String toRiskLabel(String riskLevel) {
		return switch (riskLevel == null ? "" : riskLevel) {
			case "LOW" -> "Low Risk";
			case "MEDIUM" -> "Medium Risk";
			case "HIGH" -> "High Risk";
			default -> riskLevel;
		};
	}
}
