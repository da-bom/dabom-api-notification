package com.project.global.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {
    NOTIFICATION_SAVE_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR, "NOTIFICATION_001", "알림 저장에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
