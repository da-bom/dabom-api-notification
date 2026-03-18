package com.project.domain.notification.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.project.common.util.BaseEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "json")
    private String payload;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Builder
    public NotificationLog(
            Long customerId,
            Long familyId,
            NotificationType type,
            String title,
            String message,
            String payload,
            LocalDateTime sentAt) {
        this.customerId = customerId;
        this.familyId = familyId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.payload = payload;
        this.isRead = false;
        this.sentAt = sentAt;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
