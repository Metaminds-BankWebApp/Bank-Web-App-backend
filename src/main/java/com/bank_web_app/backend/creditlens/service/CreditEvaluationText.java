package com.bank_web_app.backend.creditlens.service;

import java.util.Locale;

final class CreditEvaluationText {

	// Prevents creating this static text helper class.
	private CreditEvaluationText() {
	}

	// Trims text and converts it to uppercase for comparisons.
	static String normalizeText(String value) {
		return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
	}

	// Trims nullable text without throwing errors.
	static String safe(String value) {
		return value == null ? "" : value.trim();
	}

	// Converts normalized risk text into frontend-friendly title case.
	static String toTitleCase(String value) {
		String normalized = normalizeText(value).toLowerCase(Locale.ROOT);
		if (normalized.isBlank()) {
			return "";
		}
		return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
	}
}
