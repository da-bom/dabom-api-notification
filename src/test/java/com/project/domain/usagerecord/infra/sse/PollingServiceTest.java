package com.project.domain.usagerecord.infra.sse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.domain.customer.entity.CustomerQuota;
import com.project.domain.customer.repository.CustomerQuotaRepository;
import com.project.domain.family.entity.FamilyQuota;
import com.project.domain.family.repository.FamilyQuotaRepository;
import com.project.domain.usagerecord.dto.response.RealtimeTotalUsageResponse;
import com.project.domain.usagerecord.dto.response.RealtimeUsageByMemberResponse;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {

    @Mock private EmitterRegistry emitterRegistry;

    @Mock private FamilyQuotaRepository familyQuotaRepository;

    @Mock private CustomerQuotaRepository customerQuotaRepository;

    @InjectMocks private PollingService pollingService;

    private static final LocalDate CURRENT_MONTH = LocalDate.now().withDayOfMonth(1);

    @Test
    @DisplayName("sendHeartbeat() 메서드에 대한 단위테스트입니다.")
    void sendHeartbeat_calls_EmitterRegistry() {
        // when
        pollingService.sendHeartbeat();

        // then
        verify(emitterRegistry).sendHeartbeat();
    }

    @Test
    @DisplayName("사용량이 변경되면 usage-updated와 usage-updated-by-member 이벤트를 전송합니다.")
    void pollAndPushIfChanged_sendsEvents_whenUsageChanged() {
        // given
        Long familyId = 1L;
        FamilyQuota familyQuota =
                FamilyQuota.builder()
                        .familyId(familyId)
                        .currentMonth(CURRENT_MONTH)
                        .totalQuotaBytes(10000L)
                        .usedBytes(3000L)
                        .build();
        CustomerQuota customerQuota =
                CustomerQuota.builder()
                        .customerId(10L)
                        .familyId(familyId)
                        .monthlyUsedBytes(3000L)
                        .build();

        when(emitterRegistry.activeFamilyIds()).thenReturn(Set.of(familyId));
        when(familyQuotaRepository.findByFamilyIdInAndCurrentMonth(Set.of(familyId), CURRENT_MONTH))
                .thenReturn(List.of(familyQuota));
        when(customerQuotaRepository.findByFamilyIdIn(List.of(familyId)))
                .thenReturn(List.of(customerQuota));

        // when
        pollingService.pollAndPushIfChanged();

        // then
        verify(emitterRegistry)
                .send(
                        familyId,
                        "usage-updated",
                        new RealtimeTotalUsageResponse(familyId, 3000L, 10000L, 7000L));
        verify(emitterRegistry)
                .send(
                        familyId,
                        "usage-updated-by-member",
                        new RealtimeUsageByMemberResponse(familyId, 10L, 3000L));
    }

    @Test
    @DisplayName("사용량 변경이 없으면 이벤트를 전송하지 않습니다.")
    void pollAndPushIfChanged_doesNotSendEvents_whenUsageUnchanged() {
        // given
        Long familyId = 1L;
        FamilyQuota familyQuota =
                FamilyQuota.builder()
                        .familyId(familyId)
                        .currentMonth(CURRENT_MONTH)
                        .totalQuotaBytes(10000L)
                        .usedBytes(3000L)
                        .build();

        when(emitterRegistry.activeFamilyIds()).thenReturn(Set.of(familyId));
        when(familyQuotaRepository.findByFamilyIdInAndCurrentMonth(Set.of(familyId), CURRENT_MONTH))
                .thenReturn(List.of(familyQuota));
        when(customerQuotaRepository.findByFamilyIdIn(List.of(familyId))).thenReturn(List.of());

        // 첫 호출: 초기값 설정
        pollingService.pollAndPushIfChanged();

        // when: 두 번째 호출 (값 동일)
        pollingService.pollAndPushIfChanged();

        // then: usage-updated는 첫 호출에서 1번만 발생
        verify(emitterRegistry)
                .send(eq(familyId), eq("usage-updated"), any(RealtimeTotalUsageResponse.class));
    }

    @Test
    @DisplayName("familyId에 해당하는 FamilyQuota가 없으면 이벤트를 전송하지 않습니다.")
    void pollAndPushIfChanged_doesNotSendEvents_whenFamilyQuotaNotFound() {
        // given
        Long familyId = 999L;

        when(emitterRegistry.activeFamilyIds()).thenReturn(Set.of(familyId));
        when(familyQuotaRepository.findByFamilyIdInAndCurrentMonth(Set.of(familyId), CURRENT_MONTH))
                .thenReturn(List.of());

        // when
        pollingService.pollAndPushIfChanged();

        // then
        verify(emitterRegistry, never()).send(any(), eq("usage-updated"), any());
        verify(emitterRegistry, never()).send(any(), eq("usage-updated-by-member"), any());
    }
}
