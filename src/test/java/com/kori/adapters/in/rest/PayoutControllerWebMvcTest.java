package com.kori.adapters.in.rest;

import com.kori.adapters.in.rest.controller.PayoutController;
import com.kori.adapters.in.rest.dto.Requests.FailPayoutRequest;
import com.kori.adapters.in.rest.dto.Requests.RequestAgentPayoutRequest;
import com.kori.adapters.in.rest.error.RestExceptionHandler;
import com.kori.application.port.in.CompleteAgentPayoutUseCase;
import com.kori.application.port.in.FailAgentPayoutUseCase;
import com.kori.application.port.in.RequestAgentPayoutUseCase;
import com.kori.application.result.AgentPayoutResult;
import com.kori.application.result.FinalizationResult;
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

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PayoutController.class)
@AutoConfigureMockMvc
@Import({JacksonConfig.class, RestExceptionHandler.class})
class PayoutControllerWebMvcTest extends BaseWebMvcTest {

    private static final String URL =ApiPaths.PAYOUTS;

    @MockitoBean
    private RequestAgentPayoutUseCase requestAgentPayoutUseCase;

    @MockitoBean
    private CompleteAgentPayoutUseCase completeAgentPayoutUseCase;

    @MockitoBean
    private FailAgentPayoutUseCase failAgentPayoutUseCase;

    @Test
    void should_request_agent_payout() throws Exception {
        when(completeAgentPayoutUseCase.execute(any())).thenReturn(FinalizationResult.APPLIED);

        mockMvc.perform(post(URL + "/{payoutId}/complete", "payout-1")
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        )))
                .andExpect(status().isNoContent());
    }

    @Test
    void should_return_204_when_complete_payout_is_retried_after_success() throws Exception {
        when(completeAgentPayoutUseCase.execute(any())).thenReturn(FinalizationResult.ALREADY_APPLIED);

        var request = new RequestAgentPayoutRequest("agent-1");
        var result = new AgentPayoutResult(
                "tx-1",
                "payout-1",
                "agent-1",
                new BigDecimal("100"),
                "REQUESTED"
        );
        when(requestAgentPayoutUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(post(URL + "/requests")
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .header(ApiHeaders.IDEMPOTENCY_KEY, "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.payoutId").value("payout-1"))
                .andExpect(jsonPath("$.agentCode").value("agent-1"))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    void should_complete_agent_payout() throws Exception {
        mockMvc.perform(post(URL + "/{payoutId}/complete", "payout-1")
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        )))
                .andExpect(status().isNoContent());
    }

    @Test
    void should_fail_agent_payout() throws Exception {
        var request = new FailPayoutRequest("insufficient funds");
        when(failAgentPayoutUseCase.execute(any())).thenReturn(FinalizationResult.APPLIED);

        mockMvc.perform(post(URL + "/{payoutId}/fail", "payout-1")
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void should_return_204_when_fail_payout_is_retried_after_success() throws Exception {
        var request = new FailPayoutRequest("insufficient funds");
        when(failAgentPayoutUseCase.execute(any())).thenReturn(FinalizationResult.ALREADY_APPLIED);

        mockMvc.perform(post(URL + "/{payoutId}/fail", "payout-1")
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void should_return_400_when_request_is_invalid() throws Exception {
        var request = new RequestAgentPayoutRequest("");

        mockMvc.perform(post(URL + "/requests")
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .header(ApiHeaders.IDEMPOTENCY_KEY, "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.details.fields").isArray());
    }

    @ParameterizedTest
    @MethodSource("applicationExceptions")
    void should_map_application_exceptions(RuntimeException exception, HttpStatus status, String code, String message) throws Exception {
        var request = new RequestAgentPayoutRequest("agent-1");
        when(requestAgentPayoutUseCase.execute(any())).thenThrow(exception);

        mockMvc.perform(post(URL + "/requests")
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .header(ApiHeaders.IDEMPOTENCY_KEY, "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(status.value()))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message));
    }
}
