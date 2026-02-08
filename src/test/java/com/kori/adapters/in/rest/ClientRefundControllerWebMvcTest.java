package com.kori.adapters.in.rest;

import com.kori.adapters.in.rest.controller.ClientRefundController;
import com.kori.adapters.in.rest.dto.Requests.FailClientRefundRequest;
import com.kori.adapters.in.rest.dto.Requests.RequestClientRefundRequest;
import com.kori.adapters.in.rest.error.RestExceptionHandler;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.exception.ValidationException;
import com.kori.application.port.in.CompleteClientRefundUseCase;
import com.kori.application.port.in.FailClientRefundUseCase;
import com.kori.application.port.in.RequestClientRefundUseCase;
import com.kori.application.result.ClientRefundResult;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientRefundController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({JacksonConfig.class, RestExceptionHandler.class})
class ClientRefundControllerWebMvcTest extends BaseWebMvcTest {

    private static final String URL = ApiPaths.CLIENT_REFUNDS;

    @MockitoBean private RequestClientRefundUseCase requestClientRefundUseCase;
    @MockitoBean private CompleteClientRefundUseCase completeClientRefundUseCase;
    @MockitoBean private FailClientRefundUseCase failClientRefundUseCase;

    @Test
    void should_request_client_refund() throws Exception {
        var request = new RequestClientRefundRequest("client-1");
        var result = new ClientRefundResult("tx-1", "refund-1", "client-1", new BigDecimal("100"), "REQUESTED");
        when(requestClientRefundUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(post(URL + "/requests")
                        .header(ApiHeaders.IDEMPOTENCY_KEY, "idem-1")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.refundId").value("refund-1"))
                .andExpect(jsonPath("$.clientId").value("client-1"))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    void should_complete_client_refund() throws Exception {
        mockMvc.perform(post(URL + "/{refundId}/complete", "refund-1")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void should_fail_client_refund() throws Exception {
        mockMvc.perform(post(URL + "/{refundId}/fail", "refund-1")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FailClientRefundRequest("bank error"))))
                .andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @MethodSource("applicationExceptions")
    void should_map_application_exceptions(RuntimeException exception, HttpStatus status, String code, String message) throws Exception {
        when(requestClientRefundUseCase.execute(any())).thenThrow(exception);

        mockMvc.perform(post(URL + "/requests")
                        .header(ApiHeaders.IDEMPOTENCY_KEY, "idem-1")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestClientRefundRequest("client-1"))))
                .andExpect(status().is(status.value()))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message));
    }

    private static Stream<Arguments> applicationExceptions() {
        return Stream.of(
                Arguments.of(new ValidationException("Invalid input"), HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Invalid input"),
                Arguments.of(new ForbiddenOperationException("Forbidden"), HttpStatus.FORBIDDEN, "FORBIDDEN_OPERATION", "Forbidden"),
                Arguments.of(new NotFoundException("Not found"), HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Not found")
        );
    }
}
