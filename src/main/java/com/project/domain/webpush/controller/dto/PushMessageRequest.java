package com.project.domain.webpush.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record PushMessageRequest(@NotBlank String message) {}
