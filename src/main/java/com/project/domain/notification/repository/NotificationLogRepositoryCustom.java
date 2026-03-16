package com.project.domain.notification.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.dabom.messaging.kafka.event.dto.notification.NotificationType;
import com.project.domain.notification.entity.NotificationLog;

public interface NotificationLogRepositoryCustom {

    List<NotificationLog> findByCustomerIdWithCursor(
            Long customerId,
            Long cursorId,
            int size,
            Boolean isRead,
            List<NotificationType> types,
            LocalDateTime cutoff);

    long countUnread(Long customerId, LocalDateTime cutoff);

    void markAllAsRead(Long customerId, LocalDateTime cutoff);
}
