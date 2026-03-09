package com.project.webpushsample.controller.dto;

import java.util.Map;

public record PushSubscriptionRequest(String endpoint, Map<String, String> keys) {}
