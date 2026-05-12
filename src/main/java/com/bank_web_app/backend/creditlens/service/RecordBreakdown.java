package com.bank_web_app.backend.creditlens.service;

import java.math.BigDecimal;

record RecordBreakdown(
	BigDecimal income,
	BigDecimal loanEmi,
	BigDecimal creditCardBalance,
	BigDecimal creditCardLimit,
	BigDecimal otherLiabilities,
	BigDecimal estimatedCardMinimumPayment
) {
}
