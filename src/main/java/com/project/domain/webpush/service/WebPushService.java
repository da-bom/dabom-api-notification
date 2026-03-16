package com.project.domain.webpush.service;

import com.project.domain.webpush.controller.dto.PushSubscriptionRequest;

public interface WebPushService {

    void subscribe(PushSubscriptionRequest request, Long customerId);

    void unsubscribe(Long customerId);

    void sendToUser(Long customerId, String title, String message);

    void sendToFamily(Long familyId, String title, String message);

    String getVapidPublicKey();
}
