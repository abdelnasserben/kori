package com.kori.adapters.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

abstract class BaseWebMvcTest {

    protected static final String ACTOR_TYPE = "ADMIN";
    protected static final String ACTOR_ID = "admin-1";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
}
