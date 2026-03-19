package com.project.domain.usagerecord.infra.sse;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SsePublisher {

    private final EmitterRegistry emitterRegistry;

    public void pushNotificationEvent(Long familyId, String eventType, Object payload) {
        emitterRegistry.send(familyId, eventType, payload);
    }
}
