package com.project.webpushsample.service;

import com.project.webpushsample.controller.dto.PushSubscriptionRequest;

public interface WebPushService {

    void subscribe(PushSubscriptionRequest request, Long customerId);

    String getVapidPublicKey();

    void sendToUser(Long customerId, String message);

    void sendToFamily(Long familyId, String message);
}
