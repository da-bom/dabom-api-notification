package com.project.domain.usagerecord.infra.sse;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.project.domain.family.infra.cache.FamilyCacheRepository;
import com.project.global.event.dto.usage.UsageRealtimePayload;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PollingService {

    private final EmitterRegistry emitterRegistry;
    private final FamilyCacheRepository familyCacheRepository;
    private final ConcurrentHashMap<Long, Long> lastSeen = new ConcurrentHashMap<>();
    private final SsePublisher ssePublisher;

    private static final long HEARTBEAT_DELAY_MS = 25_000L;

    // 1) 활성 familyId를 순회하며 최신 잔여 용량을 조회합니다.
    // 2) 이전 값과 다를 때만 페이로드를 생성해 SSE로 전송합니다.
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
                                familyId,
                                4L,
                                totalUsedBytes,
                                totalLimitBytes,
                                remainingBytes,
                                30.0,
                                null,
                                null,
                                null);

                LocalDateTime now = LocalDateTime.now();
                ssePublisher.pushTotalUsageBytes(payload, now);
                ssePublisher.pushMemberUsageBytes(payload, now);
            }
        }
    }

    // 연결 유지를 위해 고정 주기로 heartbeat 이벤트를 전송합니다.
    @Scheduled(fixedDelay = HEARTBEAT_DELAY_MS)
    public void sendHeartbeat() {
        emitterRegistry.sendHeartbeat();
    }
}
