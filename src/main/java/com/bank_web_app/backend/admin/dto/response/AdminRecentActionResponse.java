package com.bank_web_app.backend.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminRecentActionResponse", description = "Single admin action entry for recent activity feeds.")
public record AdminRecentActionResponse(
	@Schema(description = "Audit log id", example = "82")
	Long actionId,
	@Schema(description = "Human readable action title", example = "Approved Branch Creation: \"Nuwara Eliya Main\"")
	String title,
	@Schema(description = "Optional action detail text", example = "Branch status set to ACTIVE")
	String details,
	@Schema(description = "Action category code", example = "BRANCH_CREATED")
	String actionType,
	@Schema(description = "Target entity type", example = "BRANCH")
	String targetType,
	@Schema(description = "Target entity identifier", example = "BR-0013")
	String targetId,
	@Schema(description = "Action tone for UI badges", example = "SUCCESS")
	String tone,
	@Schema(description = "Actor display name", example = "Kamal Edirisinghe")
	String actorName,
	@Schema(description = "Actor role", example = "ADMIN")
	String actorRole,
	@Schema(description = "Source IP address", example = "192.168.1.24")
	String ipAddress,
	@Schema(description = "Action creation timestamp in ISO format", example = "2026-04-30T13:05:29.551")
	String createdAt
) {
}
