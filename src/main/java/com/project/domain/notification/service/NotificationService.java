package com.project.domain.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import com.dabom.messaging.kafka.event.dto.notification.NotificationPayload;
import com.dabom.messaging.kafka.event.dto.notification.NotificationType;
import com.project.domain.notification.dto.NotificationSlice;

public interface NotificationService {

    void handleNotificationEvent(NotificationPayload payload, LocalDateTime sentAt);

    NotificationSlice getNotifications(
            Long customerId, String cursor, int size, Boolean isRead, List<NotificationType> types);

    long getUnreadCount(Long customerId);

    void markAsRead(Long notificationId, Long customerId);

    void markAllAsRead(Long customerId);

    void deleteNotification(Long notificationId, Long customerId);
}
