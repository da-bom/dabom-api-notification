package com.project.domain.webpush.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.webpush.dto.request.PushSubscriptionRequest;
import com.project.domain.webpush.entity.PushSubscription;
import com.project.domain.webpush.repository.PushSubscriptionRepository;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.SubscriptionErrorCode;
import com.project.global.util.NetworkValidator;

import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushServiceImpl implements WebPushService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final PushService pushService;
    private final CloseableHttpClient pushHttpClient;
    private final ObjectMapper objectMapper;

    @Value("${vapid.key.public}")
    private String vapidPublicKey;

    @Transactional
    @Override
    public void subscribe(PushSubscriptionRequest request, Long customerId) {
        validateEndpointUrl(request.endpoint());

        Optional<PushSubscription> byEndpoint =
                pushSubscriptionRepository.findByEndpoint(request.endpoint());
        Optional<PushSubscription> byCustomer =
                pushSubscriptionRepository.findByCustomerId(customerId);

        if (byEndpoint.isPresent()) {
            PushSubscription endpointSub = byEndpoint.get();
            if (endpointSub.getCustomerId().equals(customerId)) {
                endpointSub.updateSubscription(
                        request.endpoint(), request.p256dh(), request.authKey());
            } else {
                byCustomer.ifPresent(pushSubscriptionRepository::delete);
                endpointSub.reassign(customerId, request.p256dh(), request.authKey());
            }
        } else if (byCustomer.isPresent()) {
            byCustomer
                    .get()
                    .updateSubscription(request.endpoint(), request.p256dh(), request.authKey());
        } else {
            PushSubscription subscription =
                    PushSubscription.builder()
                            .endpoint(request.endpoint())
                            .p256dh(request.p256dh())
                            .auth(request.authKey())
                            .customerId(customerId)
                            .build();
            pushSubscriptionRepository.save(subscription);
        }
    }

    @Transactional
    @Override
    public void unsubscribe(Long customerId) {
        PushSubscription subscription =
                pushSubscriptionRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
        pushSubscriptionRepository.delete(subscription);
    }

    @Transactional(readOnly = true)
    @Override
    public void sendToUser(Long customerId, String title, String message) {
        PushSubscription subscription =
                pushSubscriptionRepository
                        .findByCustomerId(customerId)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));

        sendPushNotification(subscription, title, message);
    }

    @Transactional(readOnly = true)
    @Override
    public void sendToFamily(Long familyId, String title, String message) {
        List<Long> customerIds = familyMemberRepository.findCustomerIdsByFamilyId(familyId);

        List<PushSubscription> subscriptions =
                pushSubscriptionRepository.findAllByCustomerIdIn(customerIds);

        subscriptions.forEach(subscription -> sendPushNotification(subscription, title, message));
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
            if (NetworkValidator.isInternalAddress(address)) {
                throw new ApplicationException(SubscriptionErrorCode.INVALID_ENDPOINT_URL);
            }
        } catch (IllegalArgumentException | UnknownHostException e) {
            throw new ApplicationException(SubscriptionErrorCode.INVALID_ENDPOINT_URL);
        }
    }

    private void sendPushNotification(PushSubscription subscription, String title, String message) {
        try {
            String payload =
                    objectMapper.writeValueAsString(Map.of("title", title, "body", message));

            nl.martijndwars.webpush.Subscription sub =
                    new nl.martijndwars.webpush.Subscription(
                            subscription.getEndpoint(),
                            new nl.martijndwars.webpush.Subscription.Keys(
                                    subscription.getP256dh(), subscription.getAuth()));
            Notification notification = new Notification(sub, payload);
            try (CloseableHttpResponse response =
                    pushHttpClient.execute(
                            pushService.preparePost(notification, Encoding.AES128GCM))) {
                log.info(
                        "Push message sent with status code: {}",
                        response.getStatusLine().getStatusCode());
                String body = "";
                if (response.getEntity() != null) {
                    try (InputStream is = response.getEntity().getContent()) {
                        body = new String(is.readNBytes(1024), StandardCharsets.UTF_8);
                    }
                }
                log.info(
                        "Push response status={}, body={}",
                        response.getStatusLine(),
                        body.replaceAll("[\\r\\n]", "_"));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize push payload", e);
            throw new ApplicationException(SubscriptionErrorCode.PUSH_SEND_FAILED);
        } catch (GeneralSecurityException | IOException | JoseException e) {
            log.error("Failed to send push notification", e);
            throw new ApplicationException(SubscriptionErrorCode.PUSH_SEND_FAILED);
        }
    }
}
