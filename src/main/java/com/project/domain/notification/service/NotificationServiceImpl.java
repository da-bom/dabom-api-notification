package com.project.domain.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dabom.messaging.kafka.event.dto.notification.NotificationType;
import com.project.domain.notification.dto.NotificationSlice;
import com.project.domain.notification.entity.NotificationLog;
import com.project.domain.notification.repository.NotificationLogRepository;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.NotificationErrorCode;
import com.project.global.util.CursorUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final CursorUtil cursorUtil;

    @Value("${app.notification.retention-days}")
    private int retentionDays;

    @Transactional(readOnly = true)
    @Override
    public NotificationSlice getNotifications(
            Long customerId,
            String cursor,
            int size,
            Boolean isRead,
            List<NotificationType> types) {

        Long cursorId = cursorUtil.decode(cursor);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        List<NotificationLog> logs =
                notificationLogRepository.findByCustomerIdWithCursor(
                        customerId, cursorId, size, isRead, types, cutoff);

        boolean hasNext = logs.size() > size;
        List<NotificationLog> content = hasNext ? logs.subList(0, size) : logs;

        String nextCursor =
                hasNext ? cursorUtil.encode(content.get(content.size() - 1).getId()) : null;

        long unreadCount = notificationLogRepository.countUnread(customerId, cutoff);

        return new NotificationSlice(content, nextCursor, hasNext, unreadCount);
    }

    @Transactional(readOnly = true)
    @Override
    public long getUnreadCount(Long customerId) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        return notificationLogRepository.countUnread(customerId, cutoff);
    }

    @Transactional
    @Override
    public void markAsRead(Long notificationId, Long customerId) {
        NotificationLog notification = findOwnedNotification(notificationId, customerId);
        notification.markAsRead();
    }

    @Transactional
    @Override
    public void markAllAsRead(Long customerId) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        notificationLogRepository.markAllAsRead(customerId, cutoff);
    }

    @Transactional
    @Override
    public void deleteNotification(Long notificationId, Long customerId) {
        NotificationLog notification = findOwnedNotification(notificationId, customerId);
        notification.softDelete();
    }

    private NotificationLog findOwnedNotification(Long notificationId, Long customerId) {
        NotificationLog notificationLog =
                notificationLogRepository
                        .findByIdAndCustomerId(notificationId, customerId)
                        .orElseThrow(
                                () ->
                                        new ApplicationException(
                                                NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if (notificationLog.isDeleted()) {
            throw new ApplicationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        }

        return notificationLog;
    }
}
