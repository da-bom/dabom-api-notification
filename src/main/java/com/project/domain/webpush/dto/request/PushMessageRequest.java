package com.project.domain.webpush.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PushMessageRequest(@NotBlank String message) {}
