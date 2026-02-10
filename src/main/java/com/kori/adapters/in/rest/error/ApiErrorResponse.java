package com.kori.adapters.in.rest.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(name = "ApiErrorResponse", description = "Standard API error response format.")
public record ApiErrorResponse(

        Instant timestamp,
        String correlationId,
        String errorId,
        String code,
        String message,
        Map<String, Object> details,
        String path
) {
}
