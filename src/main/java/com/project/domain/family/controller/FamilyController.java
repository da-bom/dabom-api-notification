package com.project.domain.family.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.project.domain.usagerecord.infra.sse.SseSubscriber;
import com.project.global.auth.aop.CustomerId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/families")
@RequiredArgsConstructor
public class FamilyController {

    private final SseSubscriber sseSubscriber;

    @GetMapping(value = "/usage/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "가족 총 데이터 사용량 조회")
    public SseEmitter getCurrentUsage(@Parameter(hidden = true) @CustomerId Long customerId) {

        return sseSubscriber.subscribe(customerId);
    }

    @GetMapping(
            value = "/usage/sse/test/{customerId}",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "가족 총 데이터 사용량 조회 테스트")
    public SseEmitter getCurrentUsageV2(@PathVariable Long customerId) {

        return sseSubscriber.subscribe(customerId);
    }
}
