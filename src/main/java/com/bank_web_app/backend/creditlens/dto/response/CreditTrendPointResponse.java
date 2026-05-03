package com.bank_web_app.backend.creditlens.dto.response;

import java.time.LocalDateTime;

/**
 * Represents one plotted monthly point in a CreditLens score trend series.
 */
public record CreditTrendPointResponse(
	String monthKey,
	String monthLabel,
	Integer score,
	LocalDateTime evaluationDate
) {
}
