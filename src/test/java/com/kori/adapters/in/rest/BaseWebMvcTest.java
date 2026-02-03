package com.kori.adapters.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

abstract class BaseWebMvcTest {

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
}
