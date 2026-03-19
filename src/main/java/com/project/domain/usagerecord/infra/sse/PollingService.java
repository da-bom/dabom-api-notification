package com.project.domain.usagerecord.infra.sse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final long POLLING_DELAY_MS = 1_000L;
    private static final long HEARTBEAT_DELAY_MS = 25_000L;
    private static final String EVENT_USAGE_UPDATED = "usage-updated";
    private static final String EVENT_USAGE_UPDATED_BY_MEMBER = "usage-updated-by-member";

    private final EmitterRegistry emitterRegistry;
    private final FamilyRepository familyRepository;
    private final CustomerQuotaRepository customerQuotaRepository;

    private final ConcurrentHashMap<Long, Long> lastSeenUsedBytes = new ConcurrentHashMap<>();

    // 1) 활성 familyId 전체를 일괄 조회하여 N+1 문제를 방지합니다.
    // 2) 이전 값과 다른 family만 필터링하여 SSE로 전송합니다.
    @Transactional(readOnly = true)
    @Scheduled(fixedDelay = POLLING_DELAY_MS)
    public void pollAndPushIfChanged() {
        Set<Long> activeFamilyIds = emitterRegistry.activeFamilyIds();
        if (activeFamilyIds.isEmpty()) {
            return;
        }

        Map<Long, Family> familiesById =
                familyRepository.findAllById(activeFamilyIds).stream()
                        .collect(Collectors.toMap(Family::getId, Function.identity()));

        List<Long> changedFamilyIds = new ArrayList<>();
        for (Long familyId : activeFamilyIds) {
            Family family = familiesById.get(familyId);
            if (family == null) {
                lastSeenUsedBytes.remove(familyId);
                continue;
            }

            long usedBytes = family.getUsedBytes();
            Long prev = lastSeenUsedBytes.get(familyId);

            if (prev == null || prev.longValue() != usedBytes) {
                lastSeenUsedBytes.put(familyId, usedBytes);
                changedFamilyIds.add(familyId);

                long totalQuotaBytes = family.getTotalQuotaBytes();
                long remainingBytes = totalQuotaBytes - usedBytes;

                RealtimeTotalUsageResponse totalResponse =
                        new RealtimeTotalUsageResponse(
                                familyId, usedBytes, totalQuotaBytes, remainingBytes);
                emitterRegistry.send(familyId, EVENT_USAGE_UPDATED, totalResponse);
            }
        }

        if (changedFamilyIds.isEmpty()) {
            return;
        }

        Map<Long, List<CustomerQuota>> quotasByFamilyId =
                customerQuotaRepository.findByFamilyIdIn(changedFamilyIds).stream()
                        .collect(Collectors.groupingBy(CustomerQuota::getFamilyId));

        for (Long familyId : changedFamilyIds) {
            List<CustomerQuota> quotas = quotasByFamilyId.getOrDefault(familyId, List.of());
            for (CustomerQuota quota : quotas) {
                RealtimeUsageByMemberResponse memberResponse =
                        new RealtimeUsageByMemberResponse(
                                familyId, quota.getCustomerId(), quota.getMonthlyUsedBytes());
                emitterRegistry.send(familyId, EVENT_USAGE_UPDATED_BY_MEMBER, memberResponse);
            }
        }
    }

    // 연결 유지를 위해 고정 주기로 heartbeat 이벤트를 전송합니다.
    @Scheduled(fixedDelay = HEARTBEAT_DELAY_MS)
    public void sendHeartbeat() {
        emitterRegistry.sendHeartbeat();
    }
}
