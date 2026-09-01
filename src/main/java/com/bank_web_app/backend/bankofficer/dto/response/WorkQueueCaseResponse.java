package com.bank_web_app.backend.bankofficer.dto.response;

import java.time.LocalDateTime;

public record WorkQueueCaseResponse(Long userId, String caseType, String status, Long updatedByOfficerId, LocalDateTime updatedAt) {}
