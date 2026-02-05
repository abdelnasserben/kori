package com.kori.adapters.in.rest;

import com.kori.adapters.in.rest.controller.ConfigController;
import com.kori.adapters.in.rest.controller.PaymentController;
import com.kori.adapters.in.rest.controller.PayoutController;
import com.kori.adapters.in.rest.doc.IdempotencyRequestHasher;
import com.kori.adapters.in.rest.dto.Requests.*;
import com.kori.application.port.in.*;
import com.kori.application.result.*;
import com.kori.bootstrap.config.JacksonConfig;
import com.kori.bootstrap.config.SecurityConfig;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PaymentController.class, PayoutController.class, ConfigController.class})
@Import({JacksonConfig.class, SecurityConfig.class})
@TestPropertySource(properties = "app.security.jwt-secret=changeit-changeit-changeit-changeit")
class SecurityEndpointsWebMvcTest {

    private static final String IDEMPOTENCY_KEY = "idem-1";
    private static final String ACTOR_ID = "actor-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

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

    @Test
    void critical_endpoints_require_token_and_expected_role() throws Exception {
        when(idempotencyRequestHasher.hashPayload(any())).thenReturn("request-hash");
        when(payByCardUseCase.execute(any())).thenReturn(new PayByCardResult("tx-1", "m-1", "card-1", new BigDecimal("10"), new BigDecimal("1"), new BigDecimal("11")));
        when(merchantWithdrawAtAgentUseCase.execute(any())).thenReturn(new MerchantWithdrawAtAgentResult("tx-2", "m-1", "a-1", new BigDecimal("10"), new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("11")));
        when(agentBankDepositReceiptUseCase.execute(any())).thenReturn(new AgentBankDepositReceiptResult("tx-3", "A-1", new BigDecimal("10")));
        when(requestAgentPayoutUseCase.execute(any())).thenReturn(new AgentPayoutResult("tx-4", "p-1", "A-1", new BigDecimal("10"), "REQUESTED"));
        when(updateFeeConfigUseCase.execute(any())).thenReturn(new UpdateFeeConfigResult(
                new BigDecimal("10"), new BigDecimal("0.01"), new BigDecimal("1"), new BigDecimal("5"),
                new BigDecimal("0.01"), new BigDecimal("1"), new BigDecimal("5"), false, false, false
        ));

        var adminToken = bearerToken(List.of("ADMIN"));
        var agentToken = bearerToken(List.of("AGENT"));
        var terminalToken = bearerToken(List.of("TERMINAL"));

        assertPaymentCard(terminalToken, adminToken);
        assertMerchantWithdraw(agentToken, adminToken);
        assertAgentBankDeposit(adminToken, agentToken);
        assertPayoutRequest(adminToken, agentToken);
        assertUpdateFees(adminToken, agentToken);
    }

    private void assertPaymentCard(String successToken, String wrongRoleToken) throws Exception {
        var request = new PayByCardRequest("terminal-1", "card-1", "1234", new BigDecimal("10"));
        var payload = objectMapper.writeValueAsString(request);

        mockMvc.perform(post(ApiPaths.PAYMENTS + "/card")
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "TERMINAL")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(ApiPaths.PAYMENTS + "/card")
                        .header("Authorization", wrongRoleToken)
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "TERMINAL")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(ApiPaths.PAYMENTS + "/card")
                        .header("Authorization", successToken)
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "TERMINAL")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private void assertMerchantWithdraw(String successToken, String wrongRoleToken) throws Exception {
        var request = new MerchantWithdrawAtAgentRequest("M-1", "A-1", new BigDecimal("10"));
        var payload = objectMapper.writeValueAsString(request);

        mockMvc.perform(post(ApiPaths.PAYMENTS + "/merchant-withdraw")
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "AGENT")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(ApiPaths.PAYMENTS + "/merchant-withdraw")
                        .header("Authorization", wrongRoleToken)
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "AGENT")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(ApiPaths.PAYMENTS + "/merchant-withdraw")
                        .header("Authorization", successToken)
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "AGENT")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private void assertAgentBankDeposit(String adminToken, String wrongRoleToken) throws Exception {
        var request = new AgentBankDepositReceiptRequest("A-1", new BigDecimal("10"));
        var payload = objectMapper.writeValueAsString(request);

        mockMvc.perform(post(ApiPaths.PAYMENTS + "/agent-bank-deposits")
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "ADMIN")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(ApiPaths.PAYMENTS + "/agent-bank-deposits")
                        .header("Authorization", wrongRoleToken)
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "ADMIN")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(ApiPaths.PAYMENTS + "/agent-bank-deposits")
                        .header("Authorization", adminToken)
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "ADMIN")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private void assertPayoutRequest(String adminToken, String wrongRoleToken) throws Exception {
        var request = new RequestAgentPayoutRequest("A-1");
        var payload = objectMapper.writeValueAsString(request);

        mockMvc.perform(post(ApiPaths.PAYOUTS + "/requests")
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "ADMIN")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(ApiPaths.PAYOUTS + "/requests")
                        .header("Authorization", wrongRoleToken)
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "ADMIN")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(ApiPaths.PAYOUTS + "/requests")
                        .header("Authorization", adminToken)
                        .header(RestActorContextResolver.IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY)
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "ADMIN")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private void assertUpdateFees(String adminToken, String wrongRoleToken) throws Exception {
        var request = new UpdateFeeConfigRequest(
                new BigDecimal("10"), new BigDecimal("0.01"), new BigDecimal("1"), new BigDecimal("5"),
                new BigDecimal("0.01"), new BigDecimal("1"), new BigDecimal("5"),
                false, false, false, "test"
        );
        var payload = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch(ApiPaths.CONFIG + "/fees")
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "ADMIN")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch(ApiPaths.CONFIG + "/fees")
                        .header("Authorization", wrongRoleToken)
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "ADMIN")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch(ApiPaths.CONFIG + "/fees")
                        .header("Authorization", adminToken)
                        .header(RestActorContextResolver.ACTOR_TYPE_HEADER, "ADMIN")
                        .header(RestActorContextResolver.ACTOR_ID_HEADER, ACTOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    private String bearerToken(List<String> roles) throws JOSEException, ParseException {
        var now = Instant.now();
        var claims = new JWTClaimsSet.Builder()
                .issuer("kori-test")
                .subject("test-user")
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .claim("roles", roles)
                .build();

        var header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT)
                .build();

        var jwt = new SignedJWT(header, claims);
        var signer = new MACSigner("changeit-changeit-changeit-changeit".getBytes(StandardCharsets.UTF_8));
        jwt.sign(signer);

        return "Bearer " + jwt.serialize();
    }
}
