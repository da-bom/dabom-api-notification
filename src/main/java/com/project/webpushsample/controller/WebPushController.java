package com.project.webpushsample.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.webpushsample.controller.dto.PushSubscriptionRequest;
import com.project.webpushsample.domain.Subscription;
import com.project.webpushsample.service.WebPushService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/push")
public class WebPushController {

    private final WebPushService webPushService;

    public WebPushController(WebPushService webPushService) {
        this.webPushService = webPushService;
    }

    // 사용자별로 브라우저에 따른 subscription url이 저장된다
    @PostMapping("/subscribe")
    public void subscribe(@RequestBody PushSubscriptionRequest subscriptionRequest) {
        log.info("Subscribing to webpush subscription {}", subscriptionRequest);
        webPushService.subscribe(
                new Subscription(
                        subscriptionRequest.getEndpoint(),
                        subscriptionRequest.getKeys().get("p256dh"),
                        subscriptionRequest.getKeys().get("auth"),
                        subscriptionRequest.getCustomerId()));
    }

    // 구독자에게 알림을 전송한다
    @PostMapping("/send")
    public void sendPushMessage(@RequestBody String message) throws Exception {
        log.info("Sending push message: {}", message);
        // webPushService.send(message);
    }
}
