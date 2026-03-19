package com.project.domain.event.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.project.common.auth.aop.CustomerId;
import com.project.domain.usagerecord.infra.sse.SseSubscriber;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventStreamController {

    private final SseSubscriber sseSubscriber;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "실시간 이벤트 스트림 (SSE)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "SSE 스트림 연결 성공")
    public SseEmitter getEventStream(@Parameter(hidden = true) @CustomerId Long customerId) {
        return sseSubscriber.subscribe(customerId);
    }

    @GetMapping(value = "/stream/test/{customerId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "실시간 이벤트 스트림 테스트")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "SSE 테스트 스트림 연결 성공")
    public SseEmitter getEventStreamTest(@PathVariable Long customerId) {
        return sseSubscriber.subscribe(customerId);
    }
}
