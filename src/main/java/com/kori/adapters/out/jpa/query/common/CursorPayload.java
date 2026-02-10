package com.kori.adapters.out.jpa.query.common;

import java.time.Instant;

public record CursorPayload(Instant createdAt, String id) {
}
