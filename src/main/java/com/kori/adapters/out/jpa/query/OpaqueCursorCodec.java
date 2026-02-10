package com.kori.adapters.out.jpa.query;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kori.application.exception.ValidationException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

final class OpaqueCursorCodec {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    record CursorPayload(Instant createdAt, String id) {}

    String encode(Instant createdAt, UUID id) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(Map.of(
                    "createdAt", createdAt.toString(),
                    "id", id.toString()
            ));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new ValidationException("Cursor encoding failed");
        }
    }

    CursorPayload decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String json = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            Map<String, String> map = OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
            return new CursorPayload(Instant.parse(map.get("createdAt")), map.get("id"));
        } catch (Exception e) {
            throw new ValidationException("Invalid cursor format", Map.of("field", "cursor", "reason", "opaque base64 expected"));
        }
    }
}
