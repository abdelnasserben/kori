package com.kori.adapters.in.rest;

import com.kori.adapters.in.rest.controller.TerminalController;
import com.kori.adapters.in.rest.dto.Requests.CreateTerminalRequest;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.error.RestExceptionHandler;
import com.kori.application.exception.*;
import com.kori.application.port.in.CreateTerminalUseCase;
import com.kori.application.port.in.UpdateTerminalStatusUseCase;
import com.kori.application.result.CreateTerminalResult;
import com.kori.application.result.UpdateTerminalStatusResult;
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

@WebMvcTest(TerminalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({JacksonConfig.class, RestExceptionHandler.class})
class TerminalControllerWebMvcTest extends BaseWebMvcTest {

    private static final String URL = ApiPaths.TERMINALS;
    private static final String URL_PATH_VARIABLE_STATUS = URL + "/{terminalId}/status";


    @MockitoBean
    private CreateTerminalUseCase createTerminalUseCase;

    @MockitoBean
    private UpdateTerminalStatusUseCase updateTerminalStatusUseCase;

    @Test
    void should_create_terminal() throws Exception {
        var request = new CreateTerminalRequest("merchant-1");
        var result = new CreateTerminalResult("terminal-123", "merchant-1");
        when(createTerminalUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(post(URL)
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, "idem-1")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.terminalId").value("terminal-123"))
                .andExpect(jsonPath("$.merchantCode").value("merchant-1"));
    }

    @Test
    void should_update_terminal_status() throws Exception {
        var request = new UpdateStatusRequest("ACTIVE", "ok");
        var result = new UpdateTerminalStatusResult("terminal-123", "INACTIVE", "ACTIVE");
        when(updateTerminalStatusUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(patch(URL_PATH_VARIABLE_STATUS, "terminal-123")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectId").value("terminal-123"))
                .andExpect(jsonPath("$.previousStatus").value("INACTIVE"))
                .andExpect(jsonPath("$.newStatus").value("ACTIVE"));
    }

    @Test
    void should_return_400_when_request_is_invalid() throws Exception {
        var request = new UpdateStatusRequest("", "ok");

        mockMvc.perform(patch(URL_PATH_VARIABLE_STATUS, "terminal-123")
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
        when(updateTerminalStatusUseCase.execute(any())).thenThrow(exception);

        mockMvc.perform(patch(URL_PATH_VARIABLE_STATUS, "terminal-123")
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
