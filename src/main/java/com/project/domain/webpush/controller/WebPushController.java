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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/push")
@RequiredArgsConstructor
public class WebPushController {

    private final WebPushService webPushService;
    private final NotificationService notificationService;

    @Operation(summary = "VAPID 공개 키 조회")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"))
    @GetMapping("/vapid-public-key")
    public ApiResponse<VapidPublicKeyResponse> getVapidPublicKey() {
        return ApiResponse.success(new VapidPublicKeyResponse(webPushService.getVapidPublicKey()));
    }

    @Operation(summary = "푸시 알림 구독", description = "신규 구독 또는 기존 구독 갱신(업서트)을 처리한다.")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "구독 성공"))
    @PostMapping("/subscribe")
    public ApiResponse<Void> subscribe(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @Valid @RequestBody PushSubscriptionRequest subscriptionRequest) {
        webPushService.subscribe(subscriptionRequest, customerId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "푸시 알림 구독 해제")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "구독 해제 성공"))
    @DeleteMapping("/subscribe")
    public ApiResponse<Void> unsubscribe(@Parameter(hidden = true) @CustomerId Long customerId) {
        webPushService.unsubscribe(customerId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "관리자 푸시 알림 발송")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "발송 성공"))
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
