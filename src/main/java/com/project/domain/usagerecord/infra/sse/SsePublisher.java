package com.project.domain.usagerecord.infra.sse;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.project.domain.family.infra.cache.FamilyCacheRepository;
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
    private final AtomicReference<LocalDateTime> lastTotalBytesTime =
            new AtomicReference<>(LocalDateTime.MIN);
    private final AtomicReference<LocalDateTime> lastTotalMemberBytes =
            new AtomicReference<>(LocalDateTime.MIN);
    private final FamilyCacheRepository familyCacheRepository;

    private final ConcurrentHashMap<Long, Long> lastSeen = new ConcurrentHashMap<>();

    @Async
    public void pushTotalUsageBytes(UsageRealtimePayload payload, LocalDateTime publishedDateTime) {

        while (true) {
            LocalDateTime current = lastTotalBytesTime.get();
            if (publishedDateTime.isBefore(lastTotalBytesTime.get())) {
                log.info("drop older total event");
                return;
            }

            if (lastTotalBytesTime.compareAndSet(current, publishedDateTime)) {
                break;
            }
        }

        RealtimeTotalUsageResponse response =
                new RealtimeTotalUsageResponse(
                        payload.familyId(),
                        payload.totalUsedBytes(),
                        payload.totalLimitBytes(),
                        payload.remainingBytes());

        emitterRegistry.send(payload.familyId(), "usage-updated", response);
    }

    @Async
    public void pushMemberUsageBytes(
            UsageRealtimePayload payload, LocalDateTime publishedDateTime) {
        Long familyId = payload.familyId();
        Long customerId = payload.customerId();

        while (true) {
            LocalDateTime current = lastTotalMemberBytes.get();
            // 이벤트 순서 보장
            if (publishedDateTime.isBefore(lastTotalMemberBytes.get())) {
                log.info("drop older total event");
                return;
            }

            if (lastTotalMemberBytes.compareAndSet(current, publishedDateTime)) {
                break;
            }
        }

        RealtimeUsageByMemberResponse response =
                new RealtimeUsageByMemberResponse(
                        familyId,
                        customerId,
                        payload.totalUsedBytes(),
                        payload.totalLimitBytes(),
                        payload.remainingBytes());

        emitterRegistry.send(familyId, "usage-updated-by-member", response);
    }

    @Scheduled(fixedDelay = 1000)
    public void pollAndPushIfChanged() {
        for (Long familyId : emitterRegistry.activeFamilyIds()) {

            Optional<Long> latestOpt = familyCacheRepository.findFamilyRemainingBytes(familyId);
            if (latestOpt.isEmpty()) {
                lastSeen.remove(familyId);
                continue;
            }

            long remainingBytes = latestOpt.get();
            Long prev = lastSeen.putIfAbsent(familyId, remainingBytes);

            if (prev == null || prev.longValue() != remainingBytes) {
                lastSeen.put(familyId, remainingBytes);

                // 임시로 고정
                long totalLimitBytes = 20000;
                long totalUsedBytes = totalLimitBytes - remainingBytes;
                UsageRealtimePayload payload =
                        new UsageRealtimePayload(
                                1L,
                                1L,
                                totalUsedBytes,
                                totalLimitBytes,
                                remainingBytes,
                                30.0,
                                null,
                                null,
                                null);

                pushTotalUsageBytes(payload, LocalDateTime.now());
                pushMemberUsageBytes(payload, LocalDateTime.now());
            }
        }
    }
}
