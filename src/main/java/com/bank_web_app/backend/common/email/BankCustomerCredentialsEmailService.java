package com.bank_web_app.backend.common.email;

import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BankCustomerCredentialsEmailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BankCustomerCredentialsEmailService.class);
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

	private final EmailService emailService;

	public BankCustomerCredentialsEmailService(EmailService emailService) {
		this.emailService = emailService;
	}

	public void sendCredentialsEmail(String recipientEmail, String firstName, String username, String password) {
		String normalizedRecipient = recipientEmail == null ? "" : recipientEmail.trim();
		String normalizedFirstName = firstName == null ? "" : firstName.trim();
		String normalizedUsername = username == null ? "" : username.trim();
		String normalizedPassword = password == null ? "" : password;

		LOGGER.info(
			"Attempting to send bank customer credentials email to {} (username={})",
			normalizedRecipient.isBlank() ? "<blank>" : normalizedRecipient,
			normalizedUsername
		);

		if (normalizedRecipient.isBlank()) {
			LOGGER.warn("Credentials email not sent because the recipient email is blank for username={}", normalizedUsername);
			throw new IllegalArgumentException("Customer email is required to send credentials.");
		}

		if (!looksLikeEmail(normalizedRecipient)) {
			LOGGER.warn(
				"Credentials email for username={} has an invalid format: {}",
				normalizedUsername,
				normalizedRecipient
			);
		}

		String subject = "Your bank customer account credentials";
		String body = String.join("\n",
			"Hello " + normalizedFirstName + ",",
			"",
			"Your bank customer account has been created.",
			"Username: " + normalizedUsername,
			"Password: " + normalizedPassword,
			"",
			"Please sign in and change your password after the first login.",
			"",
			"Regards,",
			"Primecore Bank"
		);

		try {
			emailService.sendPlainText(normalizedRecipient, subject, body);
			LOGGER.info("Credentials email sent successfully to {} for username={}", normalizedRecipient, normalizedUsername);
		} catch (RuntimeException ex) {
			LOGGER.error("Credentials email failed for {} (username={})", normalizedRecipient, normalizedUsername, ex);
			throw ex;
		}
	}

	private boolean looksLikeEmail(String value) {
		return value != null && EMAIL_PATTERN.matcher(value).matches();
	}
}