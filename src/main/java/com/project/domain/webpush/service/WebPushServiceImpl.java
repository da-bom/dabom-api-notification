package com.project.domain.webpush.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.domain.family.entity.FamilyMember;
import com.project.domain.family.repository.FamilyMemberRepository;
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
    private final FamilyMemberRepository familyMemberRepository;
    private final PushService pushService;
    private final CloseableHttpClient pushHttpClient;

    @Value("${vapid.key.public}")
    private String vapidPublicKey;

    @Transactional
    @Override
    public void subscribe(PushSubscriptionRequest request, Long customerId) {
        validateEndpointUrl(request.endpoint());
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
        List<Long> customerIds =
                familyMemberRepository.findAllByFamilyId(familyId).stream()
                        .map(FamilyMember::getCustomerId)
                        .toList();

        List<Subscription> subscriptions =
                subscriptionRepository.findAllByCustomerIdIn(customerIds);

        subscriptions.forEach(subscription -> sendPushNotification(subscription, message));
    }

    @Override
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    private void validateEndpointUrl(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new ApplicationException(SubscriptionErrorCode.INVALID_ENDPOINT_URL);
            }
            InetAddress address = InetAddress.getByName(uri.getHost());
            if (address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()) {
                throw new ApplicationException(SubscriptionErrorCode.INVALID_ENDPOINT_URL);
            }
        } catch (IllegalArgumentException | UnknownHostException e) {
            throw new ApplicationException(SubscriptionErrorCode.INVALID_ENDPOINT_URL);
        }
    }

    private void sendPushNotification(Subscription subscription, String message) {
        try {
            nl.martijndwars.webpush.Subscription sub =
                    new nl.martijndwars.webpush.Subscription(
                            subscription.getEndpoint(),
                            new nl.martijndwars.webpush.Subscription.Keys(
                                    subscription.getP256dh(), subscription.getAuth()));
            Notification notification = new Notification(sub, message);
            try (CloseableHttpResponse response =
                    (CloseableHttpResponse)
                            pushHttpClient.execute(
                                    pushService.preparePost(notification, Encoding.AES128GCM))) {
                log.info(
                        "Push message sent with status code: {}",
                        response.getStatusLine().getStatusCode());
                String body =
                        response.getEntity() != null
                                ? EntityUtils.toString(response.getEntity())
                                : "";
                log.info(
                        "Push response status={}, body={}",
                        response.getStatusLine(),
                        body.replaceAll("[\\r\\n]", "_"));
            }
        } catch (GeneralSecurityException | IOException | JoseException e) {
            log.error("Failed to send push notification", e);
            throw new ApplicationException(SubscriptionErrorCode.PUSH_SEND_FAILED);
        }
    }
}
