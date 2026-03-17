package com.project.domain.notification.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.project.domain.notification.entity.NotificationLog;
import com.project.domain.notification.entity.NotificationType;

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
