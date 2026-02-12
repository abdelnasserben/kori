package com.kori.adapters.in.rest;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.exception.ValidationException;
import com.kori.query.model.QueryPage;
import com.kori.query.model.me.MeQueryModels;
import com.kori.query.port.in.ClientMeQueryUseCase;
import com.kori.query.port.in.ClientMeTxDetailQueryUseCase;
import com.kori.query.port.in.MerchantMeQueryUseCase;
import com.kori.query.port.in.MerchantMeTxDetailQueryUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MeQueryControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientMeQueryUseCase clientMeQueryUseCase;
    @MockitoBean
    private ClientMeTxDetailQueryUseCase clientMeTxDetailQueryUseCase;
    @MockitoBean
    private MerchantMeQueryUseCase merchantMeQueryUseCase;
    @MockitoBean
    private MerchantMeTxDetailQueryUseCase merchantMeTxDetailQueryUseCase;

    @Test
    void client_me_endpoints_enforce_auth_and_role_and_return_data() throws Exception {
        var clientJwt = jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT")).jwt(j -> j.claim("roles", List.of("CLIENT")).claim("actor_type", "CLIENT").claim("actor_id", "c-1"));
        var merchantJwt = jwt().authorities(new SimpleGrantedAuthority("ROLE_MERCHANT")).jwt(j -> j.claim("roles", List.of("MERCHANT")).claim("actor_type", "MERCHANT").claim("actor_id", "m-1"));

        when(clientMeQueryUseCase.getProfile(any())).thenReturn(new MeQueryModels.MeProfile("c-1", "+269111111", "ACTIVE", Instant.parse("2025-01-01T00:00:00Z")));
        when(clientMeQueryUseCase.getBalance(any())).thenReturn(new MeQueryModels.MeBalance("CLIENT", "c-1", new BigDecimal("123.00"), "KMF"));
        when(clientMeQueryUseCase.listCards(any())).thenReturn(List.of(new MeQueryModels.MeCardItem("card-1", "ACTIVE", Instant.parse("2025-01-01T00:00:00Z"))));
        when(clientMeQueryUseCase.listTransactions(any(), any())).thenReturn(new QueryPage<>(List.of(
                new MeQueryModels.MeTransactionItem("tx-1", "PAY_BY_CARD", "COMPLETED", new BigDecimal("10.00"), "KMF", Instant.parse("2025-01-01T00:00:00Z"))
        ), "next-cursor", true));

        mockMvc.perform(get(ApiPaths.CLIENT_ME + "/profile")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ApiPaths.CLIENT_ME + "/profile").with(merchantJwt)).andExpect(status().isForbidden());
        mockMvc.perform(get(ApiPaths.CLIENT_ME + "/profile").with(clientJwt)).andExpect(status().isOk()).andExpect(jsonPath("$.actorId").value("c-1"));
        mockMvc.perform(get(ApiPaths.CLIENT_ME + "/balance").with(clientJwt)).andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(123.00));
        mockMvc.perform(get(ApiPaths.CLIENT_ME + "/cards").with(clientJwt)).andExpect(status().isOk()).andExpect(jsonPath("$[0].cardUid").value("card-1"));
        mockMvc.perform(get(ApiPaths.CLIENT_ME + "/transactions").with(clientJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].transactionId").value("tx-1"))
                .andExpect(jsonPath("$.page.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.page.hasMore").value(true));
    }

    @Test
    void merchant_me_endpoints_enforce_auth_and_role_and_ownership() throws Exception {
        var merchantJwt = jwt().authorities(new SimpleGrantedAuthority("ROLE_MERCHANT")).jwt(j -> j.claim("roles", List.of("MERCHANT")).claim("actor_type", "MERCHANT").claim("actor_id", "m-1"));
        var clientJwt = jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT")).jwt(j -> j.claim("roles", List.of("CLIENT")).claim("actor_type", "CLIENT").claim("actor_id", "c-1"));

        when(merchantMeQueryUseCase.getProfile(any())).thenReturn(new MeQueryModels.MeProfile("m-1", "M-001", "ACTIVE", Instant.parse("2025-01-01T00:00:00Z")));
        when(merchantMeQueryUseCase.getBalance(any())).thenReturn(new MeQueryModels.MeBalance("MERCHANT", "m-1", new BigDecimal("999.00"), "KMF"));
        when(merchantMeQueryUseCase.listTransactions(any(), any())).thenReturn(new QueryPage<>(List.of(
                new MeQueryModels.MeTransactionItem("tx-2", "PAY_BY_CARD", "COMPLETED", new BigDecimal("25.00"), "KMF", Instant.parse("2025-01-01T00:00:00Z"))
        ), null, false));
        when(merchantMeQueryUseCase.listTerminals(any(), any())).thenReturn(new QueryPage<>(List.of(
                new MeQueryModels.MeTerminalItem("11111111-1111-1111-1111-111111111111", "ACTIVE", Instant.parse("2025-01-01T00:00:00Z"), null, "m-1")
        ), "n2", true));
        when(merchantMeQueryUseCase.getTerminalDetails(any(), any())).thenReturn(
                new MeQueryModels.MeTerminalItem("11111111-1111-1111-1111-111111111111", "ACTIVE", Instant.parse("2025-01-01T00:00:00Z"), null, "m-1")
        );

        mockMvc.perform(get(ApiPaths.MERCHANT_ME + "/profile")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ApiPaths.MERCHANT_ME + "/profile").with(clientJwt)).andExpect(status().isForbidden());
        mockMvc.perform(get(ApiPaths.MERCHANT_ME + "/profile").with(merchantJwt)).andExpect(status().isOk()).andExpect(jsonPath("$.actorId").value("m-1"));
        mockMvc.perform(get(ApiPaths.MERCHANT_ME + "/balance").with(merchantJwt)).andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(999.00));
        mockMvc.perform(get(ApiPaths.MERCHANT_ME + "/transactions").with(merchantJwt)).andExpect(status().isOk()).andExpect(jsonPath("$.page.hasMore").value(false));
        mockMvc.perform(get(ApiPaths.MERCHANT_ME + "/terminals").with(merchantJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].terminalUid").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.page.nextCursor").value("n2"));
        mockMvc.perform(get(ApiPaths.MERCHANT_ME + "/terminals/11111111-1111-1111-1111-111111111111").with(merchantJwt)).andExpect(status().isOk());

        when(merchantMeQueryUseCase.getTerminalDetails(any(), any())).thenThrow(new ForbiddenOperationException("Forbidden operation"));
        mockMvc.perform(get(ApiPaths.MERCHANT_ME + "/terminals/22222222-2222-2222-2222-222222222222").with(merchantJwt)).andExpect(status().isForbidden());
    }

    @Test
    void transaction_details_endpoints_validate_authz_and_ownership() throws Exception {
        var clientJwt = jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT")).jwt(j -> j.claim("roles", List.of("CLIENT")).claim("actor_type", "CLIENT").claim("actor_id", "c-1"));
        var merchantJwt = jwt().authorities(new SimpleGrantedAuthority("ROLE_MERCHANT")).jwt(j -> j.claim("roles", List.of("MERCHANT")).claim("actor_type", "MERCHANT").claim("actor_id", "m-1"));

        when(clientMeTxDetailQueryUseCase.getById(any(), any())).thenReturn(new MeQueryModels.ClientTransactionDetails(
                "11111111-1111-1111-1111-111111111111", "PAY_BY_CARD", "COMPLETED", new BigDecimal("10.00"), new BigDecimal("1.00"), new BigDecimal("11.00"), "KMF", "M-001", null, Instant.parse("2025-01-01T00:00:00Z")
        ));
        when(merchantMeTxDetailQueryUseCase.getById(any(), any())).thenReturn(new MeQueryModels.MerchantTransactionDetails(
                "11111111-1111-1111-1111-111111111111", "PAY_BY_CARD", "COMPLETED", new BigDecimal("10.00"), new BigDecimal("2.00"), new BigDecimal("12.00"), "KMF", null, "c-1", null, Instant.parse("2025-01-01T00:00:00Z")
        ));

        mockMvc.perform(get(ApiPaths.CLIENT_ME + "/transactions/11111111-1111-1111-1111-111111111111")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ApiPaths.CLIENT_ME + "/transactions/11111111-1111-1111-1111-111111111111").with(merchantJwt)).andExpect(status().isForbidden());
        mockMvc.perform(get(ApiPaths.CLIENT_ME + "/transactions/11111111-1111-1111-1111-111111111111").with(clientJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantCode").value("M-001"))
                .andExpect(jsonPath("$.fee").value(1.00))
                .andExpect(jsonPath("$.totalDebited").value(11.00))
                .andExpect(jsonPath("$.agentCode").doesNotExist());

        when(clientMeTxDetailQueryUseCase.getById(any(), eq("not-a-uuid"))).thenThrow(new ValidationException("Invalid transactionId", java.util.Map.of("field", "transactionId")));
        mockMvc.perform(get(ApiPaths.CLIENT_ME + "/transactions/not-a-uuid").with(clientJwt))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        when(clientMeTxDetailQueryUseCase.getById(any(), eq("22222222-2222-2222-2222-222222222222"))).thenThrow(new ForbiddenOperationException("Forbidden operation"));
        mockMvc.perform(get(ApiPaths.CLIENT_ME + "/transactions/22222222-2222-2222-2222-222222222222").with(clientJwt)).andExpect(status().isForbidden());

        when(clientMeTxDetailQueryUseCase.getById(any(), eq("33333333-3333-3333-3333-333333333333"))).thenThrow(new NotFoundException("Transaction not found"));
        mockMvc.perform(get(ApiPaths.CLIENT_ME + "/transactions/33333333-3333-3333-3333-333333333333").with(clientJwt)).andExpect(status().isNotFound());

        mockMvc.perform(get(ApiPaths.MERCHANT_ME + "/transactions/11111111-1111-1111-1111-111111111111").with(clientJwt)).andExpect(status().isForbidden());
        mockMvc.perform(get(ApiPaths.MERCHANT_ME + "/transactions/11111111-1111-1111-1111-111111111111").with(merchantJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fee").value(2.00))
                .andExpect(jsonPath("$.totalDebited").value(12.00));

        when(merchantMeTxDetailQueryUseCase.getById(any(), eq("22222222-2222-2222-2222-222222222222"))).thenThrow(new ForbiddenOperationException("Forbidden operation"));
        mockMvc.perform(get(ApiPaths.MERCHANT_ME + "/transactions/22222222-2222-2222-2222-222222222222").with(merchantJwt)).andExpect(status().isForbidden());

        when(merchantMeTxDetailQueryUseCase.getById(any(), eq("33333333-3333-3333-3333-333333333333"))).thenThrow(new NotFoundException("Transaction not found"));
        mockMvc.perform(get(ApiPaths.MERCHANT_ME + "/transactions/33333333-3333-3333-3333-333333333333")
                .with(merchantJwt))
                .andExpect(status().isNotFound());
    }
}
