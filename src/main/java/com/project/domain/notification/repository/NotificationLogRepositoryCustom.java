package com.project.domain.notification.repository;

import java.util.List;

import com.dabom.messaging.kafka.event.dto.notification.NotificationType;
import com.project.domain.notification.entity.NotificationLog;

public interface NotificationLogRepositoryCustom {

    List<NotificationLog> findByCustomerIdWithCursor(
            Long customerId, Long cursorId, int size, Boolean isRead, List<NotificationType> types);

    long countUnread(Long customerId);

    void markAllAsRead(Long customerId);
}
