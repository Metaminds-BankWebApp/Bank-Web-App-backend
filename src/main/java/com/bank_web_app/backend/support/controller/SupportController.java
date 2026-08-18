package com.bank_web_app.backend.support.controller;

import com.bank_web_app.backend.support.dto.request.SupportConversationCreateRequest;
import com.bank_web_app.backend.support.dto.request.SupportMessageCreateRequest;
import com.bank_web_app.backend.support.dto.response.SupportConversationDetailResponse;
import com.bank_web_app.backend.support.dto.response.SupportConversationSummaryResponse;
import com.bank_web_app.backend.support.service.SupportConversationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
public class SupportController {

	private final SupportConversationService supportConversationService;

	public SupportController(SupportConversationService supportConversationService) {
		this.supportConversationService = supportConversationService;
	}

	@PostMapping({"/requests", "/conversations"})
	public ResponseEntity<SupportConversationDetailResponse> createConversation(
		@Valid @RequestBody SupportConversationCreateRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(supportConversationService.createConversation(request));
	}

	@GetMapping("/conversations")
	public ResponseEntity<List<SupportConversationSummaryResponse>> getMyConversations() {
		return ResponseEntity.ok(supportConversationService.getMyConversations());
	}

	@GetMapping("/conversations/{conversationId}")
	public ResponseEntity<SupportConversationDetailResponse> getConversation(@PathVariable Long conversationId) {
		return ResponseEntity.ok(supportConversationService.getConversation(conversationId));
	}

	@PostMapping("/conversations/{conversationId}/messages")
	public ResponseEntity<SupportConversationDetailResponse> sendMessage(
		@PathVariable Long conversationId,
		@Valid @RequestBody SupportMessageCreateRequest request
	) {
		return ResponseEntity.ok(supportConversationService.sendMessage(conversationId, request));
	}

	@PatchMapping("/conversations/{conversationId}/read")
	public ResponseEntity<Void> markRead(@PathVariable Long conversationId) {
		supportConversationService.markConversationRead(conversationId);
		return ResponseEntity.noContent().build();
	}
}
