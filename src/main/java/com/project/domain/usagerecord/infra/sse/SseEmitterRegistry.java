package com.project.domain.usagerecord.infra.sse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SseEmitterRegistry {

    private final String registryName;
    private final Map<Long, List<SseEmitter>> map = new ConcurrentHashMap<>();

    public SseEmitterRegistry(String registryName) {
        this.registryName = registryName;
    }

    public List<SseEmitter> getEmitters(Long key) {
        return map.get(key);
    }

    public SseEmitter register(Long key) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        int emitterId = emitter.hashCode();

        map.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            emitter.completeWithError(e);
            remove(key, emitter);
            return emitter;
        }

        Runnable cleanup = () -> remove(key, emitter);

        emitter.onCompletion(
                () -> {
                    log.info("[{}] SSE completed: emitterId={}", registryName, emitterId);
                    cleanup.run();
                });

        emitter.onTimeout(
                () -> {
                    log.info("[{}] SSE timed out: emitterId={}", registryName, emitterId);
                    cleanup.run();
                });

        emitter.onError(
                throwable -> {
                    if (isClientDisconnect(throwable)) {
                        log.warn(
                                "[{}] SSE client disconnect: emitterId={}, cause={}",
                                registryName,
                                emitterId,
                                rootMessage(throwable));
                    } else if (isAlreadyCompleted(throwable)) {
                        log.debug(
                                "[{}] SSE already completed: emitterId={}, cause={}",
                                registryName,
                                emitterId,
                                rootMessage(throwable));
                    } else {
                        log.error(
                                "[{}] SSE send failed: emitterId={}, cause={}",
                                registryName,
                                emitterId,
                                rootMessage(throwable));
                    }
                    cleanup.run();
                });

        return emitter;
    }

    public void send(Long key, String eventName, Object data) {
        List<SseEmitter> list = map.get(key);
        if (list == null) {
            return;
        }

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                log.info("[{}] sent success: emitterId={}", registryName, emitter.hashCode());
            } catch (IOException e) {
                remove(key, emitter);
            }
        }
    }

    public Set<Long> activeKeys() {
        return map.keySet();
    }

    private void remove(Long key, SseEmitter emitter) {
        map.computeIfPresent(
                key,
                (id, list) -> {
                    list.remove(emitter);
                    return list.isEmpty() ? null : list;
                });
    }

    private boolean isClientDisconnect(Throwable throwable) {
        String message = safeLower(rootMessage(throwable));
        return message.contains("broken pipe")
                || message.contains("clientabort")
                || message.contains("eof");
    }

    private boolean isAlreadyCompleted(Throwable throwable) {
        return throwable instanceof IllegalStateException;
    }

    private String rootMessage(Throwable throwable) {
        Throwable root = (throwable.getCause() != null) ? throwable.getCause() : throwable;
        return root.getMessage();
    }

    private String safeLower(String message) {
        return message == null ? "" : message.toLowerCase();
    }
}
