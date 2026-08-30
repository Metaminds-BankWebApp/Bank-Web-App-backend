package com.bank_web_app.backend.common.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BankOfficerCredentialsEmailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BankOfficerCredentialsEmailService.class);

	private final EmailService emailService;
	private final String officerActivationUrl;

	public BankOfficerCredentialsEmailService(
		EmailService emailService,
		@Value("${app.frontend.activation-url:${APP_FRONTEND_ACTIVATION_URL:http://localhost:3000/reset-password}}") String officerActivationUrl
	) {
		this.emailService = emailService;
		this.officerActivationUrl = officerActivationUrl == null ? "http://localhost:3000/reset-password" : officerActivationUrl.trim();
	}

	public void sendActivationEmail(String recipientEmail, String firstName, String username, String activationToken) {
		String normalizedRecipient = recipientEmail == null ? "" : recipientEmail.trim();
		String normalizedFirstName = firstName == null ? "" : firstName.trim();
		String normalizedUsername = username == null ? "" : username.trim();
		String normalizedToken = activationToken == null ? "" : activationToken.trim();

		if (normalizedRecipient.isBlank()) {
			throw new IllegalArgumentException("Officer email is required to send an activation invitation.");
		}
		if (normalizedToken.isBlank()) throw new IllegalArgumentException("Activation token is required.");

		String greetingName = normalizedFirstName.isBlank() ? "Officer" : normalizedFirstName;
		String activationLink = officerActivationUrl + (officerActivationUrl.contains("?") ? "&" : "?") + "activationToken=" + normalizedToken;
		String subject = "Activate your PrimeCore Bank Officer Account";
		String body = String.join("\n",
			"Hi " + greetingName + ",",
			"",
			"Your bank officer account has been created. Set your own password using the secure activation link below.",
			"",
			"Username: " + normalizedUsername,
			"",
			"Activate account: " + activationLink,
			"",
			"This one-time link expires in 3 days. Do not forward it.",
			"",
			"Regards,",
			"PrimeCore Digital Banking Team"
		);

		try {
			emailService.sendPlainText(normalizedRecipient, subject, body);
			LOGGER.info("Bank officer activation email sent successfully to {}", normalizedRecipient);
		} catch (RuntimeException ex) {
			LOGGER.error("Bank officer activation email failed for {}", normalizedRecipient, ex);
			throw ex;
		}
	}
}
