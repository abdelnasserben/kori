package com.kori.adapters.in.rest;

import com.kori.adapters.in.rest.controller.CardController;
import com.kori.adapters.in.rest.dto.Requests.AddCardToExistingClientRequest;
import com.kori.adapters.in.rest.dto.Requests.AgentCardStatusRequest;
import com.kori.adapters.in.rest.dto.Requests.EnrollCardRequest;
import com.kori.adapters.in.rest.dto.Requests.UpdateStatusRequest;
import com.kori.adapters.in.rest.error.RestExceptionHandler;
import com.kori.application.port.in.*;
import com.kori.application.result.AddCardToExistingClientResult;
import com.kori.application.result.EnrollCardResult;
import com.kori.application.result.UpdateCardStatusResult;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
@AutoConfigureMockMvc
@Import({JacksonConfig.class, RestExceptionHandler.class})
class CardControllerWebMvcTest extends BaseWebMvcTest {

    private final static String URL = ApiPaths.CARDS;
    private final static String URL_ENROLL = ApiPaths.CARDS + "/enroll";
    private final static String URL_ADD = ApiPaths.CARDS + "/add";

    @MockitoBean
    private EnrollCardUseCase enrollCardUseCase;

    @MockitoBean
    private AddCardToExistingClientUseCase addCardToExistingClientUseCase;

    @MockitoBean
    private AdminUpdateCardStatusUseCase adminUpdateCardStatusUseCase;

    @MockitoBean
    private AdminUnblockCardUseCase adminUnblockCardUseCase;

    @MockitoBean
    private AgentUpdateCardStatusUseCase agentUpdateCardStatusUseCase;

    @Test
    void should_enroll_card() throws Exception {
        var request = new EnrollCardRequest("+2691234567", "card-123", "1234", "agent-1");
        var result = new EnrollCardResult(
                "tx-1",
                "+2691234567",
                "card-123",
                new BigDecimal("500"),
                new BigDecimal("50"),
                true,
                true
        );
        when(enrollCardUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(post(URL_ENROLL)
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .header(ApiHeaders.IDEMPOTENCY_KEY, "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.clientPhoneNumber").value("+2691234567"))
                .andExpect(jsonPath("$.cardUid").value("card-123"))
                .andExpect(jsonPath("$.cardPrice").value(500))
                .andExpect(jsonPath("$.agentCommission").value(50))
                .andExpect(jsonPath("$.clientCreated").value(true))
                .andExpect(jsonPath("$.clientAccountProfileCreated").value(true));
    }

    @Test
    void should_add_card_to_existing_client() throws Exception {
        var request = new AddCardToExistingClientRequest("+2691234567", "card-456", "1234", "agent-1");
        var result = new AddCardToExistingClientResult(
                "tx-2",
                "11111111-1111-1111-1111-111111111111",
                "card-456",
                new BigDecimal("500"),
                new BigDecimal("50")
        );
        when(addCardToExistingClientUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(post(URL_ADD)
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .header(ApiHeaders.IDEMPOTENCY_KEY, "idem-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("tx-2"))
                .andExpect(jsonPath("$.clientCode").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.cardUid").value("card-456"))
                .andExpect(jsonPath("$.cardPrice").value(500))
                .andExpect(jsonPath("$.agentCommission").value(50));
    }

    @Test
    void should_admin_update_card_status() throws Exception {
        var cardUid = "04A1B2C3D4E5F6A7B8C9D";
        var request = new UpdateStatusRequest("BLOCKED", "fraud");
        var result = new UpdateCardStatusResult(cardUid, "ACTIVE", "BLOCKED");
        when(adminUpdateCardStatusUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(patch(URL + "/{cardUid}/status/admin", cardUid)
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectRef").value(cardUid))
                .andExpect(jsonPath("$.previousStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.newStatus").value("BLOCKED"));
    }

    @Test
    void should_admin_unblock_card() throws Exception {
        var cardUid = "04A1B2C3D4E5F6A7B8C9D";
        var result = new UpdateCardStatusResult(cardUid, "BLOCKED", "ACTIVE");
        when(adminUnblockCardUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(post(URL + "/{cardUid}/unblock", cardUid)
                        .with(jwt().jwt(jwt -> jwt
                        .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                        .claim(ACTOR_ID_KEY, ACTOR_ID)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectRef").value(cardUid))
                .andExpect(jsonPath("$.previousStatus").value("BLOCKED"))
                .andExpect(jsonPath("$.newStatus").value("ACTIVE"));
    }

    @Test
    void should_agent_update_card_status() throws Exception {
        var cardUid = "04A1B2C3D4E5F6A7B8C9D";
        var request = new AgentCardStatusRequest("agent-1", "BLOCKED", "lost");
        var result = new UpdateCardStatusResult(cardUid, "ACTIVE", "BLOCKED");
        when(agentUpdateCardStatusUseCase.execute(any())).thenReturn(result);

        mockMvc.perform(patch(URL + "/{cardUid}/status/agent", cardUid)
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectRef").value(cardUid))
                .andExpect(jsonPath("$.previousStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.newStatus").value("BLOCKED"));
    }

    @Test
    void should_return_400_when_request_is_invalid() throws Exception {
        var request = new EnrollCardRequest("123", "", "12", "");

        mockMvc.perform(post(URL_ENROLL)
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .header(ApiHeaders.IDEMPOTENCY_KEY, "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.phone").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.details.fields").isArray());
    }

    @ParameterizedTest
    @MethodSource("applicationExceptions")
    void should_map_application_exceptions(RuntimeException exception, HttpStatus status, String code, String message) throws Exception {
        var request = new EnrollCardRequest("+2691234567", "card-123", "1234", "agent-1");
        when(enrollCardUseCase.execute(any())).thenThrow(exception);

        mockMvc.perform(post(URL_ENROLL)
                        .with(jwt().jwt(jwt -> jwt
                                .claim(ACTOR_TYPE_KEY, ACTOR_TYPE)
                                .claim(ACTOR_ID_KEY, ACTOR_ID)
                        ))
                        .header(ApiHeaders.IDEMPOTENCY_KEY, "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(status.value()))
                .andExpect(jsonPath("$.phone").value(code))
                .andExpect(jsonPath("$.message").value(message));
    }
}
