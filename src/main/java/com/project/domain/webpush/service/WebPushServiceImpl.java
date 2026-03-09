package com.project.domain.webpush.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.webpush.controller.dto.PushSubscriptionRequest;
import com.project.domain.webpush.entity.Subscription;
import com.project.domain.webpush.repository.SubscriptionRepository;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.SubscriptionErrorCode;

import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushServiceImpl implements WebPushService {

    private final SubscriptionRepository subscriptionRepository;
    private final PushService pushService;

    @Transactional
    @Override
    public void subscribe(PushSubscriptionRequest request, Long customerId) {
        subscriptionRepository
                .findByCustomerId(customerId)
                .ifPresentOrElse(
                        existing ->
                                existing.updateSubscription(
                                        request.endpoint(), request.p256dh(), request.authKey()),
                        () -> {
                            Subscription subscription =
                                    Subscription.builder()
                                            .endpoint(request.endpoint())
                                            .p256dh(request.p256dh())
                                            .auth(request.authKey())
                                            .customerId(customerId)
                                            .build();
                            subscriptionRepository.save(subscription);
                        });
    }

    @Transactional(readOnly = true)
    @Override
    public void sendToUser(Long customerId, String message) {
        Subscription subscription =
                subscriptionRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        sendPushNotification(subscription, message);
    }

    @Transactional(readOnly = true)
    @Override
    public void sendToFamily(Long familyId, String message) {
        Subscription subscription =
                subscriptionRepository
                        .findByCustomerId(familyId)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        sendPushNotification(subscription, message);
    }

    private void sendPushNotification(Subscription subscription, String message) {
        try {
            nl.martijndwars.webpush.Subscription sub =
                    new nl.martijndwars.webpush.Subscription(
                            subscription.getEndpoint(),
                            new nl.martijndwars.webpush.Subscription.Keys(
                                    subscription.getP256dh(), subscription.getAuth()));
            Notification notification = new Notification(sub, message);
            HttpResponse response = pushService.send(notification, Encoding.AES128GCM);

            log.info(
                    "Push message sent with status code: {}",
                    response.getStatusLine().getStatusCode());
            String body =
                    response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
            log.info("Push response status={}, body={}", response.getStatusLine(), body);
        } catch (GeneralSecurityException
                | IOException
                | JoseException
                | ExecutionException
                | InterruptedException e) {
            log.error("Failed to send push notification", e);
            throw new ApplicationException(SubscriptionErrorCode.PUSH_SEND_FAILED);
        }
    }
}
