package com.kori.adapters.in.rest.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(
        name = "ApiErrorResponse",
        description = "Standard API error response format."
)
public record ApiErrorResponse(

        @Schema(description = "Error timestamp (UTC).")
        Instant timestamp,

        @Schema(description = "Application-specific error code.")
        String code,

        @Schema(description = "Human-readable error message.")
        String message,

        @Schema(description = "Additional details (validation errors, metadata).")
        Map<String, Object> details,

        @Schema(description = "HTTP request path that caused the error.")
        String path
) {
}
