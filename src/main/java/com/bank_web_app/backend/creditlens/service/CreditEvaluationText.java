package com.bank_web_app.backend.creditlens.service;

import java.util.Locale;

final class CreditEvaluationText {

	private CreditEvaluationText() {
	}

	static String normalizeText(String value) {
		return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
	}

	static String safe(String value) {
		return value == null ? "" : value.trim();
	}

	static String toTitleCase(String value) {
		String normalized = normalizeText(value).toLowerCase(Locale.ROOT);
		if (normalized.isBlank()) {
			return "";
		}
		return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
	}
}
