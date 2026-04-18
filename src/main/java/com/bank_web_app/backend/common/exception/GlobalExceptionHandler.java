package com.bank_web_app.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
		IllegalArgumentException ex,
		HttpServletRequest request
	) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalState(
		IllegalStateException ex,
		HttpServletRequest request
	) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(
		MethodArgumentNotValidException ex,
		HttpServletRequest request
	) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex
			.getBindingResult()
			.getFieldErrors()
			.forEach(error -> {
				String message = error.getDefaultMessage() == null ? (error.getField() + " is invalid.") : error.getDefaultMessage();
				fieldErrors.putIfAbsent(error.getField(), message);
			});

		String message = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> error.getDefaultMessage() == null ? (error.getField() + " is invalid.") : error.getDefaultMessage())
			.distinct()
			.collect(Collectors.joining("; "));

		if (message.isBlank()) {
			message = "Request validation failed.";
		}

		return build(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), fieldErrors.isEmpty() ? null : fieldErrors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
		HttpMessageNotReadableException ex,
		HttpServletRequest request
	) {
		return build(HttpStatus.BAD_REQUEST, "Malformed JSON request body.", request.getRequestURI());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
		DataIntegrityViolationException ex,
		HttpServletRequest request
	) {
		return build(HttpStatus.CONFLICT, "Request conflicts with existing data constraints.", request.getRequestURI());
	}

	@ExceptionHandler(DuplicateFieldsException.class)
	public ResponseEntity<ApiErrorResponse> handleDuplicateFields(
		DuplicateFieldsException ex,
		HttpServletRequest request
	) {
		return build(HttpStatus.CONFLICT, "Some values are already in use.", request.getRequestURI(), ex.getFieldErrors());
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatus(
		ResponseStatusException ex,
		HttpServletRequest request
	) {
		HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
		if (status == null) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		String message = ex.getReason() == null || ex.getReason().isBlank()
			? status.getReasonPhrase()
			: ex.getReason();
		return build(status, message, request.getRequestURI());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleGeneric(
		Exception ex,
		HttpServletRequest request
	) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.", request.getRequestURI());
	}

	private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, String path) {
		return build(status, message, path, null);
	}

	private ResponseEntity<ApiErrorResponse> build(
		HttpStatus status,
		String message,
		String path,
		Map<String, String> fieldErrors
	) {
		ApiErrorResponse body = new ApiErrorResponse(
			Instant.now(),
			status.value(),
			status.getReasonPhrase(),
			message,
			path,
			fieldErrors
		);
		return ResponseEntity.status(status).body(body);
	}
}
