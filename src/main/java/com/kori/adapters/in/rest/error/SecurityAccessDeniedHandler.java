package com.kori.adapters.in.rest.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kori.application.exception.ApplicationErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public class SecurityAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        writeError(response, request.getRequestURI(), HttpStatus.FORBIDDEN,
                ApplicationErrorCode.FORBIDDEN_OPERATION.name(),
                "Forbidden operation");
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
                code,
                message,
                Map.of(),
                path
        );
        objectMapper.writeValue(response.getOutputStream(), payload);
    }
}
