package com.project.common.exception.code;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionErrorCode implements BaseErrorCode {
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SUBSCRIPTION_001", "구독 정보를 찾을 수 없습니다."),
    PUSH_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SUBSCRIPTION_002", "푸시 알림 전송에 실패했습니다."),
    INVALID_ENDPOINT_URL(HttpStatus.BAD_REQUEST, "SUBSCRIPTION_003", "유효하지 않은 구독 엔드포인트 URL입니다.");

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
