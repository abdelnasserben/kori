package com.kori.adapters.in.rest;

import com.kori.adapters.in.rest.controller.AdminController;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.error.RestExceptionHandler;
import com.kori.application.port.in.CreateAdminUseCase;
import com.kori.application.port.in.UpdateAdminStatusUseCase;
import com.kori.application.result.CreateAdminResult;
import com.kori.application.result.UpdateAdminStatusResult;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc
@Import({JacksonConfig.class, RestExceptionHandler.class})
class AdminControllerWebMvcTest extends BaseWebMvcTest {

    private final static String URL = ApiPaths.ADMINS;
    private final static String URL_PATH_VARIABLE_STATUS = URL + "/{adminUsername}/status";

    @MockitoBean
    private CreateAdminUseCase createAdminUseCase;

    @MockitoBean
    private UpdateAdminStatusUseCase updateAdminStatusUseCase;

    @Test
    void should_create_admin() throws Exception {
        var result = new CreateAdminResult("admin-123");
        when(createAdminUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(post(URL)
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .header(ApiHeaders.IDEMPOTENCY_KEY, "idem-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adminUsername").value("admin-123"));
    }

    @Test
    void should_update_admin_status() throws Exception {
        var request = new UpdateStatusRequest("ACTIVE", "ok");
        var result = new UpdateAdminStatusResult("admin-123", "INACTIVE", "ACTIVE");
        when(updateAdminStatusUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(patch(URL_PATH_VARIABLE_STATUS, "admin-123")
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectRef").value("admin-123"))
                .andExpect(jsonPath("$.previousStatus").value("INACTIVE"))
                .andExpect(jsonPath("$.newStatus").value("ACTIVE"));
    }

    @Test
    void should_return_400_when_request_is_invalid() throws Exception {
        var request = new UpdateStatusRequest("", "ok");

        mockMvc.perform(patch(URL_PATH_VARIABLE_STATUS, "admin-123")
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
        when(updateAdminStatusUseCase.execute(any())).thenThrow(exception);

        mockMvc.perform(patch(URL_PATH_VARIABLE_STATUS, "admin-123")
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
