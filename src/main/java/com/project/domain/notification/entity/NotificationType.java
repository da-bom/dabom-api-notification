package com.project.domain.notification.entity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum NotificationType {
    QUOTA_UPDATED,
    THRESHOLD_ALERT,
    CUSTOMER_BLOCKED,
    CUSTOMER_UNBLOCKED,
    POLICY_CHANGED,
    MISSION_CREATED,
    REWARD_REQUESTED,
    REWARD_APPROVED,
    REWARD_REJECTED,
    APPEAL_CREATED,
    APPEAL_APPROVED,
    APPEAL_REJECTED,
    EMERGENCY_APPROVED,
    ADMIN_PUSH;

    public static NotificationType from(
            com.dabom.messaging.kafka.event.dto.notification.NotificationType libType) {
        try {
            return valueOf(libType.name());
        } catch (IllegalArgumentException e) {
            log.warn("지원하지 않는 Kafka 알림 타입: {}", libType.name());
            throw new IllegalArgumentException("지원하지 않는 알림 타입: " + libType.name(), e);
        }
    }
}
