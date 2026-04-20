package com.bank_web_app.backend.admin.controller;

import com.bank_web_app.backend.admin.dto.request.RiskAdjustmentUpdateRequest;
import com.bank_web_app.backend.admin.dto.response.RiskAdjustmentResponse;
import com.bank_web_app.backend.admin.service.RiskAdjustmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/risk-adjustments")
@Tag(name = "Admin Risk Adjustments", description = "LoanSense risk adjustment management endpoints")
public class RiskAdjustmentController {

	private final RiskAdjustmentService riskAdjustmentService;

	public RiskAdjustmentController(RiskAdjustmentService riskAdjustmentService) {
		this.riskAdjustmentService = riskAdjustmentService;
	}

	@GetMapping
	@Operation(summary = "Get all LoanSense risk adjustments")
	public ResponseEntity<List<RiskAdjustmentResponse>> getAll() {
		return ResponseEntity.ok(riskAdjustmentService.getAll());
	}

	@GetMapping("/{adjustmentId}")
	@Operation(summary = "Get a LoanSense risk adjustment by id")
	public ResponseEntity<RiskAdjustmentResponse> getById(@PathVariable Long adjustmentId) {
		return ResponseEntity.ok(riskAdjustmentService.getById(adjustmentId));
	}

	@PutMapping("/{adjustmentId}")
	@Operation(summary = "Update a LoanSense risk adjustment")
	public ResponseEntity<RiskAdjustmentResponse> update(
		@PathVariable Long adjustmentId,
		@Valid @RequestBody RiskAdjustmentUpdateRequest request
	) {
		return ResponseEntity.ok(riskAdjustmentService.update(adjustmentId, request));
	}
}
