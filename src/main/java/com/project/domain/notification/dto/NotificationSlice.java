package com.project.domain.notification.dto;

import java.util.List;

import com.project.domain.notification.entity.NotificationLog;

public record NotificationSlice(
        List<NotificationLog> content, String nextCursor, boolean hasNext, long unreadCount) {}
