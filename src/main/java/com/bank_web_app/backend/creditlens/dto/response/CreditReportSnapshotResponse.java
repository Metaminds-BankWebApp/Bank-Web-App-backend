package com.bank_web_app.backend.creditlens.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Snapshot row used inside the multi-month CreditLens report response.
 */
public record CreditReportSnapshotResponse(
	Long evaluationId,
	String monthLabel,
	BigDecimal income,
	BigDecimal loanEmi,
	BigDecimal creditCardBalance,
	BigDecimal creditCardLimit,
	BigDecimal otherLiabilities,
	Integer score,
	String riskLabel,
	String evaluationType,
	LocalDateTime lastUpdated,
	String lastUpdatedLabel,
	Integer missedPayments,
	Integer activeFacilities,
	BigDecimal dtiPercentage,
	BigDecimal utilizationPercentage,
	String dtiLabel,
	List<CreditRiskFactorResponse> factors
) {
}
