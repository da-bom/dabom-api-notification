package com.project.domain.webpush.controller.dto;

import java.util.Map;

public record PushSubscriptionRequest(String endpoint, Map<String, String> keys) {}
