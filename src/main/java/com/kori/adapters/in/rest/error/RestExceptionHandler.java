package com.kori.adapters.in.rest.error;

import com.kori.adapters.in.rest.filter.CorrelationIdFilter;
import com.kori.application.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.*;

/**
 * Global REST exception mapper.
 *
 * Security principles:
 * - Never expose stack traces or technical exception messages to clients.
 * - For 500 errors: always return a generic message, empty details.
 * - Only expose validation field errors and a whitelisted subset of metadata for business errors.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    // Whitelist of metadata keys that are safe to expose publicly.
    // Keep this minimal. Add keys only when you are sure they do not leak sensitive info.
    private static final Set<String> SAFE_METADATA_KEYS = Set.of(
            "field", "reason", "status", "type", "limit", "currency"
    );

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> details = new HashMap<>();
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        details.put("fields", fieldErrors);

        // No sensitive info here; safe.
        return buildResponse(HttpStatus.BAD_REQUEST, ApplicationErrorCode.INVALID_INPUT.name(),
                "Validation failed", details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        // Convert to field-like errors (safe) instead of returning ex.getMessage()
        Map<String, Object> details = new HashMap<>();
        List<Map<String, String>> violations = ex.getConstraintViolations().stream()
                .map(v -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("field", v.getPropertyPath() == null ? "" : v.getPropertyPath().toString());
                    m.put("message", safeMessage(v.getMessage()));
                    return m;
                })
                .toList();
        details.put("fields", violations);

        return buildResponse(HttpStatus.BAD_REQUEST, ApplicationErrorCode.INVALID_INPUT.name(),
                "Validation failed", details, request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            ValidationException ex,
            HttpServletRequest request
    ) {
        // 4xx: we can expose a PUBLIC message if you ensure messages are written as public-safe.
        // Metadata is sanitized.
        log.warn("Validation error: phone={} path={} msg={}", ex.code(), request.getRequestURI(), ex.toString());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.code().name(),
                safeMessage(ex.getMessage()), sanitize(ex.metadata()), request);
    }

    @ExceptionHandler(ActorContextAuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleActorContextAuth(
            ActorContextAuthenticationException ex,
            HttpServletRequest request
    ) {
        // Auth missing/invalid at the application boundary
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ApplicationErrorCode.AUTHENTICATION_REQUIRED.name(),
                "Authentication required",
                Map.of(),
                request
        );
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(
            ForbiddenOperationException ex,
            HttpServletRequest request
    ) {
        log.warn("Forbidden: phone={} path={} msg={}", ex.code(), request.getRequestURI(), ex.toString());
        return buildResponse(HttpStatus.FORBIDDEN, ex.code().name(),
                safeMessage(ex.getMessage()), sanitize(ex.metadata()), request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Not found: phone={} path={} msg={}", ex.code(), request.getRequestURI(), ex.toString());
        return buildResponse(HttpStatus.NOT_FOUND, ex.code().name(),
                safeMessage(ex.getMessage()), sanitize(ex.metadata()), request);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleIdempotency(
            IdempotencyConflictException ex,
            HttpServletRequest request
    ) {
        log.warn("Idempotency conflict: phone={} path={} msg={}", ex.code(), request.getRequestURI(), ex.toString());
        return buildResponse(HttpStatus.CONFLICT, ex.code().name(),
                safeMessage(ex.getMessage()), sanitize(ex.metadata()), request);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientFunds(
            InsufficientFundsException ex,
            HttpServletRequest request
    ) {
        // Optional explicit mapping, keeps conflict semantics clear.
        log.warn("Insufficient funds: phone={} path={} msg={}", ex.code(), request.getRequestURI(), ex.toString());
        return buildResponse(HttpStatus.CONFLICT, ex.code().name(),
                safeMessage(ex.getMessage()), sanitize(ex.metadata()), request);
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplication(
            ApplicationException ex,
            HttpServletRequest request
    ) {
        // IMPORTANT:
        // - If TECHNICAL => never expose message/metadata
        // - Else map to appropriate status, expose safe message + sanitized metadata
        HttpStatus status = mapApplicationExceptionToStatus(ex);

        if (status.is5xxServerError() || ex.category() == ApplicationErrorCategory.TECHNICAL) {
            log.error("Technical failure: phone={} category={} path={}", ex.code(), ex.category(), request.getRequestURI(), ex);
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    ApplicationErrorCode.TECHNICAL_FAILURE.name(),
                    "Unexpected error",
                    Map.of(),
                    request);
        }

        log.warn("Application error: phone={} category={} path={} msg={}",
                ex.code(), ex.category(), request.getRequestURI(), ex.toString());

        return buildResponse(status, ex.code().name(),
                safeMessage(ex.getMessage()), sanitize(ex.metadata()), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        // Always generic response; full details only in server logs
        log.error("Unhandled exception on path={}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                ApplicationErrorCode.TECHNICAL_FAILURE.name(),
                "Unexpected error",
                Map.of(),
                request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            Map<String, Object> details,
            HttpServletRequest request
    ) {
        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                resolveCorrelationId(),
                UUID.randomUUID().toString(),
                code,
                message,
                details == null ? Map.of() : details,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }

    private Map<String, String> toFieldError(FieldError fieldError) {
        Map<String, String> error = new HashMap<>();
        error.put("field", fieldError.getField());
        error.put("message", safeMessage(fieldError.getDefaultMessage()));
        return error;
    }

    private String safeMessage(String raw) {
        // Keep messages short and avoid leaking structured data.
        // You can tighten this further if needed.
        if (raw == null || raw.isBlank()) return "Request rejected";
        String msg = raw.strip();
        // Avoid returning extremely long messages (often technical).
        if (msg.length() > 200) return "Request rejected";
        return msg;
    }

    private Map<String, Object> sanitize(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) return Map.of();

        Map<String, Object> safe = new HashMap<>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (entry.getKey() == null) continue;
            String key = entry.getKey().trim();
            if (!SAFE_METADATA_KEYS.contains(key)) continue;

            Object value = entry.getValue();
            // Avoid leaking complex objects (exceptions, entities, etc.)
            if (value == null
                    || value instanceof String
                    || value instanceof Number
                    || value instanceof Boolean
            ) {
                safe.put(key, value);
            } else {
                // Convert anything else to a safe string
                safe.put(key, String.valueOf(value));
            }
        }
        return safe;
    }

    private HttpStatus mapApplicationExceptionToStatus(ApplicationException ex) {
        // Default mapping based on category
        return switch (ex.category()) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case AUTHORIZATION -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case TECHNICAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String resolveCorrelationId() {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        return correlationId == null ? "" : correlationId;
    }
}
