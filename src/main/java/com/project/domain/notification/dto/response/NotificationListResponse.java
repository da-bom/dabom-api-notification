package com.project.domain.notification.dto.response;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> content, String nextCursor, boolean hasNext, long unreadCount) {}
