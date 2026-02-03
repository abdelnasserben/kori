package com.kori.adapters.in.rest;

import com.kori.adapters.in.rest.controller.AgentController;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.error.RestExceptionHandler;
import com.kori.application.exception.*;
import com.kori.application.port.in.CreateAgentUseCase;
import com.kori.application.port.in.UpdateAgentStatusUseCase;
import com.kori.application.result.CreateAgentResult;
import com.kori.application.result.UpdateAgentStatusResult;
import com.kori.bootstrap.config.JacksonConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({JacksonConfig.class, RestExceptionHandler.class})
class AgentControllerWebMvcTest extends BaseWebMvcTest {

    @MockitoBean
    private CreateAgentUseCase createAgentUseCase;

    @MockitoBean
    private UpdateAgentStatusUseCase updateAgentStatusUseCase;

    @Test
    void should_create_agent() throws Exception {
        var result = new CreateAgentResult("agent-123", "AGT-001");
        when(createAgentUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(post("/api/agents")
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, "idem-1")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agentId").value("agent-123"))
                .andExpect(jsonPath("$.agentCode").value("AGT-001"));
    }

    @Test
    void should_update_agent_status() throws Exception {
        var request = new UpdateStatusRequest("ACTIVE", "ok");
        var result = new UpdateAgentStatusResult("agent-123", "INACTIVE", "ACTIVE");
        when(updateAgentStatusUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(patch("/api/agents/{agentCode}/status", "agent-123")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value("agent-123"))
                .andExpect(jsonPath("$.previousStatus").value("INACTIVE"))
                .andExpect(jsonPath("$.newStatus").value("ACTIVE"));
    }

    @Test
    void should_return_400_when_request_is_invalid() throws Exception {
        var request = new UpdateStatusRequest("", "ok");

        mockMvc.perform(patch("/api/agents/{agentCode}/status", "agent-123")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.details.fields").isArray());
    }

    @ParameterizedTest
    @MethodSource("applicationExceptions")
    void should_map_application_exceptions(RuntimeException exception, HttpStatus status, String code, String message) throws Exception {
        var request = new UpdateStatusRequest("ACTIVE", "ok");
        when(updateAgentStatusUseCase.execute(any())).thenThrow(exception);

        mockMvc.perform(patch("/api/agents/{agentCode}/status", "agent-123")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(status.value()))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message));
    }

    private static Stream<Arguments> applicationExceptions() {
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
