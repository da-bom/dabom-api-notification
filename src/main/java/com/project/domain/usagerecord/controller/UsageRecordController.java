package com.project.domain.usagerecord.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.project.domain.usagerecord.service.UsageRecordService;
import com.project.global.auth.aop.CustomerId;

import io.swagger.v3.oas.annotations.Operation;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sse")
public class UsageRecordController {

    private final UsageRecordService usageRecordService;

    @GetMapping(value = "/usage/current", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "가족 총 데이터 사용량 조회")
    public SseEmitter getCurrentUsage(@CustomerId Long customerId) {
        return usageRecordService.subscribeTotal(customerId);
    }

    @GetMapping(value = "/usage/current/customer", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "사용량이 변화한 가족 구성원 조회")
    public SseEmitter getCurrentUsageByCustomer(@CustomerId Long customerId) {
        return usageRecordService.subscribeByMember(customerId);
    }
}
