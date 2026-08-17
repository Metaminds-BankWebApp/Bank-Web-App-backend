package com.bank_web_app.backend.admin.audit;

import com.bank_web_app.backend.admin.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Safety-net interceptor that records meaningful request-level audit events for
 * mutating HTTP operations when business services did not log an action.
 */
@Component
public class SystemAuditLoggingInterceptor implements HandlerInterceptor {

	private static final String AUDIT_ELIGIBLE_ATTR = "primecore.audit.eligible";
	private static final Set<String> AUDITED_HTTP_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
	private static final Set<String> CUSTOMER_ROLES = Set.of("PUBLIC_CUSTOMER", "BANK_CUSTOMER");
	private static final Set<String> EXCLUDED_PATH_PREFIXES = Set.of(
		"/swagger-ui",
		"/swagger-ui.html",
		"/v3/api-docs",
		"/api/notifications"
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
		request.setAttribute(
			AUDIT_ELIGIBLE_ATTR,
			shouldAudit(request.getMethod(), request.getRequestURI(), resolveAuthenticatedActorRole())
		);
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
		AuditAction action = describeAction(method, path, targetType);
		boolean failed = ex != null || response.getStatus() >= 400;

		auditLogService.logAction(
			failed ? action.actionType() + "_FAILED" : action.actionType(),
			failed ? action.failureTitle() : action.successTitle(),
			targetType,
			targetId,
			buildDetails(handler, response.getStatus(), ex),
			failed ? "ERROR" : "INFO"
		);
	}

	private boolean shouldAudit(String method, String path, String actorRole) {
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

		// Staff and system write operations are fully audited. Customer audit entries
		// are intentionally limited to high-value security, application, and payment events.
		return !CUSTOMER_ROLES.contains(actorRole) || isImportantCustomerAction(path);
	}

	private boolean isImportantCustomerAction(String path) {
		String normalizedPath = path.toLowerCase(Locale.ROOT);
		return normalizedPath.equals("/api/users/profile")
			|| normalizedPath.endsWith("/financial-records/submit")
			|| normalizedPath.equals("/api/creditlens/public/evaluations")
			|| normalizedPath.equals("/api/bank-customers/transact/transactions/verify-otp")
			|| normalizedPath.startsWith("/api/bank-customers/transact/beneficiaries")
			|| normalizedPath.equals("/api/support/requests");
	}

	private String resolveAuthenticatedActorRole() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return "SYSTEM";
		}

		return authentication
			.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.filter(authority -> authority != null && authority.startsWith("ROLE_"))
			.map(authority -> authority.substring("ROLE_".length()).toUpperCase(Locale.ROOT))
			.findFirst()
			.orElse("SYSTEM");
	}

	private String deriveTargetType(String path) {
		String normalizedPath = path == null ? "" : path.toLowerCase(Locale.ROOT);
		if (normalizedPath.endsWith("/financial-records/submit")) {
			return "FINANCIAL_APPLICATION";
		}

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

	private AuditAction describeAction(String method, String path, String targetType) {
		String normalizedPath = path == null ? "" : path.toLowerCase(Locale.ROOT);
		if (normalizedPath.endsWith("/login")) {
			return new AuditAction("LOGIN", "Signed in", "Sign-in failed");
		}
		if (normalizedPath.endsWith("/logout")) {
			return new AuditAction("LOGOUT", "Signed out", "Sign-out failed");
		}
		if (normalizedPath.endsWith("/financial-records/submit")) {
			return new AuditAction(
				"SUBMIT_FINANCIAL_APPLICATION",
				"Submitted financial application",
				"Financial application submission failed"
			);
		}
		if (normalizedPath.endsWith("/transactions/verify-otp")) {
			return new AuditAction("CONFIRM_TRANSACTION", "Confirmed transfer", "Transfer confirmation failed");
		}

		String action = switch (method == null ? "" : method.toUpperCase(Locale.ROOT)) {
			case "POST" -> "CREATE";
			case "PUT", "PATCH" -> "UPDATE";
			case "DELETE" -> "DELETE";
			default -> "RECORD";
		};
		String targetLabel = toDisplayLabel(targetType == null ? "ACTION" : targetType);
		return switch (action) {
			case "CREATE" -> new AuditAction("CREATE_" + targetType, "Created " + targetLabel, "Failed to create " + targetLabel);
			case "UPDATE" -> new AuditAction("UPDATE_" + targetType, "Updated " + targetLabel, "Failed to update " + targetLabel);
			case "DELETE" -> new AuditAction("DELETE_" + targetType, "Deleted " + targetLabel, "Failed to delete " + targetLabel);
			default -> new AuditAction("RECORDED_" + targetType, "Recorded " + targetLabel, "Failed to record " + targetLabel);
		};
	}

	private String toDisplayLabel(String value) {
		String normalized = value == null ? "action" : value.trim().replace('_', ' ').toLowerCase(Locale.ROOT);
		if (normalized.isBlank()) {
			return "action";
		}
		return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
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

	private record AuditAction(String actionType, String successTitle, String failureTitle) {}

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
		mapping.put("requests", "SUPPORT_REQUEST");
		mapping.put("loan-eligibility", "LOAN_ELIGIBILITY");
		mapping.put("otp", "OTP");
		mapping.put("password", "PASSWORD");
		return mapping;
	}
}
