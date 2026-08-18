package com.bank_web_app.backend.support.controller;

import com.bank_web_app.backend.support.dto.request.SupportConversationStatusUpdateRequest;
import com.bank_web_app.backend.support.dto.response.SupportConversationDetailResponse;
import com.bank_web_app.backend.support.dto.response.SupportConversationSummaryResponse;
import com.bank_web_app.backend.support.service.SupportConversationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/support/conversations")
public class AdminSupportController {

	private final SupportConversationService supportConversationService;

	public AdminSupportController(SupportConversationService supportConversationService) {
		this.supportConversationService = supportConversationService;
	}

	@GetMapping
	public ResponseEntity<List<SupportConversationSummaryResponse>> getAllConversations() {
		return ResponseEntity.ok(supportConversationService.getAllConversations());
	}

	@PatchMapping("/{conversationId}/status")
	public ResponseEntity<SupportConversationDetailResponse> updateStatus(
		@PathVariable Long conversationId,
		@Valid @RequestBody SupportConversationStatusUpdateRequest request
	) {
		return ResponseEntity.ok(supportConversationService.updateStatus(conversationId, request));
	}

	@DeleteMapping("/{conversationId}")
	public ResponseEntity<Void> permanentlyDelete(@PathVariable Long conversationId) {
		supportConversationService.permanentlyDeleteConversation(conversationId);
		return ResponseEntity.noContent().build();
	}
}
