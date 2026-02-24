package com.project.domain.usagerecord.infra.sse;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberUsageSseEmitterRegistry {

    private final SseEmitterRegistry memberUsageEmitterRegistryCore;

    public List<SseEmitter> getEmitters(Long familyId) {
        return memberUsageEmitterRegistryCore.getEmitters(familyId);
    }

    public SseEmitter register(Long familyId) {
        return memberUsageEmitterRegistryCore.register(familyId);
    }

    public void send(Long familyId, String eventName, Object data) {
        memberUsageEmitterRegistryCore.send(familyId, eventName, data);
    }

    public Set<Long> activeFamilyIds() {
        return memberUsageEmitterRegistryCore.activeKeys();
    }
}
