package com.bank_web_app.backend.support.controller;

import com.bank_web_app.backend.support.dto.SupportRequest;
import com.bank_web_app.backend.support.service.SupportRequestService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/support")
public class SupportController {
	private final SupportRequestService supportRequestService;
	public SupportController(SupportRequestService supportRequestService) { this.supportRequestService = supportRequestService; }
	@PostMapping("/requests")
	public ResponseEntity<Map<String, String>> submit(@Valid @RequestBody SupportRequest request) {
		supportRequestService.submit(request);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("message", "Your support request has been sent."));
	}
}
