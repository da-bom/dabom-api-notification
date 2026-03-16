package com.project.domain.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dabom.messaging.kafka.event.dto.notification.NotificationPayload;
import com.dabom.messaging.kafka.event.dto.notification.NotificationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.notification.dto.NotificationSlice;
import com.project.domain.notification.entity.NotificationLog;
import com.project.domain.notification.repository.NotificationLogRepository;
import com.project.domain.usagerecord.infra.sse.SsePublisher;
import com.project.domain.webpush.service.WebPushService;
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
    private final FamilyMemberRepository familyMemberRepository;
    private final WebPushService webPushService;
    private final SsePublisher ssePublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    @Override
    public void handleNotificationEvent(NotificationPayload payload, LocalDateTime sentAt) {
        String payloadJson = serializePayload(payload.data());

        if (payload.customerId() != null) {
            saveNotification(payload.customerId(), payload, payloadJson, sentAt);
            try {
                webPushService.sendToUser(payload.customerId(), payload.title(), payload.message());
            } catch (Exception e) {
                log.warn("푸시 전송 실패 customerId={}", payload.customerId(), e);
            }
        } else {
            List<Long> memberIds =
                    familyMemberRepository.findCustomerIdsByFamilyId(payload.familyId());
            memberIds.forEach(id -> saveNotification(id, payload, payloadJson, sentAt));
            try {
                webPushService.sendToFamily(payload.familyId(), payload.title(), payload.message());
            } catch (Exception e) {
                log.warn("푸시 전송 실패 familyId={}", payload.familyId(), e);
            }
        }

        ssePublisher.pushNotificationEvent(payload.familyId(), payload.type().name(), payload);
    }

    @Transactional(readOnly = true)
    @Override
    public NotificationSlice getNotifications(
            Long customerId,
            String cursor,
            int size,
            Boolean isRead,
            List<NotificationType> types) {

        Long cursorId = CursorUtil.decode(cursor);

        List<NotificationLog> logs =
                notificationLogRepository.findByCustomerIdWithCursor(
                        customerId, cursorId, size, isRead, types);

        boolean hasNext = logs.size() > size;
        List<NotificationLog> content = hasNext ? logs.subList(0, size) : logs;

        String nextCursor =
                hasNext ? CursorUtil.encode(content.get(content.size() - 1).getId()) : null;

        long unreadCount = notificationLogRepository.countUnread(customerId);

        return new NotificationSlice(content, nextCursor, hasNext, unreadCount);
    }

    @Transactional(readOnly = true)
    @Override
    public long getUnreadCount(Long customerId) {
        return notificationLogRepository.countUnread(customerId);
    }

    @Transactional
    @Override
    public void markAsRead(Long notificationId, Long customerId) {
        NotificationLog log = findOwnedNotification(notificationId, customerId);
        log.markAsRead();
    }

    @Transactional
    @Override
    public void markAllAsRead(Long customerId) {
        notificationLogRepository.markAllAsRead(customerId);
    }

    @Transactional
    @Override
    public void deleteNotification(Long notificationId, Long customerId) {
        NotificationLog log = findOwnedNotification(notificationId, customerId);
        log.softDelete();
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

    private void saveNotification(
            Long customerId,
            NotificationPayload payload,
            String payloadJson,
            LocalDateTime sentAt) {
        notificationLogRepository.save(
                NotificationLog.builder()
                        .customerId(customerId)
                        .familyId(payload.familyId())
                        .type(payload.type())
                        .title(payload.title())
                        .message(payload.message())
                        .payload(payloadJson)
                        .sentAt(sentAt)
                        .build());
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Payload 직렬화 실패", e);
            throw new ApplicationException(NotificationErrorCode.NOTIFICATION_SAVE_FAILED);
        }
    }
}
