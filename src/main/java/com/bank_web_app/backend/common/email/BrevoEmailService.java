package com.bank_web_app.backend.common.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/** Sends transactional email through Brevo's REST API. */
@Service
public class BrevoEmailService implements EmailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BrevoEmailService.class);
	private static final URI BREVO_EMAILS_URI = URI.create("https://api.brevo.com/v3/smtp/email");

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final String brevoApiKey;
	private final String fromAddress;
	private final String fromName;

	public BrevoEmailService(
		@Value("${BREVO_API_KEY:}") String brevoApiKey,
		@Value("${APP_MAIL_FROM:}") String fromAddress,
		@Value("${APP_MAIL_NAME:Primecore}") String fromName
	) {
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
		this.objectMapper = new ObjectMapper();
		this.brevoApiKey = safeTrim(brevoApiKey);
		this.fromAddress = safeTrim(fromAddress);
		this.fromName = safeTrim(fromName).isBlank() ? "Primecore" : safeTrim(fromName);
		LOGGER.info("Brevo mail configuration initialized: apiKeyConfigured={}, senderConfigured={}", !this.brevoApiKey.isBlank(), !this.fromAddress.isBlank());
	}

	@Override
	public void sendPlainText(String toEmail, String subject, String body) {
		String recipient = safeTrim(toEmail);
		String normalizedSubject = safeTrim(subject);
		if (recipient.isBlank()) throw new IllegalArgumentException("Email recipient is required.");
		if (normalizedSubject.isBlank()) throw new IllegalArgumentException("Email subject is required.");
		if (body == null || body.isBlank()) throw new IllegalArgumentException("Email body is required.");
		if (brevoApiKey.isBlank()) throw new EmailDeliveryException("Unable to deliver email: BREVO_API_KEY is not configured.", new IllegalStateException("Brevo API key is blank."));
		if (fromAddress.isBlank()) throw new EmailDeliveryException("Unable to deliver email: APP_MAIL_FROM is required.", new IllegalStateException("Sender address is blank."));

		Map<String, Object> payload = Map.of(
			"sender", Map.of("name", fromName, "email", fromAddress),
			"to", List.of(Map.of("email", recipient)),
			"subject", normalizedSubject,
			"textContent", body
		);
		String requestBody;
		try {
			requestBody = objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException ex) {
			throw new EmailDeliveryException("Unable to prepare email for delivery.", ex);
		}

		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(BREVO_EMAILS_URI)
				.timeout(Duration.ofSeconds(30))
				.header("api-key", brevoApiKey)
				.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				LOGGER.warn("Brevo rejected an email request with HTTP status {}.", response.statusCode());
				throw new EmailDeliveryException(userFacingMessage(response.statusCode()), new IllegalStateException("Brevo returned HTTP " + response.statusCode()));
			}
			LOGGER.info("Brevo accepted transactional email for {}.", recipient);
		} catch (HttpTimeoutException ex) {
			throw new EmailDeliveryException("Unable to deliver email: Brevo timed out.", ex);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new EmailDeliveryException("Unable to deliver email right now. Please try again.", ex);
		} catch (IOException ex) {
			throw new EmailDeliveryException("Unable to deliver email: cannot connect to Brevo.", ex);
		}
	}

	private String userFacingMessage(int statusCode) {
		return switch (statusCode) {
			case 401, 403 -> "Unable to deliver email: check the Brevo API key, authorised IPs, and sender verification.";
			case 429 -> "Unable to deliver email: Brevo rate limit reached. Please try again.";
			default -> statusCode >= 500 ? "Unable to deliver email: Brevo is temporarily unavailable." : "Unable to deliver email right now. Check the Brevo configuration.";
		};
	}

	private String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}
}
