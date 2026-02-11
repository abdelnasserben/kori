package com.kori.adapters.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kori.adapters.in.rest.doc.IdempotencyRequestHasher;
import com.kori.application.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

abstract class BaseWebMvcTest {

    protected static final String ACTOR_TYPE_KEY = "actorType";
    protected static final String ACTOR_ID_KEY = "actorRef";
    protected static final String ACTOR_TYPE = "ADMIN";
    protected static final String ACTOR_ID = "admin-1";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected IdempotencyRequestHasher idempotencyRequestHasher;

    @BeforeEach
    void stubIdempotencyHasher() {
        when(idempotencyRequestHasher.hashPayload(any())).thenReturn("request-hash");
    }

    protected static Stream<Arguments> applicationExceptions() {
        return Stream.of(
                Arguments.of(new ValidationException("Invalid input"), HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Invalid input"),
                Arguments.of(new ForbiddenOperationException("Forbidden"), HttpStatus.FORBIDDEN, "FORBIDDEN_OPERATION", "Forbidden"),
                Arguments.of(new NotFoundException("Not found"), HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Not found"),
                Arguments.of(new IdempotencyConflictException("Conflict"), HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "Conflict"),
                Arguments.of(
                        new ApplicationException(ApplicationErrorCode.TECHNICAL_FAILURE, ApplicationErrorCategory.TECHNICAL, "Boom"),
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "TECHNICAL_FAILURE",
                        "Unexpected error"
                )
        );
    }
}
