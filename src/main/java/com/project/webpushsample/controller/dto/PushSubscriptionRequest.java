package com.project.webpushsample.controller.dto;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PushSubscriptionRequest {
    private String endpoint;
    private Map<String, String> keys;
    private Long customerId;

    @Override
    public String toString() {
        return "PushSubscriptionRequest{" + "endpoint='" + endpoint + '\'' + ", keys=" + keys + '}';
    }
}
