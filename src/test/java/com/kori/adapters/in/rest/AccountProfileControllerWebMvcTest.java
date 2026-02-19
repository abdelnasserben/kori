package com.kori.adapters.in.rest;

import com.kori.adapters.in.rest.controller.AccountProfileController;
import com.kori.adapters.in.rest.dto.Requests.UpdateAccountProfileStatusRequest;
import com.kori.adapters.in.rest.error.RestExceptionHandler;
import com.kori.application.port.in.UpdateAccountProfileStatusUseCase;
import com.kori.application.result.UpdateAccountProfileStatusResult;
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

@WebMvcTest(AccountProfileController.class)
@AutoConfigureMockMvc
@Import({JacksonConfig.class, RestExceptionHandler.class})
class AccountProfileControllerWebMvcTest extends BaseWebMvcTest {

    private static final String URL = ApiPaths.ACCOUNT_PROFILES + "/status";

    @MockitoBean
    private UpdateAccountProfileStatusUseCase updateAccountProfileStatusUseCase;

    @Test
    void should_update_account_profile_status() throws Exception {
        var request = new UpdateAccountProfileStatusRequest("CLIENT", "owner-1", "ACTIVE", "ok");
        var result = new UpdateAccountProfileStatusResult("CLIENT", "owner-1", "INACTIVE", "ACTIVE");
        when(updateAccountProfileStatusUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(patch(URL)
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountType").value("CLIENT"))
                .andExpect(jsonPath("$.ownerRef").value("owner-1"))
                .andExpect(jsonPath("$.previousStatus").value("INACTIVE"))
                .andExpect(jsonPath("$.newStatus").value("ACTIVE"));
    }

    @Test
    void should_return_400_when_request_is_invalid() throws Exception {
        var request = new UpdateAccountProfileStatusRequest("", "", "", "ok");

        mockMvc.perform(patch(URL)
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
        var request = new UpdateAccountProfileStatusRequest("CLIENT", "owner-1", "ACTIVE", "ok");
        when(updateAccountProfileStatusUseCase.execute(any())).thenThrow(exception);

        mockMvc.perform(patch(URL)
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
