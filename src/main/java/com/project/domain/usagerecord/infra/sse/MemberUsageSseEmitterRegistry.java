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

    // 멤버 사용량 SSE emitter 목록을 조회합니다.
    public List<SseEmitter> getEmitters(Long familyId) {
        return memberUsageEmitterRegistryCore.getEmitters(familyId);
    }

    // 멤버 사용량 SSE 구독 emitter를 등록합니다.
    public SseEmitter register(Long familyId) {
        return memberUsageEmitterRegistryCore.register(familyId);
    }

    // 멤버 사용량 이벤트를 SSE로 전송합니다.
    public void send(Long familyId, String eventName, Object data) {
        memberUsageEmitterRegistryCore.send(familyId, eventName, data);
    }

    // 현재 SSE 연결이 있는 familyId 목록을 조회합니다.
    public Set<Long> activeFamilyIds() {
        return memberUsageEmitterRegistryCore.activeKeys();
    }
}
