package com.project.domain.webpush.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminPushRequest(
        @NotNull Long customerId, @NotBlank String title, @NotBlank String message) {}
