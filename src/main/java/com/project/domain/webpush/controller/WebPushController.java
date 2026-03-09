package com.project.domain.webpush.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.domain.webpush.controller.dto.PushSubscriptionRequest;
import com.project.domain.webpush.controller.dto.VapidPublicKey;
import com.project.domain.webpush.service.WebPushService;
import com.project.global.api.response.ApiResponse;
import com.project.global.auth.aop.CustomerId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/push")
@RequiredArgsConstructor
public class WebPushController {

    private final WebPushService webPushService;

    @GetMapping("/vapid-public-key")
    public ApiResponse<VapidPublicKey> getVapidPublicKey() {
        return ApiResponse.success(new VapidPublicKey(webPushService.getVapidPublicKey()));
    }

    @PostMapping("/subscribe")
    public ApiResponse<Void> subscribe(
            @CustomerId Long customerId, @RequestBody PushSubscriptionRequest subscriptionRequest) {
        webPushService.subscribe(subscriptionRequest, customerId);
        return ApiResponse.created(null);
    }

    @PostMapping("/send")
    public ApiResponse<Void> sendPushMessage(@RequestBody String message) {
        log.info("Sending push message: {}", message.replaceAll("[\\r\\n]", "_"));
        return ApiResponse.success(null);
    }
}
