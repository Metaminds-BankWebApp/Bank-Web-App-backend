package com.bank_web_app.backend.support.service;

import com.bank_web_app.backend.common.email.EmailService;
import com.bank_web_app.backend.notification.service.NotificationCurrentUserService;
import com.bank_web_app.backend.support.dto.SupportRequest;
import com.bank_web_app.backend.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SupportRequestService {
	private final EmailService emailService; private final NotificationCurrentUserService currentUserService; private final String supportAdminEmail;
	public SupportRequestService(EmailService emailService, NotificationCurrentUserService currentUserService, @Value("${SUPPORT_ADMIN_EMAIL:}") String supportAdminEmail) {
		this.emailService = emailService; this.currentUserService = currentUserService; this.supportAdminEmail = supportAdminEmail == null ? "" : supportAdminEmail.trim();
	}
	public void submit(SupportRequest request) {
		if (supportAdminEmail.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Support is not configured.");
		User user = currentUserService.resolveRequiredUser();
		String role = user.getRole() == null ? "UNKNOWN" : user.getRole().getRoleName();
		String body = "A signed-in user submitted a support request.\n\nCategory: " + request.category().trim() + "\nSubject: " + request.subject().trim() + "\nUser: " + user.getEmail() + "\nRole: " + role + "\n\nMessage:\n" + request.message().trim();
		emailService.sendPlainText(supportAdminEmail, "[Support] " + request.subject().trim(), body);
	}
}
