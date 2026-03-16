package com.project.domain.webpush.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminPushRequest(
        @NotNull Long customerId, @NotBlank String title, @NotBlank String message) {}
