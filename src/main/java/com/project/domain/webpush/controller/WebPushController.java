package com.project.domain.webpush.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.common.api.response.ApiResponse;
import com.project.common.auth.aop.AdminOnly;
import com.project.common.auth.aop.CustomerId;
import com.project.domain.notification.service.NotificationService;
import com.project.domain.webpush.dto.request.AdminPushRequest;
import com.project.domain.webpush.dto.request.PushSubscriptionRequest;
import com.project.domain.webpush.dto.response.VapidPublicKeyResponse;
import com.project.domain.webpush.service.WebPushService;

import io.swagger.v3.oas.annotations.Parameter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/push")
@RequiredArgsConstructor
public class WebPushController {

    private final WebPushService webPushService;
    private final NotificationService notificationService;

    @GetMapping("/vapid-public-key")
    public ApiResponse<VapidPublicKeyResponse> getVapidPublicKey() {
        return ApiResponse.success(new VapidPublicKeyResponse(webPushService.getVapidPublicKey()));
    }

    @PostMapping("/subscribe")
    public ApiResponse<Void> subscribe(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @Valid @RequestBody PushSubscriptionRequest subscriptionRequest) {
        webPushService.subscribe(subscriptionRequest, customerId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/subscribe")
    public ApiResponse<Void> unsubscribe(@Parameter(hidden = true) @CustomerId Long customerId) {
        webPushService.unsubscribe(customerId);
        return ApiResponse.success(null);
    }

    @AdminOnly
    @PostMapping("/send")
    public ApiResponse<Void> sendPushMessage(@Valid @RequestBody AdminPushRequest request) {
        log.info("Push message send requested for customerId={}", request.customerId());
        notificationService.saveAdminPushNotification(
                request.customerId(), request.title(), request.message());
        webPushService.sendToUser(request.customerId(), request.title(), request.message());
        return ApiResponse.success(null);
    }
}
