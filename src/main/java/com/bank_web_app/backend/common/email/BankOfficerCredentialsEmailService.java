package com.bank_web_app.backend.common.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BankOfficerCredentialsEmailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BankOfficerCredentialsEmailService.class);

	private final EmailService emailService;
	private final String officerSignInUrl;

	public BankOfficerCredentialsEmailService(
		EmailService emailService,
		@Value("${app.frontend.sign-in-url:${APP_FRONTEND_SIGN_IN_URL:http://localhost:3000/login}}") String officerSignInUrl
	) {
		this.emailService = emailService;
		this.officerSignInUrl = officerSignInUrl == null ? "http://localhost:3000/login" : officerSignInUrl.trim();
	}

	public void sendCredentialsEmail(String recipientEmail, String firstName, String username, String password) {
		String normalizedRecipient = recipientEmail == null ? "" : recipientEmail.trim();
		String normalizedFirstName = firstName == null ? "" : firstName.trim();
		String normalizedUsername = username == null ? "" : username.trim();
		String normalizedPassword = password == null ? "" : password;

		if (normalizedRecipient.isBlank()) {
			throw new IllegalArgumentException("Officer email is required to send credentials.");
		}

		String greetingName = normalizedFirstName.isBlank() ? "Officer" : normalizedFirstName;
		String subject = "Your PrimeCore Bank Officer Account Is Ready";
		String body = String.join("\n",
			"Hi " + greetingName + ",",
			"",
			"Your bank officer account has been created successfully.",
			"",
			"Username: " + normalizedUsername,
			"Password: " + normalizedPassword,
			"",
			"Sign In: " + officerSignInUrl,
			"",
			"Please sign in and change your password after your first login.",
			"",
			"Regards,",
			"PrimeCore Digital Banking Team"
		);

		try {
			emailService.sendPlainText(normalizedRecipient, subject, body);
			LOGGER.info("Bank officer credentials email sent successfully to {}", normalizedRecipient);
		} catch (RuntimeException ex) {
			LOGGER.error("Bank officer credentials email failed for {}", normalizedRecipient, ex);
			throw ex;
		}
	}
}
