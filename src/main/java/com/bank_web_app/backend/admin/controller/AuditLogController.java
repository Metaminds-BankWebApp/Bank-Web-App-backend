package com.bank_web_app.backend.admin.controller;

import com.bank_web_app.backend.admin.dto.response.AdminAuditLogFilterOptionsResponse;
import com.bank_web_app.backend.admin.dto.response.AdminAuditLogPageResponse;
import com.bank_web_app.backend.admin.dto.response.AdminRecentActionResponse;
import com.bank_web_app.backend.admin.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@Tag(name = "Admin Audit Logs", description = "Admin audit-log endpoints")
public class AuditLogController {

	private final AuditLogService auditLogService;

	public AuditLogController(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@GetMapping
	@Operation(summary = "Search audit logs with pagination and filters")
	public ResponseEntity<AdminAuditLogPageResponse> getAuditLogs(
		@RequestParam(defaultValue = "1") Integer page,
		@RequestParam(defaultValue = "20") Integer size,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDateTime,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDateTime,
		@RequestParam(required = false) String tone,
		@RequestParam(required = false) String actionType,
		@RequestParam(required = false) String actorRole,
		@RequestParam(required = false) String targetType,
		@RequestParam(required = false) String actorName,
		@RequestParam(required = false) String query
	) {
		return ResponseEntity.ok(
			auditLogService.getAuditLogs(
				page,
				size,
				fromDateTime,
				toDateTime,
				tone,
				actionType,
				actorRole,
				targetType,
				actorName,
				query
			)
		);
	}

	@GetMapping("/recent")
	@Operation(summary = "Get recent admin actions within a time window")
	public ResponseEntity<List<AdminRecentActionResponse>> getRecent(
		@RequestParam(defaultValue = "12") Integer hours,
		@RequestParam(defaultValue = "20") Integer limit
	) {
		return ResponseEntity.ok(auditLogService.getRecentActions(hours, limit));
	}

	@GetMapping("/filters")
	@Operation(summary = "Get audit-log filter options")
	public ResponseEntity<AdminAuditLogFilterOptionsResponse> getFilterOptions() {
		return ResponseEntity.ok(auditLogService.getFilterOptions());
	}
}
