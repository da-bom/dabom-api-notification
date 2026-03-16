package com.project.global.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CursorUtil {

    private final ObjectMapper objectMapper;
    private static final String NOTIFICATION_ID_FIELD = "notificationId";

    public String encode(Long notificationId) {
        String json = "{\"" + NOTIFICATION_ID_FIELD + "\":" + notificationId + "}";
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public Long decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            JsonNode node = objectMapper.readTree(decoded);
            return node.get(NOTIFICATION_ID_FIELD).asLong();
        } catch (Exception e) {
            log.warn("커서 디코딩 실패: {}", cursor, e);
            return null;
        }
    }
}
