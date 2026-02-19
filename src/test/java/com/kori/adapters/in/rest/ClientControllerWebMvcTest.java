package com.kori.adapters.in.rest;

import com.kori.adapters.in.rest.controller.ClientController;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.error.RestExceptionHandler;
import com.kori.application.port.in.UpdateClientStatusUseCase;
import com.kori.application.result.UpdateClientStatusResult;
import com.kori.bootstrap.config.JacksonConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
@AutoConfigureMockMvc
@Import({JacksonConfig.class, RestExceptionHandler.class})
class ClientControllerWebMvcTest extends BaseWebMvcTest {

    private static final String URL_PATH_VARIABLE_STATUS = ApiPaths.CLIENTS + "/{clientCode}/status";

    @MockitoBean
    private UpdateClientStatusUseCase updateClientStatusUseCase;

    @Test
    void should_update_client_status() throws Exception {
        var request = new UpdateStatusRequest("ACTIVE", "ok");
        var result = new UpdateClientStatusResult("client-123", "INACTIVE", "ACTIVE");
        when(updateClientStatusUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(patch(URL_PATH_VARIABLE_STATUS, "client-123")
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectRef").value("client-123"))
                .andExpect(jsonPath("$.previousStatus").value("INACTIVE"))
                .andExpect(jsonPath("$.newStatus").value("ACTIVE"));
    }

    @Test
    void should_return_400_when_request_is_invalid() throws Exception {
        var request = new UpdateStatusRequest("", "ok");

        mockMvc.perform(patch(URL_PATH_VARIABLE_STATUS, "client-123")
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.phone").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.details.fields").isArray());
    }

    @ParameterizedTest
    @MethodSource("applicationExceptions")
    void should_map_application_exceptions(RuntimeException exception, HttpStatus status, String code, String message) throws Exception {
        var request = new UpdateStatusRequest("ACTIVE", "ok");
        when(updateClientStatusUseCase.execute(any())).thenThrow(exception);

        mockMvc.perform(patch(URL_PATH_VARIABLE_STATUS, "client-123")
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(status.value()))
                .andExpect(jsonPath("$.phone").value(code))
                .andExpect(jsonPath("$.message").value(message));
    }
}
