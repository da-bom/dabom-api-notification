package com.project.domain.usagerecord.infra.sse;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SsePublisher {

    private final EmitterRegistry emitterRegistry;

    public void pushNotificationEvent(Long familyId, String eventType, Object payload) {
        emitterRegistry.send(familyId, eventType, payload);
    }
}
