package com.bank_web_app.backend.common.email;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class BrevoSmtpEmailService implements EmailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BrevoSmtpEmailService.class);
	private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

	private final RestTemplate restTemplate;
	private final String brevoApiKey;
	private final String fromAddress;
	private final String fromName;

	public BrevoSmtpEmailService(
		@Value("${mail.brevo.api.key:}") String brevoApiKey,
		@Value("${mail.app.email.from:${APP_MAIL_FROM:no-reply@primecore.local}}") String fromAddress,
		@Value("${mail.app.email.name:Primecore}") String fromName
	) {
		this.restTemplate = new RestTemplate();
		this.brevoApiKey = brevoApiKey == null ? "" : brevoApiKey.trim();
		this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
		this.fromName = fromName == null || fromName.isBlank() ? "Primecore" : fromName.trim();
	}

	@Override
	public void sendPlainText(String toEmail, String subject, String body) {
		if (toEmail == null || toEmail.isBlank()) {
			throw new IllegalArgumentException("Email recipient is required.");
		}
		if (brevoApiKey.isBlank()) {
			throw new EmailDeliveryException(
				"Unable to deliver OTP email: BREVO_API_KEY is required.",
				new IllegalStateException("Brevo API key is blank.")
			);
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

		HttpHeaders headers = new HttpHeaders();
		headers.set("api-key", brevoApiKey);
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> payload = new HashMap<>();
		payload.put("sender", Map.of("name", fromName, "email", fromAddress));
		payload.put("to", List.of(Map.of("email", toEmail.trim())));
		payload.put("subject", subject.trim());
		payload.put("textContent", body);

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
		try {
			ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, request, String.class);
			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new EmailDeliveryException(
					"Unable to deliver OTP email right now. Brevo API returned " + response.getStatusCode().value() + ".",
					new IllegalStateException("Brevo API non-success response.")
				);
			}
		} catch (HttpStatusCodeException ex) {
			LOGGER.error("Brevo API rejected email for {}", toEmail, ex);
			throw new EmailDeliveryException(buildUserFacingMessage(ex), ex);
		} catch (ResourceAccessException ex) {
			LOGGER.error("Brevo API connection error for {}", toEmail, ex);
			throw new EmailDeliveryException("Unable to deliver OTP email: cannot connect to Brevo API server.", ex);
		} catch (RestClientException ex) {
			LOGGER.error("Failed to send email to {}", toEmail, ex);
			throw new EmailDeliveryException("Unable to deliver OTP email right now. Check Brevo API settings and try again.", ex);
		}
	}

	private String buildUserFacingMessage(HttpStatusCodeException ex) {
		int statusCode = ex.getStatusCode().value();
		String raw = extractRawResponse(ex).toLowerCase();
		if (
			statusCode == 401 ||
			statusCode == 403 ||
			raw.contains("api key") ||
			raw.contains("api-key") ||
			raw.contains("unauthorized")
		) {
			return "Unable to deliver OTP email: invalid Brevo API key.";
		}
		if (
			raw.contains("sender") &&
			(raw.contains("not valid") || raw.contains("not verified") || raw.contains("rejected") || raw.contains("invalid"))
		) {
			return "Unable to deliver OTP email: APP_MAIL_FROM must be a Brevo-verified sender email.";
		}
		if (
			raw.contains("recipient") ||
			raw.contains("invalid_parameter") ||
			raw.contains("email")
		) {
			return "Unable to deliver OTP email: recipient email is invalid or unreachable.";
		}
		if (
			statusCode == 429 ||
			raw.contains("rate limit")
		) {
			return "Unable to deliver OTP email: Brevo rate limit reached. Please try again.";
		}
		if (statusCode >= 500) {
			return "Unable to deliver OTP email: Brevo service is currently unavailable.";
		}
		return "Unable to deliver OTP email right now. Check Brevo API settings and try again.";
	}

	private String extractRawResponse(HttpStatusCodeException ex) {
		String responseBody = ex.getResponseBodyAsString();
		if (responseBody != null && !responseBody.isBlank()) {
			return responseBody;
		}
		String message = ex.getMessage();
		return message == null ? "" : message;
	}
}
