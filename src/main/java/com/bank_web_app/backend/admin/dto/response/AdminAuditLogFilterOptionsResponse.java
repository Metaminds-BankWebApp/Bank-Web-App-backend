package com.bank_web_app.backend.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "AdminAuditLogFilterOptionsResponse", description = "Filter dropdown values derived from existing audit logs.")
public record AdminAuditLogFilterOptionsResponse(
	@Schema(description = "Available action types")
	List<String> actionTypes,
	@Schema(description = "Available tones")
	List<String> tones,
	@Schema(description = "Available actor roles")
	List<String> actorRoles,
	@Schema(description = "Available target types")
	List<String> targetTypes
) {
}
