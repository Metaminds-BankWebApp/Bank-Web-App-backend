package com.bank_web_app.backend.common.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class BrevoSmtpEmailService implements EmailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BrevoSmtpEmailService.class);

	private final JavaMailSender javaMailSender;
	private final String fromAddress;

	public BrevoSmtpEmailService(
		JavaMailSender javaMailSender,
		@Value("${app.mail.from:no-reply@primecore.local}") String fromAddress
	) {
		this.javaMailSender = javaMailSender;
		this.fromAddress = fromAddress;
	}

	@Override
	public void sendPlainText(String toEmail, String subject, String body) {
		if (toEmail == null || toEmail.isBlank()) {
			throw new IllegalArgumentException("Email recipient is required.");
		}
		if (fromAddress == null || fromAddress.isBlank()) {
			throw new EmailDeliveryException(
				"Unable to deliver OTP email: APP_MAIL_FROM is required.",
				new IllegalStateException("APP_MAIL_FROM is blank.")
			);
		}
		if (subject == null || subject.isBlank()) {
			throw new IllegalArgumentException("Email subject is required.");
		}
		if (body == null || body.isBlank()) {
			throw new IllegalArgumentException("Email body is required.");
		}

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromAddress.trim());
		message.setTo(toEmail.trim());
		message.setSubject(subject.trim());
		message.setText(body);

		try {
			javaMailSender.send(message);
		} catch (MailException ex) {
			LOGGER.error("Failed to send email to {}", toEmail, ex);
			throw new EmailDeliveryException(buildUserFacingMessage(ex), ex);
		}
	}

	private String buildUserFacingMessage(MailException ex) {
		String raw = extractRootMessage(ex).toLowerCase();
		if (
			raw.contains("authentication failed") ||
			raw.contains("username and password not accepted") ||
			raw.contains("535")
		) {
			return "Unable to deliver OTP email: invalid Brevo SMTP login or key.";
		}
		if (
			raw.contains("sender address rejected") ||
			raw.contains("from address") ||
			raw.contains("not verified")
		) {
			return "Unable to deliver OTP email: APP_MAIL_FROM must be a Brevo-verified sender email.";
		}
		if (
			raw.contains("recipient address rejected") ||
			raw.contains("invalid address") ||
			raw.contains("mailbox unavailable") ||
			raw.contains("user unknown") ||
			raw.contains("unknown user")
		) {
			return "Unable to deliver OTP email: recipient email is invalid or unreachable.";
		}
		if (
			raw.contains("domain not found") ||
			raw.contains("name or service not known") ||
			raw.contains("no such domain")
		) {
			return "Unable to deliver OTP email: recipient domain is not reachable.";
		}
		if (
			raw.contains("could not connect to smtp host") ||
			raw.contains("connection refused") ||
			raw.contains("timed out")
		) {
			return "Unable to deliver OTP email: cannot connect to Brevo SMTP server.";
		}
		return "Unable to deliver OTP email right now. Check Brevo SMTP settings and try again.";
	}

	private String extractRootMessage(Throwable ex) {
		Throwable current = ex;
		while (current.getCause() != null) {
			current = current.getCause();
		}
		String message = current.getMessage();
		return message == null ? "" : message;
	}
}
