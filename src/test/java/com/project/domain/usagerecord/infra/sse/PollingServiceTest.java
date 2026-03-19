package com.project.domain.usagerecord.infra.sse;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.domain.customer.repository.CustomerQuotaRepository;
import com.project.domain.family.repository.FamilyRepository;

@ExtendWith(MockitoExtension.class)
class PollingServiceTest {

    @Mock private EmitterRegistry emitterRegistry;

    @Mock private FamilyRepository familyRepository;

    @Mock private CustomerQuotaRepository customerQuotaRepository;

    @InjectMocks private PollingService pollingService;

    @Test
    @DisplayName("sendHeartbeat() 메서드에 대한 단위테스트입니다.")
    void sendHeartbeat_calls_EmitterRegistry() {
        // when
        pollingService.sendHeartbeat();

        // then
        verify(emitterRegistry).sendHeartbeat();
    }
}
