package com.bank_web_app.backend.common.exception;

import java.util.Map;

public class DuplicateFieldsException extends RuntimeException {

	private final Map<String, String> fieldErrors;

	public DuplicateFieldsException(Map<String, String> fieldErrors) {
		super("Request contains duplicate values.");
		this.fieldErrors = Map.copyOf(fieldErrors);
	}

	public Map<String, String> getFieldErrors() {
		return fieldErrors;
	}
}
