package com.bank_web_app.backend.admin.audit;

import com.bank_web_app.backend.admin.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SystemAuditLoggingInterceptor implements HandlerInterceptor {

	private static final String AUDIT_ELIGIBLE_ATTR = "primecore.audit.eligible";
	private static final Set<String> AUDITED_HTTP_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
	private static final Set<String> EXCLUDED_PATH_PREFIXES = Set.of(
		"/swagger-ui",
		"/swagger-ui.html",
		"/v3/api-docs"
	);
	private static final Set<String> NON_TARGET_SEGMENTS = Set.of(
		"api",
		"admin",
		"auth",
		"dashboard",
		"status",
		"summary",
		"recent",
		"recent-actions",
		"filters",
		"history",
		"current",
		"continue",
		"draft",
		"steps",
		"verify",
		"verify-otp",
		"resend-otp",
		"initiate",
		"report"
	);
	private static final Map<String, String> RESOURCE_TYPE_MAP = createResourceTypeMap();

	private final AuditLogService auditLogService;

	public SystemAuditLoggingInterceptor(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		request.setAttribute(AUDIT_ELIGIBLE_ATTR, shouldAudit(request.getMethod(), request.getRequestURI()));
		return true;
	}

	@Override
	public void afterCompletion(
		HttpServletRequest request,
		HttpServletResponse response,
		Object handler,
		Exception ex
	) {
		Object eligible = request.getAttribute(AUDIT_ELIGIBLE_ATTR);
		if (!(eligible instanceof Boolean isEligible) || !isEligible) {
			return;
		}

		if (auditLogService.hasLoggedActionForCurrentRequest()) {
			return;
		}

		String method = request.getMethod();
		String path = request.getRequestURI();
		String targetType = deriveTargetType(path);
		String targetId = deriveTargetId(path);
		String actionType = method + "_" + (targetType == null ? "ACTION" : targetType);
		boolean failed = ex != null || response.getStatus() >= 400;

		auditLogService.logAction(
			failed ? actionType + "_FAILED" : actionType,
			(failed ? "Failed " : "Executed ") + method + " on " + path,
			targetType,
			targetId,
			buildDetails(handler, response.getStatus(), ex),
			failed ? "ERROR" : "INFO"
		);
	}

	private boolean shouldAudit(String method, String path) {
		if (method == null || path == null) {
			return false;
		}

		String normalizedMethod = method.toUpperCase(Locale.ROOT);
		if (!AUDITED_HTTP_METHODS.contains(normalizedMethod)) {
			return false;
		}

		for (String prefix : EXCLUDED_PATH_PREFIXES) {
			if (path.startsWith(prefix)) {
				return false;
			}
		}

		return true;
	}

	private String deriveTargetType(String path) {
		String[] segments = splitPath(path);
		for (int i = segments.length - 1; i >= 0; i--) {
			String normalized = normalizeSegment(segments[i]);
			if (normalized.isBlank()) {
				continue;
			}
			String mapped = RESOURCE_TYPE_MAP.get(normalized);
			if (mapped != null) {
				return mapped;
			}
		}

		for (int i = segments.length - 1; i >= 0; i--) {
			String normalized = normalizeSegment(segments[i]);
			if (normalized.isBlank() || NON_TARGET_SEGMENTS.contains(normalized) || isIdentifierSegment(normalized)) {
				continue;
			}
			return toEnumValue(singularize(normalized));
		}

		return "SYSTEM";
	}

	private String deriveTargetId(String path) {
		String[] segments = splitPath(path);
		for (int i = segments.length - 1; i >= 0; i--) {
			String raw = segments[i] == null ? "" : segments[i].trim();
			String normalized = normalizeSegment(raw);
			if (normalized.isBlank() || NON_TARGET_SEGMENTS.contains(normalized)) {
				continue;
			}
			if (isIdentifierSegment(raw)) {
				return raw;
			}
		}
		return null;
	}

	private boolean isIdentifierSegment(String value) {
		if (value == null) {
			return false;
		}

		String normalized = value.trim();
		if (normalized.isBlank()) {
			return false;
		}

		return (
			normalized.matches("\\d+") ||
			normalized.matches(".*\\d.*-.*") ||
			normalized.matches(".*-.*\\d.*") ||
			normalized.matches("[0-9a-fA-F]{8}-[0-9a-fA-F\\-]{27}")
		);
	}

	private String singularize(String value) {
		if (value.endsWith("ies") && value.length() > 3) {
			return value.substring(0, value.length() - 3) + "y";
		}
		if (value.endsWith("s") && value.length() > 1) {
			return value.substring(0, value.length() - 1);
		}
		return value;
	}

	private String normalizeSegment(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private String[] splitPath(String path) {
		return path == null ? new String[0] : path.split("/");
	}

	private String toEnumValue(String value) {
		return value.toUpperCase(Locale.ROOT).replace("-", "_");
	}

	private String buildDetails(Object handler, int statusCode, Exception ex) {
		String handlerName = handler == null ? "UnknownHandler" : handler.getClass().getSimpleName();
		String statusPart = "HTTP " + statusCode;
		String errorPart = ex == null ? null : ex.getMessage();
		String details = statusPart + " | Handler " + handlerName + (errorPart == null ? "" : " | " + errorPart);
		return trim(details, 500);
	}

	private String trim(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

	private static Map<String, String> createResourceTypeMap() {
		Map<String, String> mapping = new HashMap<>();
		mapping.put("branches", "BRANCH");
		mapping.put("branch", "BRANCH");
		mapping.put("bank-officers", "BANK_OFFICER");
		mapping.put("officers", "BANK_OFFICER");
		mapping.put("users", "USER");
		mapping.put("public-customers", "PUBLIC_CUSTOMER");
		mapping.put("customers", "CUSTOMER");
		mapping.put("loan-policies", "LOAN_POLICY");
		mapping.put("transactions", "TRANSACTION");
		mapping.put("beneficiaries", "BENEFICIARY");
		mapping.put("expenses", "EXPENSE");
		mapping.put("incomes", "INCOME");
		mapping.put("budgets", "BUDGET");
		mapping.put("evaluations", "EVALUATION");
		mapping.put("profile", "PROFILE");
		mapping.put("financial-records", "FINANCIAL_RECORD");
		mapping.put("loan-eligibility", "LOAN_ELIGIBILITY");
		mapping.put("otp", "OTP");
		mapping.put("password", "PASSWORD");
		return mapping;
	}
}
