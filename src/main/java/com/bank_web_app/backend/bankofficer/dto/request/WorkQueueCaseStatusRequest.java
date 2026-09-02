package com.bank_web_app.backend.bankofficer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkQueueCaseStatusRequest(@NotNull Long userId, @NotBlank String caseType, @NotBlank String status) {}
