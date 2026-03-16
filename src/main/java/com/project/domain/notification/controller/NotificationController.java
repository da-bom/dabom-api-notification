package com.project.domain.notification.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dabom.messaging.kafka.event.dto.notification.NotificationType;
import com.project.domain.notification.dto.NotificationListResponse;
import com.project.domain.notification.dto.UnreadCountResponse;
import com.project.domain.notification.service.NotificationService;
import com.project.global.api.response.ApiResponse;
import com.project.global.auth.aop.CustomerId;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<NotificationListResponse> getNotifications(
            @CustomerId Long customerId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) List<NotificationType> types) {
        return ApiResponse.success(
                notificationService.getNotifications(customerId, cursor, size, isRead, types));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(@CustomerId Long customerId) {
        return ApiResponse.success(notificationService.getUnreadCount(customerId));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @CustomerId Long customerId, @PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId, customerId);
        return ApiResponse.success(null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@CustomerId Long customerId) {
        notificationService.markAllAsRead(customerId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> deleteNotification(
            @CustomerId Long customerId, @PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId, customerId);
        return ApiResponse.success(null);
    }
}
