package com.kori.adapters.in.rest;

import com.kori.adapters.in.rest.controller.LedgerController;
import com.kori.adapters.in.rest.dto.Requests.SearchLedgerRequest;
import com.kori.adapters.in.rest.error.RestExceptionHandler;
import com.kori.application.port.in.GetBalanceUseCase;
import com.kori.application.port.in.SearchTransactionHistoryUseCase;
import com.kori.application.result.BalanceResult;
import com.kori.application.result.TransactionHistoryItem;
import com.kori.application.result.TransactionHistoryResult;
import com.kori.bootstrap.config.JacksonConfig;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerAccountType;
import com.kori.domain.model.transaction.TransactionType;
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
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LedgerController.class)
@AutoConfigureMockMvc
@Import({JacksonConfig.class, RestExceptionHandler.class})
class LedgerControllerWebMvcTest extends BaseWebMvcTest {

    private static final String URL = ApiPaths.LEDGER;
    private static final String URL_TRANSACTION_SEARCH = URL + "/transactions/search";

    @MockitoBean
    private GetBalanceUseCase getBalanceUseCase;

    @MockitoBean
    private SearchTransactionHistoryUseCase searchTransactionHistoryUseCase;

    @Test
    void should_get_balance() throws Exception {
        var result = new BalanceResult("CLIENT", "client-1", new BigDecimal("250"));
        when(getBalanceUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(get(URL + "/balance")
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .queryParam("accountType", "CLIENT")
                        .queryParam("ownerRef", "client-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountType").value("CLIENT"))
                .andExpect(jsonPath("$.ownerRef").value("client-1"))
                .andExpect(jsonPath("$.balance").value(250));
    }

    @Test
    void should_search_transactions() throws Exception {
        var request = new SearchLedgerRequest(
                "CLIENT",
                "client-1",
                "PAY_BY_CARD",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                25
        );
        var result = getTransactionHistoryResult();
        when(searchTransactionHistoryUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(post(URL_TRANSACTION_SEARCH)
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ledgerScope.accountType").value("CLIENT"))
                .andExpect(jsonPath("$.ledgerScope.ownerRef").value("client-1"))
                .andExpect(jsonPath("$.items[0].transactionId").value("tx-1"))
                .andExpect(jsonPath("$.items[0].transactionType").value("PAY_BY_CARD"))
                .andExpect(jsonPath("$.items[0].createdAt").value("2023-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.items[0].amount").value(100))
                .andExpect(jsonPath("$.nextBeforeTransactionId").value("tx-2"));
    }

    private static TransactionHistoryResult getTransactionHistoryResult() {
        var item = new TransactionHistoryItem(
                "tx-1",
                TransactionType.PAY_BY_CARD,
                Instant.parse("2023-01-01T00:00:00Z"),
                "client-1",
                "merchant-1",
                "agent-1",
                new BigDecimal("10"),
                new BigDecimal("5"),
                new BigDecimal("-5"),
                new BigDecimal("100"),
                new BigDecimal("2"),
                new BigDecimal("102")
        );
        return new TransactionHistoryResult(
                new LedgerAccountRef(LedgerAccountType.CLIENT, "client-1"),
                List.of(item),
                Instant.parse("2023-01-02T00:00:00Z"),
                "tx-2"
        );
    }

    @Test
    void should_return_400_when_request_is_invalid() throws Exception {
        var request = new SearchLedgerRequest(
                "CLIENT",
                "client-1",
                "PAY_BY_CARD",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                -1
        );

        mockMvc.perform(post(URL_TRANSACTION_SEARCH)
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
        var request = new SearchLedgerRequest(
                "CLIENT",
                "client-1",
                "PAY_BY_CARD",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                25
        );
        when(searchTransactionHistoryUseCase.execute(any())).thenThrow(exception);

        mockMvc.perform(post(URL_TRANSACTION_SEARCH)
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
