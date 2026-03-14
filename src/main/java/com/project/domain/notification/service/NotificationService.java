package com.project.domain.notification.service;

import java.time.LocalDateTime;

import com.dabom.messaging.kafka.event.dto.notification.CustomerBlockedPayload;
import com.dabom.messaging.kafka.event.dto.notification.ThresholdAlertPayload;

public interface NotificationService {

    void handleThresholdAlert(ThresholdAlertPayload payload, LocalDateTime sentAt);

    void handleCustomerBlocked(CustomerBlockedPayload payload, LocalDateTime sentAt);
}
