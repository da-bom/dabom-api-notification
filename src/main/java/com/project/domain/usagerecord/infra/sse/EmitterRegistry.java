package com.project.domain.usagerecord.infra.sse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EmitterRegistry {
    private final Map<Long, List<SseEmitter>> map = new ConcurrentHashMap<>();
    private static final long EMITTER_TIMEOUT_MS = 60_000L;

    // 1) emitter를 등록하고 connected 이벤트를 즉시 전송합니다.
    // 2) 완료/타임아웃/에러 콜백에서 공통 cleanup으로 연결을 정리합니다.
    public SseEmitter register(Long familyId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        int id = emitter.hashCode();

        map.computeIfAbsent(familyId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        try {
            emitter.send(SseEmitter.event().name("connected").data("🎯연결되었습니다."));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }

        Runnable cleanup = () -> remove(familyId, emitter);

        emitter.onCompletion(
                () -> {
                    log.info("🎯SSE completed: emitter={}", emitter);
                    cleanup.run();
                });

        emitter.onTimeout(
                () -> {
                    log.info("🎯SSE timed out: emitter={}", emitter);
                    cleanup.run();
                });

        emitter.onError(
                throwable -> {
                    if (isClientDisconnect(throwable)) {
                        log.warn(
                                "🎯SSE client disconnect: emitterId={}, cause={}",
                                id,
                                rootMessage(throwable));
                    } else if (isAlreadyCompleted(throwable)) {
                        log.debug(
                                "🎯SSE already completed: emitterId={}, cause={}",
                                id,
                                rootMessage(throwable));
                    } else {
                        log.error(
                                "🎯SSE send failed: emitterId={}, cause={}",
                                id,
                                rootMessage(throwable));
                    }
                    cleanup.run();
                });
        return emitter;
    }

    // 특정 familyId에 연결된 모든 emitter로 지정 이벤트를 브로드캐스트합니다.
    public void send(Long familyId, String eventName, Object data) {
        List<SseEmitter> list = map.get(familyId);
        // SSE에 연결된 familyId가 없을땐 이벤트를 소멸시킵니다.
        if (list == null) {
            log.debug(
                    "🎯skip send: no active emitters, familyId={}, eventName={}",
                    familyId,
                    eventName);
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                log.info("🎯sent success: emitter= id {}", emitter.hashCode());
            } catch (IOException e) {
                remove(familyId, emitter);
            }
        }
    }

    // 활성 연결에 heartbeat 이벤트를 주기적으로 전송합니다.
    public void sendHeartbeat() {
        final String heartbeatEventName = "heartbeat";
        final String heartbeatEventBody = "ping";
        for (Long familyId : activeFamilyIds()) {
            send(familyId, heartbeatEventName, heartbeatEventBody);
        }
    }

    private void remove(Long familyId, SseEmitter emitter) {
        map.computeIfPresent(
                familyId,
                (id, list) -> {
                    list.remove(emitter);

                    if (list.isEmpty()) {
                        return null;
                    }
                    return list;
                });
    }

    private boolean isClientDisconnect(Throwable throwable) {
        String message = rootMessage(throwable).toLowerCase();
        return message.contains("broken pipe")
                || message.contains("clientabort")
                || message.contains("eof");
    }

    private boolean isAlreadyCompleted(Throwable throwable) {
        return throwable instanceof IllegalStateException;
    }

    private String rootMessage(Throwable throwable) {
        return (throwable.getCause() != null ? throwable.getCause() : throwable).getMessage();
    }

    // 현재 SSE 연결이 살아있는 familyId 목록을 반환합니다.
    public Set<Long> activeFamilyIds() {
        return map.keySet();
    }
}
