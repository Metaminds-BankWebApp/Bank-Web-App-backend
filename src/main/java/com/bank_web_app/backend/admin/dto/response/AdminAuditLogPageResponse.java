package com.bank_web_app.backend.admin.dto.response;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "AdminAuditLogPageResponse", description = "Paginated response of system audit logs for admin.")
public record AdminAuditLogPageResponse(
	@Schema(description = "Current page number (1-based)", example = "1")
	int page,
	@Schema(description = "Page size", example = "20")
	int size,
	@Schema(description = "Total log count", example = "124")
	long totalElements,
	@Schema(description = "Total number of pages", example = "7")
	int totalPages,
	@Schema(description = "Audit log records for requested page")
	List<AdminRecentActionResponse> records
) {
}
