package com.project.domain.webpush.dto.request;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PushSubscriptionRequest(
        @NotBlank String endpoint, @NotNull Map<String, String> keys) {

    private static final String P256DH_KEY = "p256dh";
    private static final String AUTH_KEY = "auth";

    public String p256dh() {
        return keys.get(P256DH_KEY);
    }

    public String authKey() {
        return keys.get(AUTH_KEY);
    }
}
