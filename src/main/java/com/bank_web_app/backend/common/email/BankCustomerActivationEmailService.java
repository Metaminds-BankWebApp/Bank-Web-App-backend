package com.bank_web_app.backend.common.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Sends a one-time password setup link only after bank-customer onboarding is approved. */
@Service
public class BankCustomerActivationEmailService {
	private final EmailService emailService;
	private final String activationUrl;

	public BankCustomerActivationEmailService(
		EmailService emailService,
		@Value("${app.frontend.activation-url:${APP_FRONTEND_ACTIVATION_URL:http://localhost:3000/reset-password}}") String activationUrl
	) {
		this.emailService = emailService;
		this.activationUrl = activationUrl == null ? "http://localhost:3000/reset-password" : activationUrl.trim();
	}

	public void sendActivationEmail(String recipientEmail, String firstName, String username, String token) {
		if (recipientEmail == null || recipientEmail.isBlank() || token == null || token.isBlank()) {
			throw new IllegalArgumentException("Customer email and activation token are required.");
		}
		String link = activationUrl + (activationUrl.contains("?") ? "&" : "?") + "activationToken=" + token.trim();
		emailService.sendPlainText(recipientEmail.trim(), "Activate your PrimeCore customer account", String.join("\n",
			"Hello " + (firstName == null || firstName.isBlank() ? "Customer" : firstName.trim()) + ",",
			"",
			"Your bank onboarding has been completed. Set your own password using this secure one-time link.",
			"Username: " + (username == null ? "" : username.trim()),
			"Activate account: " + link,
			"",
			"This link expires in 24 hours. Do not forward it.",
			"",
			"PrimeCore Digital Banking Team"
		));
	}
}
