package com.project.webpushsample.service;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
public class WebPushService {
    @Value("${vapid.key.public}")
    public String vapidPublicKey;

    private final SubscriptionRepository subscriptionRepository;
    private final PushService pushService;

    public void subscribe(Subscription subscription) {
        subscriptionRepository.save(subscription);
    }

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    public void sendToUser(Long customerId, String message) throws Exception {
        // todo: customerId 기반으로 subscription을 찾습니다
        Subscription subscription = subscriptionRepository.findById(customerId).orElse(null);
        nl.martijndwars.webpush.Subscription sub =
                new nl.martijndwars.webpush.Subscription(
                        subscription.getEndpoint(),
                        new nl.martijndwars.webpush.Subscription.Keys(
                                subscription.getP256dh(), subscription.getAuth()));
        Notification notification = new Notification(sub, message);
        HttpResponse response = pushService.send(notification, Encoding.AES128GCM);

        log.info(
                "Push message sent with status code: {}", response.getStatusLine().getStatusCode());
        log.info("Response: {}", response);
        String body =
                response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
        log.info("Push response status={}, body={}", response.getStatusLine(), body);
    }

    public void sendToFamily(Long customerId, String message) throws Exception {
        // todo: familyId 기반으로 subscription을 찾습니다
        Subscription subscription = subscriptionRepository.findById(customerId).orElse(null);
        nl.martijndwars.webpush.Subscription sub =
                new nl.martijndwars.webpush.Subscription(
                        subscription.getEndpoint(),
                        new nl.martijndwars.webpush.Subscription.Keys(
                                subscription.getP256dh(), subscription.getAuth()));
        Notification notification = new Notification(sub, message);
        HttpResponse response = pushService.send(notification, Encoding.AES128GCM);

        log.info(
                "Push message sent with status code: {}", response.getStatusLine().getStatusCode());
        log.info("Response: {}", response);
        String body =
                response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
        log.info("Push response status={}, body={}", response.getStatusLine(), body);
    }
}
