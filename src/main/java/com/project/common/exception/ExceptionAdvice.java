package com.project.common.exception;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.project.common.api.response.ApiResponse;
import com.project.common.exception.code.BaseErrorCode;
import com.project.common.exception.code.GlobalErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class ExceptionAdvice extends ResponseEntityExceptionHandler {

    /** BaseException - 도메인 예외 (ex: ApplicationException) */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        BaseErrorCode code = e.getCode();
        log.error("[BaseException] {} - {}", code.name(), code.getMessage());

        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResponse.fail(code.getCustomCode(), code.getMessage(), null));
    }

    /** IOException - SSE 클라이언트 연결 해제 (Broken pipe) 등 */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<Void>> handleIOException(
            IOException e, HttpServletResponse response) {
        if (response.isCommitted()) {
            log.debug("[SSE] Client disconnected: {}", e.getMessage());
            return null;
        }

        log.error("[Exception] IOException: {}", e.getMessage(), e);
        GlobalErrorCode code = GlobalErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResponse.fail(code.getCustomCode(), code.getMessage(), null));
    }

    /** 그 외 모든 예외 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandledException(
            Exception e, HttpServletResponse response) {
        if (response.isCommitted()) {
            log.debug("[Exception] Response already committed: {}", e.getMessage());
            return null;
        }

        log.error("[Exception] Unhandled: {}", e.getMessage(), e);
        GlobalErrorCode code = GlobalErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResponse.fail(code.getCustomCode(), code.getMessage(), null));
    }

    /** Spring MVC 예외 (MethodArgumentNotValid, AsyncRequestTimeoutException 등) */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        if (request instanceof ServletWebRequest servletRequest) {
            HttpServletResponse response = servletRequest.getResponse();
            if (response != null && response.isCommitted()) {
                log.debug("[Spring MVC Exception] Response already committed: {}", ex.getMessage());
                return null;
            }
        }

        log.error("[Spring MVC Exception] {}: {}", ex.getClass().getSimpleName(), ex.getMessage());

        GlobalErrorCode code = GlobalErrorCode.INVALID_INPUT_VALUE;
        ApiResponse<Void> response =
                ApiResponse.fail(code.getCustomCode(), code.getMessage(), ex.getMessage());

        return ResponseEntity.status(statusCode).headers(headers).body(response);
    }
}
