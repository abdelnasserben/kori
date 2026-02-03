package com.kori.adapters.in.rest.error;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        Instant timestamp,
        String code,
        String message,
        Map<String, Object> details,
        String path
) {
}
