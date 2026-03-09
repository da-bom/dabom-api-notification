package com.project.webpushsample.service;

import com.project.webpushsample.domain.Subscription;

public interface WebPushService {

    void subscribe(Subscription subscription);

    String getVapidPublicKey();

    void sendToUser(Long customerId, String message);

    void sendToFamily(Long familyId, String message);
}
