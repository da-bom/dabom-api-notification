package com.project.domain.usagerecord.infra.sse;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TotalUsageSseEmitterRegistry {

    private final SseEmitterRegistry totalUsageEmitterRegistryCore;

    public SseEmitter register(Long familyId) {
        return totalUsageEmitterRegistryCore.register(familyId);
    }

    public void send(Long familyId, String eventName, Object data) {
        totalUsageEmitterRegistryCore.send(familyId, eventName, data);
    }

    public Set<Long> activeFamilyIds() {
        return totalUsageEmitterRegistryCore.activeKeys();
    }
}
