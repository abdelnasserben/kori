package com.kori.adapters.in.rest;

import com.kori.adapters.in.rest.controller.ConfigController;
import com.kori.adapters.in.rest.dto.Requests.UpdateCommissionConfigRequest;
import com.kori.adapters.in.rest.dto.Requests.UpdateFeeConfigRequest;
import com.kori.adapters.in.rest.error.RestExceptionHandler;
import com.kori.application.exception.*;
import com.kori.application.port.in.UpdateCommissionConfigUseCase;
import com.kori.application.port.in.UpdateFeeConfigUseCase;
import com.kori.application.result.UpdateCommissionConfigResult;
import com.kori.application.result.UpdateFeeConfigResult;
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

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({JacksonConfig.class, RestExceptionHandler.class})
class ConfigControllerWebMvcTest extends BaseWebMvcTest {

    @MockitoBean
    private UpdateFeeConfigUseCase updateFeeConfigUseCase;

    @MockitoBean
    private UpdateCommissionConfigUseCase updateCommissionConfigUseCase;

    @Test
    void should_update_fee_config() throws Exception {
        var request = new UpdateFeeConfigRequest(
                new BigDecimal("500"),
                new BigDecimal("0.01"),
                new BigDecimal("1"),
                new BigDecimal("10"),
                new BigDecimal("0.02"),
                new BigDecimal("2"),
                new BigDecimal("20"),
                "ok"
        );
        var result = new UpdateFeeConfigResult(
                new BigDecimal("500"),
                new BigDecimal("0.01"),
                new BigDecimal("1"),
                new BigDecimal("10"),
                new BigDecimal("0.02"),
                new BigDecimal("2"),
                new BigDecimal("20")
        );
        when(updateFeeConfigUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(patch("/api/config/fees")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardEnrollmentPrice").value(500))
                .andExpect(jsonPath("$.cardPaymentFeeRate").value(0.01))
                .andExpect(jsonPath("$.cardPaymentFeeMin").value(1))
                .andExpect(jsonPath("$.cardPaymentFeeMax").value(10))
                .andExpect(jsonPath("$.merchantWithdrawFeeRate").value(0.02))
                .andExpect(jsonPath("$.merchantWithdrawFeeMin").value(2))
                .andExpect(jsonPath("$.merchantWithdrawFeeMax").value(20));
    }

    @Test
    void should_update_commission_config() throws Exception {
        var request = new UpdateCommissionConfigRequest(
                new BigDecimal("50"),
                new BigDecimal("0.01"),
                new BigDecimal("1"),
                new BigDecimal("10"),
                "ok"
        );
        var result = new UpdateCommissionConfigResult(
                new BigDecimal("50"),
                new BigDecimal("0.01"),
                new BigDecimal("1"),
                new BigDecimal("10")
        );
        when(updateCommissionConfigUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(patch("/api/config/commissions")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardEnrollmentAgentCommission").value(50))
                .andExpect(jsonPath("$.merchantWithdrawCommissionRate").value(0.01))
                .andExpect(jsonPath("$.merchantWithdrawCommissionMin").value(1))
                .andExpect(jsonPath("$.merchantWithdrawCommissionMax").value(10));
    }

    @Test
    void should_return_400_when_request_is_invalid() throws Exception {
        var request = new UpdateFeeConfigRequest(
                new BigDecimal("500"),
                new BigDecimal("-0.01"),
                new BigDecimal("1"),
                new BigDecimal("10"),
                new BigDecimal("0.02"),
                new BigDecimal("2"),
                new BigDecimal("20"),
                "ok"
        );

        mockMvc.perform(patch("/api/config/fees")
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
        var request = new UpdateFeeConfigRequest(
                new BigDecimal("500"),
                new BigDecimal("0.01"),
                new BigDecimal("1"),
                new BigDecimal("10"),
                new BigDecimal("0.02"),
                new BigDecimal("2"),
                new BigDecimal("20"),
                "ok"
        );
        when(updateFeeConfigUseCase.execute(any())).thenThrow(exception);

        mockMvc.perform(patch("/api/config/fees")
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
