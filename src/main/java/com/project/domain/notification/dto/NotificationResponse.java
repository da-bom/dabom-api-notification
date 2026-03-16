package com.project.domain.notification.dto;

import java.time.LocalDateTime;

import com.project.domain.notification.entity.NotificationLog;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        String payload,
        boolean isRead,
        LocalDateTime sentAt) {

    public static NotificationResponse from(NotificationLog log) {
        return new NotificationResponse(
                log.getId(),
                log.getType().name(),
                log.getTitle(),
                log.getMessage(),
                log.getPayload(),
                log.isRead(),
                log.getSentAt());
    }
}
