package com.bank_web_app.backend.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

	@Test
	void multipartLimitsMatchSupportedProfileImageSize() throws IOException {
		Properties properties = new Properties();
		try (var inputStream = new ClassPathResource("application.properties").getInputStream()) {
			properties.load(inputStream);
		}

		assertThat(properties.getProperty("spring.servlet.multipart.max-file-size")).isEqualTo("5MB");
		assertThat(properties.getProperty("spring.servlet.multipart.max-request-size")).isEqualTo("6MB");
	}

	@Test
	void oversizedMultipartUploadReturnsPayloadTooLargeResponse() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		HttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/profile/image");

		ResponseEntity<ApiErrorResponse> response = handler.handleMaxUploadSizeExceeded(
			new MaxUploadSizeExceededException(5L * 1024 * 1024),
			request
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().message()).isEqualTo("Profile image must not exceed 5 MB.");
		assertThat(response.getBody().path()).isEqualTo("/api/users/profile/image");
	}
}
