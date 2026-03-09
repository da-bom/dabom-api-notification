package com.project.domain.webpush.service;

import com.project.domain.webpush.controller.dto.PushSubscriptionRequest;

public interface WebPushService {

    void subscribe(PushSubscriptionRequest request, Long customerId);

    String getVapidPublicKey();

    void sendToUser(Long customerId, String message);

    void sendToFamily(Long familyId, String message);
}
