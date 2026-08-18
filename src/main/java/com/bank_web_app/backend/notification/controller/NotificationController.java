package com.bank_web_app.backend.notification.controller;

import com.bank_web_app.backend.notification.dto.response.NotificationPageResponse;
import com.bank_web_app.backend.notification.dto.response.NotificationResponse;
import com.bank_web_app.backend.notification.dto.response.UnreadNotificationCountResponse;
import com.bank_web_app.backend.notification.entity.NotificationSource;
import com.bank_web_app.backend.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Shared authenticated notification inbox")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	@Operation(summary = "List notifications owned by the logged-in user")
	public ResponseEntity<NotificationPageResponse> getMine(
		@RequestParam(required = false) NotificationSource source,
		@RequestParam(defaultValue = "false") boolean unreadOnly,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ResponseEntity.ok(notificationService.getMyNotifications(source, unreadOnly, page, size));
	}

	@GetMapping("/unread-count")
	@Operation(summary = "Get the logged-in user's unread count")
	public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount(
		@RequestParam(required = false) NotificationSource source
	) {
		return ResponseEntity.ok(notificationService.getMyUnreadCount(source));
	}

	@PatchMapping("/{notificationId}/read")
	@Operation(summary = "Mark one owned notification as read")
	public ResponseEntity<NotificationResponse> markRead(@PathVariable Long notificationId) {
		return ResponseEntity.ok(notificationService.markMyNotificationRead(notificationId));
	}

	@PatchMapping("/read-all")
	@Operation(summary = "Mark all owned notifications as read")
	public ResponseEntity<Void> markAllRead(@RequestParam(required = false) NotificationSource source) {
		notificationService.markAllMyNotificationsRead(source);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{notificationId}")
	@Operation(summary = "Permanently delete one owned notification")
	public ResponseEntity<Void> dismiss(@PathVariable Long notificationId) {
		notificationService.deleteMyNotification(notificationId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping
	@Operation(summary = "Permanently clear all notifications owned by the logged-in user")
	public ResponseEntity<Void> clearAll() {
		notificationService.clearAllMyNotifications();
		return ResponseEntity.noContent().build();
	}
}
