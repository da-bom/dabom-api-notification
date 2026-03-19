package com.project.domain.usagerecord.infra.sse;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.project.domain.customer.entity.CustomerQuota;
import com.project.domain.customer.repository.CustomerQuotaRepository;
import com.project.domain.family.entity.Family;
import com.project.domain.family.repository.FamilyRepository;
import com.project.domain.usagerecord.dto.response.RealtimeTotalUsageResponse;
import com.project.domain.usagerecord.dto.response.RealtimeUsageByMemberResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollingService {

    private static final long HEARTBEAT_DELAY_MS = 25_000L;

    private final EmitterRegistry emitterRegistry;
    private final FamilyRepository familyRepository;
    private final CustomerQuotaRepository customerQuotaRepository;

    private final ConcurrentHashMap<Long, Long> lastSeenUsedBytes = new ConcurrentHashMap<>();

    // 1) 활성 familyId를 순회하며 DB에서 최신 사용량을 조회합니다.
    // 2) 이전 값과 다를 때만 SSE로 전송합니다.
    @Scheduled(fixedDelay = 1000)
    public void pollAndPushIfChanged() {
        for (Long familyId : emitterRegistry.activeFamilyIds()) {
            Family family = familyRepository.findById(familyId).orElse(null);
            if (family == null) {
                lastSeenUsedBytes.remove(familyId);
                continue;
            }

            long usedBytes = family.getUsedBytes();
            Long prev = lastSeenUsedBytes.putIfAbsent(familyId, usedBytes);

            if (prev == null || prev.longValue() != usedBytes) {
                lastSeenUsedBytes.put(familyId, usedBytes);

                long totalQuotaBytes = family.getTotalQuotaBytes();
                long remainingBytes = totalQuotaBytes - usedBytes;

                RealtimeTotalUsageResponse totalResponse =
                        new RealtimeTotalUsageResponse(
                                familyId, usedBytes, totalQuotaBytes, remainingBytes);
                emitterRegistry.send(familyId, "usage-updated", totalResponse);

                List<CustomerQuota> quotas = customerQuotaRepository.findByFamilyId(familyId);
                for (CustomerQuota quota : quotas) {
                    RealtimeUsageByMemberResponse memberResponse =
                            new RealtimeUsageByMemberResponse(
                                    familyId,
                                    quota.getCustomerId(),
                                    quota.getMonthlyUsedBytes());
                    emitterRegistry.send(familyId, "usage-updated-by-member", memberResponse);
                }
            }
        }
    }

    // 연결 유지를 위해 고정 주기로 heartbeat 이벤트를 전송합니다.
    @Scheduled(fixedDelay = HEARTBEAT_DELAY_MS)
    public void sendHeartbeat() {
        emitterRegistry.sendHeartbeat();
    }
}
