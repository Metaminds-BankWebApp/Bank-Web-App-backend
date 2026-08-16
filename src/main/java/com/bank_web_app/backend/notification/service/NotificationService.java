package com.bank_web_app.backend.notification.service;

import com.bank_web_app.backend.notification.dto.response.NotificationPageResponse;
import com.bank_web_app.backend.notification.dto.response.NotificationResponse;
import com.bank_web_app.backend.notification.dto.response.UnreadNotificationCountResponse;
import com.bank_web_app.backend.notification.entity.Notification;
import com.bank_web_app.backend.notification.entity.NotificationSeverity;
import com.bank_web_app.backend.notification.entity.NotificationSource;
import com.bank_web_app.backend.notification.entity.NotificationType;
import com.bank_web_app.backend.notification.repository.NotificationRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

	private static final TypeReference<Map<String, String>> STRING_MAP_TYPE = new TypeReference<>() {};
	private static final String FINANCIAL_DETAILS_MISSING_KEY_PREFIX = "public-financial-details-missing:";

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final NotificationCurrentUserService currentUserService;
	private final ObjectMapper objectMapper;

	public NotificationService(
		NotificationRepository notificationRepository,
		UserRepository userRepository,
		NotificationCurrentUserService currentUserService
	) {
		this.notificationRepository = notificationRepository;
		this.userRepository = userRepository;
		this.currentUserService = currentUserService;
		this.objectMapper = new ObjectMapper();
	}

	@Transactional(readOnly = true)
	public NotificationPageResponse getMyNotifications(
		NotificationSource source,
		boolean unreadOnly,
		int page,
		int size
	) {
		Long userId = currentUserService.resolveRequiredUser().getUserId();
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 100);
		Specification<Notification> requested = visibleSpecification(userId, source, unreadOnly, null);

		Page<Notification> result = notificationRepository.findAll(
			requested,
			PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
		);
		long unreadCount = notificationRepository.count(visibleSpecification(userId, source, true, null));
		long actionNeededCount = notificationRepository.count(
			visibleSpecification(
				userId,
				source,
				false,
				List.of(NotificationSeverity.WARNING, NotificationSeverity.ALERT)
			)
		);

		return new NotificationPageResponse(
			result.getContent().stream().map(this::toResponse).toList(),
			result.getNumber(),
			result.getSize(),
			result.getTotalElements(),
			result.getTotalPages(),
			unreadCount,
			actionNeededCount
		);
	}

	@Transactional(readOnly = true)
	public UnreadNotificationCountResponse getMyUnreadCount(NotificationSource source) {
		Long userId = currentUserService.resolveRequiredUser().getUserId();
		return new UnreadNotificationCountResponse(
			notificationRepository.count(visibleSpecification(userId, source, true, null))
		);
	}

	@Transactional
	public NotificationResponse markMyNotificationRead(Long notificationId) {
		Notification notification = findOwnedVisibleNotification(notificationId);
		if (notification.getReadAt() == null) {
			notification.setReadAt(LocalDateTime.now());
		}
		return toResponse(notificationRepository.save(notification));
	}

	@Transactional
	public void markAllMyNotificationsRead(NotificationSource source) {
		Long userId = currentUserService.resolveRequiredUser().getUserId();
		LocalDateTime now = LocalDateTime.now();
		List<Notification> unread = notificationRepository.findAll(visibleSpecification(userId, source, true, null));
		unread.forEach(notification -> notification.setReadAt(now));
		notificationRepository.saveAll(unread);
	}

	@Transactional
	public void dismissMyNotification(Long notificationId) {
		Notification notification = findOwnedVisibleNotification(notificationId);
		if (notification.getType() == NotificationType.FINANCIAL_DETAILS_MISSING) {
			throw new ResponseStatusException(
				HttpStatus.CONFLICT,
				"Complete your financial details before removing this notification."
			);
		}
		notification.setDismissedAt(LocalDateTime.now());
		notificationRepository.save(notification);
	}

	@Transactional
	public void resolveFinancialDetailsMissing(Long recipientUserId) {
		if (recipientUserId == null || recipientUserId <= 0) return;
		LocalDateTime now = LocalDateTime.now();
		List<Notification> reminders = notificationRepository
			.findAllByRecipient_UserIdAndTypeAndDismissedAtIsNull(
				recipientUserId,
				NotificationType.FINANCIAL_DETAILS_MISSING
			);
		reminders.forEach(notification -> notification.setDismissedAt(now));
		notificationRepository.saveAll(reminders);
	}

	public static String financialDetailsMissingDeduplicationKey(Long recipientUserId) {
		return FINANCIAL_DETAILS_MISSING_KEY_PREFIX + recipientUserId;
	}

	@Transactional
	public Notification createOrRefresh(NotificationCommand command) {
		validateCommand(command);
		User recipient = userRepository
			.findById(command.recipientUserId())
			.orElseThrow(() -> new IllegalArgumentException("Notification recipient was not found."));

		Notification notification = null;
		if (command.deduplicationKey() != null && !command.deduplicationKey().isBlank()) {
			notification = notificationRepository
				.findByRecipient_UserIdAndDeduplicationKey(recipient.getUserId(), command.deduplicationKey().trim())
				.orElse(null);
		}

		if (notification != null) {
			if (!command.aggregate()) {
				return notification;
			}
			notification.setAffectedCount(Math.max(1, notification.getAffectedCount()) + 1);
			notification.setReadAt(null);
			notification.setDismissedAt(null);
			notification.setTitle(command.title().trim());
			notification.setMessage(command.message().trim());
			notification.setSeverity(command.severity());
			notification.setActionKey(normalizeOptional(command.actionKey()));
			notification.setActionMetadata(serializeMetadata(command.actionMetadata()));
			return notificationRepository.save(notification);
		}

		Notification created = new Notification();
		created.setRecipient(recipient);
		created.setType(command.type());
		created.setSource(command.source());
		created.setSeverity(command.severity());
		created.setTitle(command.title().trim());
		created.setMessage(command.message().trim());
		created.setActionKey(normalizeOptional(command.actionKey()));
		created.setActionMetadata(serializeMetadata(command.actionMetadata()));
		created.setDeduplicationKey(normalizeOptional(command.deduplicationKey()));
		created.setAffectedCount(1);
		return notificationRepository.save(created);
	}

	private Notification findOwnedVisibleNotification(Long notificationId) {
		if (notificationId == null || notificationId <= 0) {
			throw new IllegalArgumentException("Notification id must be a positive number.");
		}
		Long userId = currentUserService.resolveRequiredUser().getUserId();
		return notificationRepository
			.findByNotificationIdAndRecipient_UserIdAndDismissedAtIsNull(notificationId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification was not found."));
	}

	private Specification<Notification> visibleSpecification(
		Long userId,
		NotificationSource source,
		boolean unreadOnly,
		List<NotificationSeverity> severities
	) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("recipient").get("userId"), userId));
			predicates.add(criteriaBuilder.isNull(root.get("dismissedAt")));
			if (source != null) {
				predicates.add(criteriaBuilder.equal(root.get("source"), source));
			}
			if (unreadOnly) {
				predicates.add(criteriaBuilder.isNull(root.get("readAt")));
			}
			if (severities != null && !severities.isEmpty()) {
				predicates.add(root.get("severity").in(severities));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private NotificationResponse toResponse(Notification notification) {
		return new NotificationResponse(
			notification.getNotificationId(),
			notification.getType(),
			notification.getSource(),
			notification.getSeverity(),
			notification.getTitle(),
			notification.getMessage(),
			notification.getActionKey(),
			deserializeMetadata(notification.getActionMetadata()),
			notification.getAffectedCount(),
			notification.getReadAt() == null,
			notification.getCreatedAt(),
			notification.getUpdatedAt()
		);
	}

	private String serializeMetadata(Map<String, String> metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return null;
		}
		Map<String, String> safeMetadata = new LinkedHashMap<>();
		metadata.forEach((key, value) -> {
			if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
				safeMetadata.put(key.trim(), value.trim());
			}
		});
		if (safeMetadata.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(safeMetadata);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Notification action metadata is invalid.", ex);
		}
	}

	private Map<String, String> deserializeMetadata(String metadata) {
		if (metadata == null || metadata.isBlank()) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(metadata, STRING_MAP_TYPE);
		} catch (JsonProcessingException ex) {
			return Map.of();
		}
	}

	private void validateCommand(NotificationCommand command) {
		if (command == null || command.recipientUserId() == null || command.recipientUserId() <= 0) {
			throw new IllegalArgumentException("A valid notification recipient is required.");
		}
		if (command.type() == null || command.source() == null || command.severity() == null) {
			throw new IllegalArgumentException("Notification type, source, and severity are required.");
		}
		if (command.title() == null || command.title().isBlank() || command.message() == null || command.message().isBlank()) {
			throw new IllegalArgumentException("Notification title and message are required.");
		}
	}

	private String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
