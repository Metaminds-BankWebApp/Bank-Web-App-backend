package com.bank_web_app.backend.bankofficer.controller;

import com.bank_web_app.backend.bankofficer.dto.request.WorkQueueCaseStatusRequest;
import com.bank_web_app.backend.bankofficer.dto.response.WorkQueueCaseResponse;
import com.bank_web_app.backend.bankofficer.service.WorkQueueCaseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/bank-officers/work-queue/cases")
public class WorkQueueCaseController {
	private final WorkQueueCaseService service;
	public WorkQueueCaseController(WorkQueueCaseService service) { this.service = service; }
	@GetMapping public ResponseEntity<List<WorkQueueCaseResponse>> getAll() { return ResponseEntity.ok(service.getAll()); }
	@PutMapping public ResponseEntity<WorkQueueCaseResponse> update(@Valid @RequestBody WorkQueueCaseStatusRequest request) { return ResponseEntity.ok(service.update(request)); }
}
