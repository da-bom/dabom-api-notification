package com.project.global.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionErrorCode implements BaseErrorCode {
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SUBSCRIPTION_001", "구독 정보를 찾을 수 없습니다."),
    PUSH_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SUBSCRIPTION_002", "푸시 알림 전송에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
