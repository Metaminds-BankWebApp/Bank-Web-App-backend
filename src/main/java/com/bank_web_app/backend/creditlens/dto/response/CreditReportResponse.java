package com.bank_web_app.backend.creditlens.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CreditReportResponse(
	String customerType,
	String evaluationType,
	LocalDateTime generatedAt,
	List<CreditReportSnapshotResponse> snapshots
) {
}
