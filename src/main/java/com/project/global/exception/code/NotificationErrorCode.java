package com.project.global.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {
    NOTIFICATION_SAVE_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR, "NOTIFICATION_001", "알림 저장에 실패했습니다."),
    NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "NOTIFICATION_002", "해당 알림에 대한 권한이 없습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_003", "알림을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
