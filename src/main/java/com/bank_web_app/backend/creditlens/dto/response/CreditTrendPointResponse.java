package com.bank_web_app.backend.creditlens.dto.response;

import java.time.LocalDateTime;

public record CreditTrendPointResponse(
	String monthKey,
	String monthLabel,
	Integer score,
	LocalDateTime evaluationDate
) {
}
