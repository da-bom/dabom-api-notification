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

    private final ConcurrentHashMap<Long, Long> lastSeen = new ConcurrentHashMap<>();

    @Async
    public void pushTotalUsageBytes(UsageRealtimePayload payload, LocalDateTime publishedDateTime) {
        log.info("pushTotalUsageBytes thread : {}", Thread.currentThread().getName());
        Long familyId = payload.familyId();
        AtomicReference<LocalDateTime> lastRef = totalTsRef(familyId);

        while (true) {
            LocalDateTime current = lastRef.get();

            if (!publishedDateTime.isAfter(current)) {
                log.info(
                        "drop older total event: familyId={}, incoming={}, last={}",
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

    @Async
    public void pushMemberUsageBytes(
            UsageRealtimePayload payload, LocalDateTime publishedDateTime) {
        log.info("pushMemberusageBytes thread : {}", Thread.currentThread().getName());

        Long familyId = payload.familyId();
        Long customerId = payload.customerId();
        AtomicReference<LocalDateTime> lastRef = memberTsRef(familyId);

        while (true) {
            LocalDateTime current = lastRef.get();

            if (!publishedDateTime.isAfter(current)) {
                log.info(
                        "drop older member event: familyId={}, customerId={}, incoming={}, last={}",
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

    private AtomicReference<LocalDateTime> totalTsRef(Long familyId) {
        return lastTotalBytesTimeByFamily.computeIfAbsent(
                familyId, id -> new AtomicReference<>(LocalDateTime.MIN));
    }

    private AtomicReference<LocalDateTime> memberTsRef(Long familyId) {
        return lastTotalMemberBytesTimeByFamily.computeIfAbsent(
                familyId, id -> new AtomicReference<>(LocalDateTime.MIN));
    }
}
