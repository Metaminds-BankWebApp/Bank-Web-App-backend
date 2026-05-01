package com.bank_web_app.backend.admin.service;

import com.bank_web_app.backend.admin.dto.response.AdminAuditLogFilterOptionsResponse;
import com.bank_web_app.backend.admin.dto.response.AdminAuditLogPageResponse;
import com.bank_web_app.backend.admin.dto.response.AdminRecentActionResponse;
import com.bank_web_app.backend.admin.entity.AuditLog;
import com.bank_web_app.backend.admin.repository.AuditLogRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogService.class);
	private static final String REQUEST_LOGGED_ATTRIBUTE = "primecore.audit.logged";
	private static final int DEFAULT_HOURS = 12;
	private static final int DEFAULT_LIMIT = 20;
	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_HOURS = 168;
	private static final int MAX_LIMIT = 100;
	private static final int MAX_PAGE_SIZE = 200;

	private final AuditLogRepository auditLogRepository;
	private final UserRepository userRepository;

	public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
		this.auditLogRepository = auditLogRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public void logAction(
		String actionType,
		String title,
		String targetType,
		String targetId,
		String details,
		String tone
	) {
		logAction(actionType, title, targetType, targetId, details, tone, resolveRequestIpAddress());
	}

	@Transactional
	public void logAction(
		String actionType,
		String title,
		String targetType,
		String targetId,
		String details,
		String tone,
		String ipAddress
	) {
		try {
			Optional<User> actor = resolveAuthenticatedUser();
			AuditLog log = new AuditLog();
			log.setActorUser(actor.orElse(null));
			log.setActorName(trimToLength(resolveActorDisplayName(actor.orElse(null)), 150));
			log.setActorRole(normalizeNullable(resolveActorRole(actor.orElse(null)), 60));
			log.setActionType(normalize(actionType, "ACTION", 80));
			log.setTitle(normalize(title, "Admin action", 255));
			log.setTargetType(normalizeNullable(targetType, 50));
			log.setTargetId(normalizeNullable(targetId, 100));
			log.setDetails(trimToLength(details, 500));
			log.setTone(normalizeTone(tone));
			log.setIpAddress(normalizeNullable(ipAddress, 64));
			auditLogRepository.save(log);
			markLoggedForCurrentRequest();
		} catch (Exception ex) {
			LOGGER.warn("Failed to record admin audit log entry.", ex);
		}
	}

	@Transactional(readOnly = true)
	public AdminAuditLogPageResponse getAuditLogs(
		Integer page,
		Integer size,
		LocalDateTime fromDateTime,
		LocalDateTime toDateTime,
		String tone,
		String actionType,
		String actorRole,
		String targetType,
		String actorName,
		String query
	) {
		int normalizedPage = normalizePage(page);
		int normalizedPageSize = normalizePageSize(size);
		String normalizedTone = normalizeNullable(tone, 20);
		String normalizedActionType = normalizeNullable(actionType, 80);
		String normalizedActorRole = normalizeNullable(actorRole, 60);
		String normalizedTargetType = normalizeNullable(targetType, 50);
		String normalizedActorName = normalizeNullable(actorName, 150);
		String normalizedQuery = normalizeNullable(query, 100);

		Specification<AuditLog> spec = buildSearchSpecification(
			fromDateTime,
			toDateTime,
			normalizedTone,
			normalizedActionType,
			normalizedActorRole,
			normalizedTargetType,
			normalizedActorName,
			normalizedQuery
		);

		Page<AuditLog> result = auditLogRepository.findAll(
			spec,
			PageRequest.of(
				normalizedPage - 1,
				normalizedPageSize,
				Sort.by(Sort.Direction.DESC, "createdAt")
			)
		);

		return new AdminAuditLogPageResponse(
			normalizedPage,
			normalizedPageSize,
			result.getTotalElements(),
			result.getTotalPages(),
			result.getContent().stream().map(this::toResponse).toList()
		);
	}

	@Transactional(readOnly = true)
	public List<AdminRecentActionResponse> getRecentActions(Integer hours, Integer limit) {
		int normalizedHours = normalizeHours(hours);
		int normalizedLimit = normalizeLimit(limit);
		LocalDateTime threshold = LocalDateTime.now().minusHours(normalizedHours);

		return auditLogRepository
			.findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
				threshold,
				PageRequest.of(0, normalizedLimit)
			)
			.stream()
			.map(this::toResponse)
			.toList();
	}

	private AdminRecentActionResponse toResponse(AuditLog log) {
		return new AdminRecentActionResponse(
			log.getAuditLogId(),
			log.getTitle(),
			log.getDetails(),
			log.getActionType(),
			log.getTargetType(),
			log.getTargetId(),
			log.getTone(),
			log.getActorName(),
			log.getActorRole(),
			log.getIpAddress(),
			log.getCreatedAt() == null ? null : log.getCreatedAt().toString()
		);
	}

	@Transactional(readOnly = true)
	public AdminAuditLogFilterOptionsResponse getFilterOptions() {
		return new AdminAuditLogFilterOptionsResponse(
			cleanValues(auditLogRepository.findDistinctActionTypes()),
			cleanValues(auditLogRepository.findDistinctTones()),
			cleanValues(auditLogRepository.findDistinctActorRoles()),
			cleanValues(auditLogRepository.findDistinctTargetTypes())
		);
	}

	public boolean hasLoggedActionForCurrentRequest() {
		ServletRequestAttributes attributes = getRequestAttributes();
		if (attributes == null) {
			return false;
		}
		Object value = attributes.getRequest().getAttribute(REQUEST_LOGGED_ATTRIBUTE);
		return value instanceof Boolean logged && logged;
	}

	private Optional<User> resolveAuthenticatedUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (
			authentication == null ||
			!authentication.isAuthenticated() ||
			authentication instanceof AnonymousAuthenticationToken
		) {
			return Optional.empty();
		}

		String principal = authentication.getName();
		if (principal == null || principal.isBlank()) {
			return Optional.empty();
		}

		String normalizedPrincipal = principal.trim().toLowerCase(Locale.ROOT);
		return userRepository
			.findByEmail(normalizedPrincipal)
			.or(() -> userRepository.findByUsername(principal.trim()))
			.or(() -> userRepository.findByUsername(normalizedPrincipal));
	}

	private String resolveRequestIpAddress() {
		ServletRequestAttributes attributes = getRequestAttributes();
		if (attributes == null) {
			return null;
		}

		HttpServletRequest request = attributes.getRequest();
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			String first = forwardedFor.split(",")[0];
			if (first != null && !first.trim().isBlank()) {
				return first.trim();
			}
		}

		String realIp = request.getHeader("X-Real-IP");
		if (realIp != null && !realIp.isBlank()) {
			return realIp.trim();
		}

		return request.getRemoteAddr();
	}

	private String resolveActorDisplayName(User actor) {
		if (actor == null) {
			return "System";
		}
		String firstName = actor.getFirstName() == null ? "" : actor.getFirstName().trim();
		String lastName = actor.getLastName() == null ? "" : actor.getLastName().trim();
		String fullName = (firstName + " " + lastName).trim();
		if (!fullName.isBlank()) {
			return fullName;
		}
		if (actor.getUsername() != null && !actor.getUsername().trim().isBlank()) {
			return actor.getUsername().trim();
		}
		return actor.getEmail() == null || actor.getEmail().trim().isBlank() ? "System" : actor.getEmail().trim();
	}

	private String resolveActorRole(User actor) {
		if (actor == null || actor.getRole() == null || actor.getRole().getRoleName() == null) {
			return "SYSTEM";
		}
		return actor.getRole().getRoleName().trim();
	}

	private Specification<AuditLog> buildSearchSpecification(
		LocalDateTime fromDateTime,
		LocalDateTime toDateTime,
		String tone,
		String actionType,
		String actorRole,
		String targetType,
		String actorName,
		String query
	) {
		return (root, ignoredQuery, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (fromDateTime != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDateTime));
			}
			if (toDateTime != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDateTime));
			}
			if (tone != null) {
				predicates.add(cb.equal(cb.upper(root.get("tone")), tone.toUpperCase(Locale.ROOT)));
			}
			if (actionType != null) {
				predicates.add(cb.equal(cb.upper(root.get("actionType")), actionType.toUpperCase(Locale.ROOT)));
			}
			if (actorRole != null) {
				predicates.add(cb.equal(cb.upper(root.get("actorRole")), actorRole.toUpperCase(Locale.ROOT)));
			}
			if (targetType != null) {
				predicates.add(cb.equal(cb.upper(root.get("targetType")), targetType.toUpperCase(Locale.ROOT)));
			}
			if (actorName != null) {
				predicates.add(cb.like(cb.lower(root.get("actorName")), "%" + actorName.toLowerCase(Locale.ROOT) + "%"));
			}
			if (query != null) {
				String search = "%" + query.toLowerCase(Locale.ROOT) + "%";
				Expression<String> title = cb.lower(cb.coalesce(root.get("title"), ""));
				Expression<String> details = cb.lower(cb.coalesce(root.get("details"), ""));
				Expression<String> targetIdExpr = cb.lower(cb.coalesce(root.get("targetId"), ""));
				Expression<String> ipExpr = cb.lower(cb.coalesce(root.get("ipAddress"), ""));
				predicates.add(
					cb.or(
						cb.like(title, search),
						cb.like(details, search),
						cb.like(targetIdExpr, search),
						cb.like(ipExpr, search)
					)
				);
			}

			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	private int normalizeHours(Integer value) {
		if (value == null || value <= 0) {
			return DEFAULT_HOURS;
		}
		return Math.min(value, MAX_HOURS);
	}

	private int normalizeLimit(Integer value) {
		if (value == null || value <= 0) {
			return DEFAULT_LIMIT;
		}
		return Math.min(value, MAX_LIMIT);
	}

	private int normalizePage(Integer value) {
		if (value == null || value <= 0) {
			return DEFAULT_PAGE;
		}
		return value;
	}

	private int normalizePageSize(Integer value) {
		if (value == null || value <= 0) {
			return DEFAULT_PAGE_SIZE;
		}
		return Math.min(value, MAX_PAGE_SIZE);
	}

	private String normalize(String value, String fallback, int maxLength) {
		String normalized = value == null ? "" : value.trim();
		return normalized.isBlank() ? trimToLength(fallback, maxLength) : trimToLength(normalized, maxLength);
	}

	private String normalizeNullable(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.isBlank()) {
			return null;
		}
		return trimToLength(normalized, maxLength);
	}

	private String normalizeTone(String tone) {
		String normalized = tone == null ? "" : tone.trim().toUpperCase(Locale.ROOT);
		return switch (normalized) {
			case "SUCCESS", "WARNING", "INFO", "ERROR" -> normalized;
			default -> "INFO";
		};
	}

	private String trimToLength(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

	private List<String> cleanValues(List<String> values) {
		return values
			.stream()
			.map(value -> value == null ? null : value.trim())
			.filter(value -> value != null && !value.isBlank())
			.distinct()
			.toList();
	}

	private void markLoggedForCurrentRequest() {
		ServletRequestAttributes attributes = getRequestAttributes();
		if (attributes == null) {
			return;
		}
		attributes.getRequest().setAttribute(REQUEST_LOGGED_ATTRIBUTE, Boolean.TRUE);
	}

	private ServletRequestAttributes getRequestAttributes() {
		try {
			return (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		} catch (Exception ex) {
			return null;
		}
	}
}
