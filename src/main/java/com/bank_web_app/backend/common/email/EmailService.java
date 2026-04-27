package com.bank_web_app.backend.common.email;

public interface EmailService {

	void sendPlainText(String toEmail, String subject, String body);
}
