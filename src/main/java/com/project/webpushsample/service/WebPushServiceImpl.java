package com.project.webpushsample.service;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.SubscriptionErrorCode;
import com.project.webpushsample.controller.dto.PushSubscriptionRequest;
import com.project.webpushsample.domain.Subscription;
import com.project.webpushsample.repository.SubscriptionRepository;

import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushServiceImpl implements WebPushService {

    @Value("${vapid.key.public}")
    private String vapidPublicKey;

    private final SubscriptionRepository subscriptionRepository;
    private final PushService pushService;

    @Override
    public void subscribe(PushSubscriptionRequest request, Long customerId) {
        Subscription subscription =
                Subscription.builder()
                        .endpoint(request.endpoint())
                        .p256dh(request.keys().get("p256dh"))
                        .auth(request.keys().get("auth"))
                        .customerId(customerId)
                        .build();
        subscriptionRepository.save(subscription);
    }

    @Override
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

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
        } catch (Exception e) {
            log.error("Failed to send push notification", e);
            throw new ApplicationException(SubscriptionErrorCode.PUSH_SEND_FAILED);
        }
    }
}
