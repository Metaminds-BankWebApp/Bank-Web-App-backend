package com.bank_web_app.backend.common.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class BrevoSmtpEmailService implements EmailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BrevoSmtpEmailService.class);
	private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final String brevoApiKey;
	private final String fromAddress;
	private final String fromName;

	public BrevoSmtpEmailService(
		@Value("${spring.mail.brevo.api.key:${BREVO_API_KEY:}}") String brevoApiKey,
		@Value("${app.mail.from:${spring.mail.app.email.from:${APP_MAIL_FROM:}}}") String fromAddress,
		@Value("${app.mail.name:${spring.mail.app.email.name:${APP_MAIL_NAME:Primecore}}}") String fromName
	) {
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(15))
			.build();
		this.objectMapper = new ObjectMapper();
		this.brevoApiKey = brevoApiKey == null ? "" : brevoApiKey.trim();
		this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
		this.fromName = fromName == null || fromName.isBlank() ? "Primecore" : fromName.trim();
		LOGGER.info(
			"Brevo mail config initialized with apiKeyLength={}, fromAddress={}, fromName={}",
			this.brevoApiKey.length(),
			this.fromAddress,
			this.fromName
		);
	}

	@Override
	public void sendPlainText(String toEmail, String subject, String body) {
		if (toEmail == null || toEmail.isBlank()) {
			throw new IllegalArgumentException("Email recipient is required.");
		}
		if (brevoApiKey.isBlank()) {
			throw new EmailDeliveryException(
				"Unable to deliver credentials email: BREVO_API_KEY is required.",
				new IllegalStateException("Brevo API key is blank.")
			);
		}
		if (fromAddress == null || fromAddress.isBlank()) {
			throw new EmailDeliveryException(
				"Unable to deliver credentials email: APP_MAIL_FROM is required.",
				new IllegalStateException("APP_MAIL_FROM is blank.")
			);
		}
		if (subject == null || subject.isBlank()) {
			throw new IllegalArgumentException("Email subject is required.");
		}
		if (body == null || body.isBlank()) {
			throw new IllegalArgumentException("Email body is required.");
		}

		Map<String, Object> payload = new HashMap<>();
		payload.put("sender", Map.of("name", fromName, "email", fromAddress));
		payload.put("to", List.of(Map.of("email", toEmail.trim())));
		payload.put("subject", subject.trim());
		payload.put("textContent", body);

		String requestBody;
		try {
			requestBody = objectMapper.writeValueAsString(payload);
		} catch (Exception ex) {
			throw new EmailDeliveryException("Unable to deliver OTP email right now. Failed to serialize Brevo payload.", ex);
		}

		try {
			String masked;
			if (brevoApiKey.isBlank()) {
				masked = "<empty>";
			} else if (brevoApiKey.length() <= 12) {
				masked = "<short>(" + brevoApiKey.length() + ")";
			} else {
				masked = brevoApiKey.substring(0, 6) + "..." + brevoApiKey.substring(brevoApiKey.length() - 6);
			}
			LOGGER.info("Using Brevo API key {} (length {}) for sender {}", masked, brevoApiKey.length(), fromAddress);
			Map<String, Object> safePayload = new HashMap<>(payload);
			safePayload.put("textContent", "<omitted>");
			LOGGER.info("Sending email request to Brevo for {} with payload: {}", toEmail.trim(), safePayload);
			LOGGER.info("Brevo request sender: {} <{}>", fromName, fromAddress);
			LOGGER.info("Brevo raw JSON payload for {}: {}", toEmail.trim(), requestBody);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(BREVO_API_URL))
				.timeout(Duration.ofSeconds(30))
				.header("api-key", brevoApiKey)
				.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			int statusCode = response.statusCode();
			String responseBody = response.body();
			if (statusCode < 200 || statusCode >= 300) {
				LOGGER.error(
					"Brevo API rejected email for {} with status {} and body {}",
					toEmail,
					statusCode,
					responseBody == null || responseBody.isBlank() ? "<no body>" : responseBody.trim()
				);
				throw new EmailDeliveryException(
					buildUserFacingMessage(statusCode, responseBody, requestBody),
					new IllegalStateException("Brevo API non-success response.")
				);
			}

			if (responseBody == null || responseBody.isBlank()) {
				LOGGER.info("Brevo accepted credentials email for {} with status {}.", toEmail.trim(), statusCode);
			} else {
				LOGGER.info("Brevo accepted credentials email for {} with status {} and response {}.", toEmail.trim(), statusCode, responseBody.trim());
			}
		} catch (HttpTimeoutException ex) {
			LOGGER.error("Brevo API timeout for {}", toEmail, ex);
			throw new EmailDeliveryException("Unable to deliver credentials email: cannot connect to Brevo API server.", ex);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			LOGGER.error("Brevo API interrupted for {}", toEmail, ex);
			throw new EmailDeliveryException("Unable to deliver credentials email right now. Check Brevo API settings and try again.", ex);
		} catch (IOException ex) {
			LOGGER.error("Brevo API I/O error for {}", toEmail, ex);
			throw new EmailDeliveryException("Unable to deliver credentials email: cannot connect to Brevo API server.", ex);
		}
	}

	private String buildUserFacingMessage(int statusCode, String responseBody, String requestBody) {
		String raw = ((responseBody == null ? "" : responseBody) + " " + (requestBody == null ? "" : requestBody)).toLowerCase();
		if (
			statusCode == 401 ||
			statusCode == 403 ||
			raw.contains("api key") ||
			raw.contains("api-key") ||
			raw.contains("unauthorized")
		) {
			return "Unable to deliver credentials email: invalid Brevo API key.";
		}
		if (
			raw.contains("sender") &&
			(raw.contains("not valid") || raw.contains("not verified") || raw.contains("rejected") || raw.contains("invalid"))
		) {
			return "Unable to deliver credentials email: APP_MAIL_FROM must be a Brevo-verified sender email.";
		}
		if (
			raw.contains("recipient") ||
			raw.contains("invalid_parameter") ||
			raw.contains("email")
		) {
			return "Unable to deliver credentials email: recipient email is invalid or unreachable.";
		}
		if (
			statusCode == 429 ||
			raw.contains("rate limit")
		) {
			return "Unable to deliver credentials email: Brevo rate limit reached. Please try again.";
		}
		if (statusCode >= 500) {
			return "Unable to deliver credentials email: Brevo service is currently unavailable.";
		}
		return "Unable to deliver credentials email right now. Check Brevo API settings and try again.";
	}
}
