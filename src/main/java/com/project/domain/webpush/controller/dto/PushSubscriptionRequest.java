package com.project.domain.webpush.controller.dto;

import java.util.Map;

public record PushSubscriptionRequest(String endpoint, Map<String, String> keys) {

    public static final String P256DH_KEY = "p256dh";
    public static final String AUTH_KEY = "auth";

    public String p256dh() {
        return keys.get(P256DH_KEY);
    }

    public String authKey() {
        return keys.get(AUTH_KEY);
    }
}
