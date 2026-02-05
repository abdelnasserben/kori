package com.kori.adapters.in.rest;

import com.kori.adapters.in.rest.controller.PaymentController;
import com.kori.adapters.in.rest.dto.Requests.*;
import com.kori.adapters.in.rest.error.RestExceptionHandler;
import com.kori.application.exception.*;
import com.kori.application.port.in.*;
import com.kori.application.result.*;
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

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({JacksonConfig.class, RestExceptionHandler.class})
class PaymentControllerWebMvcTest extends BaseWebMvcTest {

    private static final String URL = ApiPaths.PAYMENTS;

    @MockitoBean
    private PayByCardUseCase payByCardUseCase;

    @MockitoBean
    private MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase;

    @MockitoBean
    private CashInByAgentUseCase cashInByAgentUseCase;

    @MockitoBean
    private ReversalUseCase reversalUseCase;

    @MockitoBean
    private AgentBankDepositReceiptUseCase agentBankDepositReceiptUseCase;

    @Test
    void should_pay_by_card() throws Exception {
        var request = new PayByCardRequest("terminal-1", "card-1", "1234", new BigDecimal("100"));
        var result = new PayByCardResult(
                "tx-1",
                "merchant-1",
                "card-1",
                new BigDecimal("100"),
                new BigDecimal("2"),
                new BigDecimal("102")
        );
        when(payByCardUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(post(URL + "/card")
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, "idem-1")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.merchantCode").value("merchant-1"))
                .andExpect(jsonPath("$.cardUid").value("card-1"))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.fee").value(2))
                .andExpect(jsonPath("$.totalDebited").value(102));
    }

    @Test
    void should_withdraw_from_merchant_at_agent() throws Exception {
        var request = new MerchantWithdrawAtAgentRequest("merchant-1", "agent-1", new BigDecimal("100"));
        var result = new MerchantWithdrawAtAgentResult(
                "tx-1",
                "merchant-1",
                "agent-1",
                new BigDecimal("100"),
                new BigDecimal("3"),
                new BigDecimal("1"),
                new BigDecimal("103")
        );
        when(merchantWithdrawAtAgentUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(post(URL + "/merchant-withdraw")
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, "idem-1")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.merchantCode").value("merchant-1"))
                .andExpect(jsonPath("$.agentCode").value("agent-1"))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.fee").value(3))
                .andExpect(jsonPath("$.commission").value(1))
                .andExpect(jsonPath("$.totalDebitedMerchant").value(103));
    }

    @Test
    void should_cash_in_by_agent() throws Exception {
        var request = new CashInByAgentRequest("+2690000000", new BigDecimal("120"));
        var result = new CashInByAgentResult(
                "tx-1",
                "agent-1",
                "client-1",
                "+2690000000",
                new BigDecimal("120")
        );
        when(cashInByAgentUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(post(URL + "/cash-in")
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, "idem-1")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "AGENT")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, "agent-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.agentId").value("agent-1"))
                .andExpect(jsonPath("$.clientId").value("client-1"))
                .andExpect(jsonPath("$.clientPhoneNumber").value("+2690000000"))
                .andExpect(jsonPath("$.amount").value(120));
    }

    @Test
    void should_record_agent_bank_deposit_receipt() throws Exception {
        var request = new AgentBankDepositReceiptRequest("A-123456", new BigDecimal("200"));
        var result = new AgentBankDepositReceiptResult("tx-1", "A-123456", new BigDecimal("200"));
        when(agentBankDepositReceiptUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(post(URL + "/agent-bank-deposits")
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, "idem-1")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.agentCode").value("A-123456"))
                .andExpect(jsonPath("$.amount").value(200));
    }

    @Test
    void should_reverse_transaction() throws Exception {
        var request = new ReversalRequest("tx-1");
        var result = new ReversalResult("tx-2", "tx-1");
        when(reversalUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(post(URL + "/reversals")
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, "idem-1")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, ACTOR_TYPE)
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("tx-2"))
                .andExpect(jsonPath("$.originalTransactionId").value("tx-1"));
    }


    @Test
    void should_return_400_when_request_is_invalid() throws Exception {
        var request = new PayByCardRequest("terminal-1", "card-1", "12", new BigDecimal("100"));

        mockMvc.perform(post(URL + "/card")
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, "idem-1")
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
        var request = new PayByCardRequest("terminal-1", "card-1", "1234", new BigDecimal("100"));
        when(payByCardUseCase.execute(any())).thenThrow(exception);

        mockMvc.perform(post(URL + "/card")
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, "idem-1")
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
