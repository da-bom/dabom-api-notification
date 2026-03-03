package com.project.domain.usagerecord.infra.sse;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.project.domain.usagerecord.dto.response.RealtimeTotalUsageResponse;
import com.project.domain.usagerecord.dto.response.RealtimeUsageByMemberResponse;
import com.project.global.event.dto.usage.UsageRealtimePayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SsePublisher {

    private final EmitterRegistry emitterRegistry;

    private final ConcurrentHashMap<Long, AtomicReference<LocalDateTime>>
            lastTotalBytesTimeByFamily = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, AtomicReference<LocalDateTime>>
            lastTotalMemberBytesTimeByFamily = new ConcurrentHashMap<>();

    // 1) 가족별 최신 발행 시각보다 오래된 이벤트는 폐기합니다.
    // 2) 최신 이벤트만 응답 DTO로 변환해 SSE로 전송합니다.
    @Async
    public void pushTotalUsageBytes(UsageRealtimePayload payload, LocalDateTime publishedDateTime) {
        log.info("pushTotalUsageBytes thread : {}", Thread.currentThread().getName());
        Long familyId = payload.familyId();
        AtomicReference<LocalDateTime> lastRef = totalTsRef(familyId);

        while (true) {
            LocalDateTime current = lastRef.get();

            if (!publishedDateTime.isAfter(current)) {
                log.info(
                        "🎯drop older total event: familyId={}, incoming={}, last={}",
                        familyId,
                        publishedDateTime,
                        current);
                return;
            }

            if (lastRef.compareAndSet(current, publishedDateTime)) {
                break;
            }
        }

        RealtimeTotalUsageResponse response =
                new RealtimeTotalUsageResponse(
                        payload.familyId(),
                        payload.totalUsedBytes(),
                        payload.totalLimitBytes(),
                        payload.remainingBytes());

        emitterRegistry.send(familyId, "usage-updated", response);
    }

    // 1) 가족별 멤버 사용량 이벤트의 최신 시각을 CAS로 보장합니다.
    // 2) 최신 이벤트만 멤버 단위 응답으로 변환해 SSE로 전송합니다.
    @Async
    public void pushMemberUsageBytes(
            UsageRealtimePayload payload, LocalDateTime publishedDateTime) {
        log.info("🎯pushMemberusageBytes thread : {}", Thread.currentThread().getName());

        Long familyId = payload.familyId();
        Long customerId = payload.customerId();
        AtomicReference<LocalDateTime> lastRef = memberTsRef(familyId);

        while (true) {
            LocalDateTime current = lastRef.get();

            if (!publishedDateTime.isAfter(current)) {
                log.info(
                        "🎯drop older member event: familyId={}, customerId={}, incoming={},"
                                + " last={}",
                        familyId,
                        customerId,
                        publishedDateTime,
                        current);
                return;
            }

            if (lastRef.compareAndSet(current, publishedDateTime)) {
                break;
            }
        }

        RealtimeUsageByMemberResponse response =
                new RealtimeUsageByMemberResponse(familyId, customerId, payload.monthlyUsedBytes());
        emitterRegistry.send(familyId, "usage-updated-by-member", response);
    }

    // 가족별 총 사용량 최신 시각 저장소를 초기화하거나 반환합니다.
    private AtomicReference<LocalDateTime> totalTsRef(Long familyId) {
        return lastTotalBytesTimeByFamily.computeIfAbsent(
                familyId, id -> new AtomicReference<>(LocalDateTime.MIN));
    }

    // 가족별 멤버 사용량 최신 시각 저장소를 초기화하거나 반환합니다.
    private AtomicReference<LocalDateTime> memberTsRef(Long familyId) {
        return lastTotalMemberBytesTimeByFamily.computeIfAbsent(
                familyId, id -> new AtomicReference<>(LocalDateTime.MIN));
    }
}
