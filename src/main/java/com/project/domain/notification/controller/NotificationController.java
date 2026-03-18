package com.project.domain.notification.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.common.api.response.ApiResponse;
import com.project.common.auth.aop.CustomerId;
import com.project.domain.notification.dto.NotificationSlice;
import com.project.domain.notification.dto.response.NotificationListResponse;
import com.project.domain.notification.dto.response.NotificationResponse;
import com.project.domain.notification.dto.response.UnreadCountResponse;
import com.project.domain.notification.entity.NotificationType;
import com.project.domain.notification.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회", description = "커서 기반 페이지네이션으로 알림 목록을 조회한다.")
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping
    public ApiResponse<NotificationListResponse> getNotifications(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) List<NotificationType> types) {
        NotificationSlice slice =
                notificationService.getNotifications(customerId, cursor, size, isRead, types);
        List<NotificationResponse> responses =
                slice.content().stream().map(NotificationResponse::from).toList();
        return ApiResponse.success(
                new NotificationListResponse(
                        responses, slice.nextCursor(), slice.hasNext(), slice.unreadCount()));
    }

    @Operation(summary = "읽지 않은 알림 수 조회")
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"))
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(
            @Parameter(hidden = true) @CustomerId Long customerId) {
        long count = notificationService.getUnreadCount(customerId);
        return ApiResponse.success(new UnreadCountResponse(count));
    }

    @Operation(summary = "알림 읽음 처리")
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공"))
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId, customerId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "알림 전체 읽음 처리")
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전체 읽음 처리 성공"))
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@Parameter(hidden = true) @CustomerId Long customerId) {
        notificationService.markAllAsRead(customerId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "알림 삭제")
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"))
    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> deleteNotification(
            @Parameter(hidden = true) @CustomerId Long customerId,
            @PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId, customerId);
        return ApiResponse.success(null);
    }
}
