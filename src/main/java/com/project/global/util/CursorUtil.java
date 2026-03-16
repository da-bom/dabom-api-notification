package com.project.global.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CursorUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String NOTIFICATION_ID_FIELD = "notificationId";

    private CursorUtil() {}

    public static String encode(Long notificationId) {
        String json = "{\"" + NOTIFICATION_ID_FIELD + "\":" + notificationId + "}";
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    public static Long decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            JsonNode node = OBJECT_MAPPER.readTree(decoded);
            return node.get(NOTIFICATION_ID_FIELD).asLong();
        } catch (Exception e) {
            log.warn("커서 디코딩 실패: {}", cursor, e);
            return null;
        }
    }
}
