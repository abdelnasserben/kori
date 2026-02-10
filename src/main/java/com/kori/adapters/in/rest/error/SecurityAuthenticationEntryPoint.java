package com.kori.adapters.in.rest.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kori.application.exception.ApplicationErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class SecurityAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public SecurityAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        writeError(response, request.getRequestURI(), HttpStatus.UNAUTHORIZED,
                ApplicationErrorCode.AUTHENTICATION_REQUIRED.name(),
                "Authentication required");
    }

    private void writeError(HttpServletResponse response,
                            String path,
                            HttpStatus status,
                            String code,
                            String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiErrorResponse payload = new ApiErrorResponse(
                Instant.now(),
                response.getHeader("X-Correlation-Id") == null ? "" : response.getHeader("X-Correlation-Id"),
                UUID.randomUUID().toString(),
                code,
                message,
                Map.of(),
                path
        );
        objectMapper.writeValue(response.getOutputStream(), payload);
    }
}
