package com.kori.adapters.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kori.adapters.in.rest.doc.IdempotencyRequestHasher;
import com.kori.adapters.in.rest.dto.Requests.*;
import com.kori.application.port.in.*;
import com.kori.application.result.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityEndpointsWebMvcTest {

    private static final String IDEMPOTENCY_KEY = "idem-1";
    private static final String ACTOR_ID = "actor-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IdempotencyRequestHasher idempotencyRequestHasher;

    @MockitoBean private PayByCardUseCase payByCardUseCase;
    @MockitoBean private MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase;
    @MockitoBean private CashInByAgentUseCase cashInByAgentUseCase;
    @MockitoBean private ReversalUseCase reversalUseCase;
    @MockitoBean private AgentBankDepositReceiptUseCase agentBankDepositReceiptUseCase;

    @MockitoBean private RequestAgentPayoutUseCase requestAgentPayoutUseCase;
    @MockitoBean private CompleteAgentPayoutUseCase completeAgentPayoutUseCase;
    @MockitoBean private FailAgentPayoutUseCase failAgentPayoutUseCase;

    @MockitoBean private UpdateFeeConfigUseCase updateFeeConfigUseCase;
    @MockitoBean private UpdateCommissionConfigUseCase updateCommissionConfigUseCase;
    @MockitoBean private UpdatePlatformConfigUseCase updatePlatformConfigUseCase;

    @Test
    void critical_endpoints_require_token_and_expected_role() throws Exception {
        when(idempotencyRequestHasher.hashPayload(any())).thenReturn("request-hash");
        when(payByCardUseCase.execute(any()))
                .thenReturn(new PayByCardResult("tx-1", "m-1", "card-1",
                        new BigDecimal("10"), new BigDecimal("1"), new BigDecimal("11")));
        when(merchantWithdrawAtAgentUseCase.execute(any()))
                .thenReturn(new MerchantWithdrawAtAgentResult("tx-2", "m-1", "a-1",
                        new BigDecimal("10"), new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("11")));
        when(agentBankDepositReceiptUseCase.execute(any()))
                .thenReturn(new AgentBankDepositReceiptResult("tx-3", "A-1", new BigDecimal("10")));
        when(requestAgentPayoutUseCase.execute(any()))
                .thenReturn(new AgentPayoutResult("tx-4", "p-1", "A-1", new BigDecimal("10"), "REQUESTED"));
        when(updateFeeConfigUseCase.execute(any()))
                .thenReturn(new UpdateFeeConfigResult(
                        new BigDecimal("10"), new BigDecimal("0.01"), new BigDecimal("1"), new BigDecimal("5"),
                        new BigDecimal("0.01"), new BigDecimal("1"), new BigDecimal("5"),
                        false, false, false
                ));
        when(updatePlatformConfigUseCase.execute(any()))
                .thenReturn(new UpdatePlatformConfigResult(new BigDecimal("1000")));

        // success tokens (bon rôle + actor claims)
        var adminJwt = jwtWithActorAndRoles("ADMIN", List.of("ADMIN"));
        var agentJwt = jwtWithActorAndRoles("AGENT", List.of("AGENT"));
        var terminalJwt = jwtWithActorAndRoles("TERMINAL", List.of("TERMINAL"));

        // wrong role tokens (actor claims OK mais rôle pas celui attendu)
        var wrongAsAdmin = adminJwt;
        var wrongAsAgent = agentJwt;

        assertPaymentCard(terminalJwt, wrongAsAdmin);
        assertMerchantWithdraw(agentJwt, wrongAsAdmin);
        assertAgentBankDeposit(adminJwt, wrongAsAgent);
        assertPayoutRequest(adminJwt, wrongAsAgent);
        assertUpdateFees(adminJwt, wrongAsAgent);
        assertUpdatePlatform(adminJwt, wrongAsAgent);
    }

    @Test
    void actor_context_claims_are_required_for_authenticated_requests() throws Exception {
        when(idempotencyRequestHasher.hashPayload(any())).thenReturn("request-hash");
        when(payByCardUseCase.execute(any()))
                .thenReturn(new PayByCardResult("tx-1", "m-1", "card-1",
                        new BigDecimal("10"), new BigDecimal("1"), new BigDecimal("11")));

        // jwt avec rôle TERMINAL mais sans actor claims
        var tokenMissingActorClaims = jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_TERMINAL"))
                .jwt(j -> j.claim("roles", List.of("TERMINAL")));

        // jwt avec rôle TERMINAL + actor claims
        var tokenWithActorClaims = jwtWithActorAndRoles("TERMINAL", List.of("TERMINAL"));

        var request = new PayByCardRequest("terminal-1", "card-1", "1234", new BigDecimal("10"));
        var payload = objectMapper.writeValueAsString(request);

        // sans token => 401
        mockMvc.perform(post(ApiPaths.PAYMENTS + "/card")
                        .header(ApiHeaders.IDEMPOTENCY_KEY, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Authentication required"));

        // avec token mais sans actor claims => ton resolver lève ActorContextAuthenticationException => généralement 403
        mockMvc.perform(post(ApiPaths.PAYMENTS + "/card")
                        .with(tokenMissingActorClaims)
                        .header(ApiHeaders.IDEMPOTENCY_KEY, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Authentication required"));

        // avec token + actor claims => OK (et rôle TERMINAL OK)
        mockMvc.perform(post(ApiPaths.PAYMENTS + "/card")
                        .with(tokenWithActorClaims)
                        .header(ApiHeaders.IDEMPOTENCY_KEY, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private void assertPaymentCard(RequestPostProcessor successJwt,
                                   RequestPostProcessor wrongRoleJwt) throws Exception {
        var request = new PayByCardRequest("terminal-1", "card-1", "1234", new BigDecimal("10"));
        var payload = objectMapper.writeValueAsString(request);

        // sans token => 401
        mockMvc.perform(post(ApiPaths.PAYMENTS + "/card")
                        .header(ApiHeaders.IDEMPOTENCY_KEY, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.path").exists());

        // mauvais rôle => 403
        mockMvc.perform(post(ApiPaths.PAYMENTS + "/card")
                        .with(wrongRoleJwt)
                        .header(ApiHeaders.IDEMPOTENCY_KEY, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"))
                .andExpect(jsonPath("$.message").value("Forbidden operation"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.path").exists());

        // bon rôle => 201
        mockMvc.perform(post(ApiPaths.PAYMENTS + "/card")
                        .with(successJwt)
                        .header(ApiHeaders.IDEMPOTENCY_KEY, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private void assertMerchantWithdraw(RequestPostProcessor successJwt,
                                        RequestPostProcessor wrongRoleJwt) throws Exception {
        var request = new MerchantWithdrawAtAgentRequest("M-1", "A-1", new BigDecimal("10"));
        var payload = objectMapper.writeValueAsString(request);

        mockMvc.perform(post(ApiPaths.PAYMENTS + "/merchant-withdraw")
                        .header(ApiHeaders.IDEMPOTENCY_KEY, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.path").exists());

        mockMvc.perform(post(ApiPaths.PAYMENTS + "/merchant-withdraw")
                        .with(wrongRoleJwt)
                        .header(ApiHeaders.IDEMPOTENCY_KEY, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"))
                .andExpect(jsonPath("$.message").value("Forbidden operation"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.path").exists());

        mockMvc.perform(post(ApiPaths.PAYMENTS + "/merchant-withdraw")
                        .with(successJwt)
                        .header(ApiHeaders.IDEMPOTENCY_KEY, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private void assertAgentBankDeposit(RequestPostProcessor adminJwt,
                                        RequestPostProcessor wrongRoleJwt) throws Exception {
        var request = new AgentBankDepositReceiptRequest("A-1", new BigDecimal("10"));
        var payload = objectMapper.writeValueAsString(request);

        mockMvc.perform(post(ApiPaths.PAYMENTS + "/agent-bank-deposits")
                        .header(ApiHeaders.IDEMPOTENCY_KEY, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.path").exists());

        mockMvc.perform(post(ApiPaths.PAYMENTS + "/agent-bank-deposits")
                        .with(wrongRoleJwt)
                        .header(ApiHeaders.IDEMPOTENCY_KEY, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"))
                .andExpect(jsonPath("$.message").value("Forbidden operation"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.path").exists());

        mockMvc.perform(post(ApiPaths.PAYMENTS + "/agent-bank-deposits")
                        .with(adminJwt)
                        .header(ApiHeaders.IDEMPOTENCY_KEY, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private void assertPayoutRequest(RequestPostProcessor adminJwt,
                                     RequestPostProcessor wrongRoleJwt) throws Exception {
        var request = new RequestAgentPayoutRequest("A-1");
        var payload = objectMapper.writeValueAsString(request);

        mockMvc.perform(post(ApiPaths.PAYOUTS + "/requests")
                        .header(ApiHeaders.IDEMPOTENCY_KEY, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.path").exists());

        mockMvc.perform(post(ApiPaths.PAYOUTS + "/requests")
                        .with(wrongRoleJwt)
                        .header(ApiHeaders.IDEMPOTENCY_KEY, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"))
                .andExpect(jsonPath("$.message").value("Forbidden operation"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.path").exists());

        mockMvc.perform(post(ApiPaths.PAYOUTS + "/requests")
                        .with(adminJwt)
                        .header(ApiHeaders.IDEMPOTENCY_KEY, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private void assertUpdateFees(RequestPostProcessor adminJwt,
                                  RequestPostProcessor wrongRoleJwt) throws Exception {
        var request = new UpdateFeeConfigRequest(
                new BigDecimal("10"), new BigDecimal("0.01"), new BigDecimal("1"), new BigDecimal("5"),
                new BigDecimal("0.01"), new BigDecimal("1"), new BigDecimal("5"),
                false, false, false, "test"
        );
        var payload = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch(ApiPaths.CONFIG + "/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.path").exists());

        mockMvc.perform(patch(ApiPaths.CONFIG + "/fees")
                        .with(wrongRoleJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"))
                .andExpect(jsonPath("$.message").value("Forbidden operation"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.path").exists());

        mockMvc.perform(patch(ApiPaths.CONFIG + "/fees")
                        .with(adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    private void assertUpdatePlatform(RequestPostProcessor adminJwt,
                                      RequestPostProcessor wrongRoleJwt) throws Exception {
        var request = new UpdatePlatformConfigRequest(new BigDecimal("1000"), "test");
        var payload = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch(ApiPaths.CONFIG + "/platform")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(patch(ApiPaths.CONFIG + "/platform")
                        .with(wrongRoleJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_OPERATION"));

        mockMvc.perform(patch(ApiPaths.CONFIG + "/platform")
                        .with(adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    /**
     * Construit un Jwt "mock" compatible avec:
     * - ton JwtGrantedAuthoritiesConverter (claim "roles" + prefix ROLE_)
     * - ton ActorContextArgumentResolver (claims actor_type / actor_id)
     */
    private RequestPostProcessor jwtWithActorAndRoles(String actorType, List<String> roles) {
        var authorities = roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .toArray(SimpleGrantedAuthority[]::new);

        return jwt()
                .authorities(authorities)
                .jwt(j -> j
                        .claim("roles", roles)      // optionnel : utile si tu veux garder la cohérence
                        .claim("actor_type", actorType)
                        .claim("actor_id", ACTOR_ID)
                );
    }
}
