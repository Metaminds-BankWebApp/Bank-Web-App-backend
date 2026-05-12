package com.bank_web_app.backend.creditlens.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class CreditEvaluationAmounts {

	private static final BigDecimal ESTIMATED_CARD_MIN_PAYMENT_RATIO = new BigDecimal("0.05");

	private CreditEvaluationAmounts() {
	}

	static BigDecimal estimateCardMinimumPayment(BigDecimal totalCardOutstanding) {
		return safeAmount(totalCardOutstanding)
			.multiply(ESTIMATED_CARD_MIN_PAYMENT_RATIO)
			.setScale(2, RoundingMode.HALF_UP);
	}

	static BigDecimal safeAmount(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	static BigDecimal toPercentage(BigDecimal ratio) {
		return safeAmount(ratio)
			.multiply(new BigDecimal("100"))
			.setScale(1, RoundingMode.HALF_UP);
	}
}
