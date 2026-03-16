package com.project.domain.notification.service;

import java.time.LocalDateTime;

import com.dabom.messaging.kafka.event.dto.notification.NotificationPayload;

public interface NotificationEventService {

    void handleNotificationEvent(NotificationPayload payload, LocalDateTime sentAt);
}
