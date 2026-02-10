package com.kori.adapters.in.rest;

import com.kori.application.exception.ValidationException;
import com.kori.application.port.in.query.*;
import com.kori.application.query.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "kori.security.keycloak.client-id=kori-api")
class BackofficeQueryControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackofficeTransactionQueryUseCase transactionQueryUseCase;
    @MockitoBean
    private BackofficeAuditEventQueryUseCase auditEventQueryUseCase;
    @MockitoBean
    private BackofficeActorQueryUseCase actorQueryUseCase;
    @MockitoBean
    private BackofficeActorDetailQueryUseCase actorDetailQueryUseCase;
    @MockitoBean
    private BackofficeLookupQueryUseCase lookupQueryUseCase;

    @Test
    void transactions_endpoint_supports_pagination_and_security() throws Exception {
        when(transactionQueryUseCase.list(any())).thenReturn(new QueryPage<>(List.of(
                new BackofficeTransactionItem("tx-1", "PAY_BY_CARD", "COMPLETED", new BigDecimal("100.00"), "KMF", "M-001", "A-001", "c1", Instant.parse("2025-01-01T00:00:00Z"))
        ), "cursor-2", true));

        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("roles", List.of("ADMIN")).claim("actor_type", "ADMIN").claim("actor_id", "adm-1"));
        var client = jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT")).jwt(j -> j.claim("roles", List.of("CLIENT")).claim("actor_type", "CLIENT").claim("actor_id", "cli-1"));

        mockMvc.perform(get(ApiPaths.BACKOFFICE_TRANSACTIONS))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(ApiPaths.BACKOFFICE_TRANSACTIONS).with(client))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(ApiPaths.BACKOFFICE_TRANSACTIONS).with(admin).param("limit", "1").param("cursor", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].transactionId").value("tx-1"))
                .andExpect(jsonPath("$.page.nextCursor").value("cursor-2"))
                .andExpect(jsonPath("$.page.hasMore").value(true));
    }

    @Test
    void transaction_details_endpoint_is_admin_only() throws Exception {
        when(transactionQueryUseCase.getById("tx-1")).thenReturn(new BackofficeTransactionDetails("tx-1", "PAY_BY_CARD", "COMPLETED", new BigDecimal("100.00"), "KMF", "M-001", "A-001", "c1", null, Instant.parse("2025-01-01T00:00:00Z")));
        var agent = jwt().authorities(new SimpleGrantedAuthority("ROLE_AGENT")).jwt(j -> j.claim("roles", List.of("AGENT")).claim("actor_type", "AGENT").claim("actor_id", "agt-1"));
        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("roles", List.of("ADMIN")).claim("actor_type", "ADMIN").claim("actor_id", "adm-1"));

        mockMvc.perform(get(ApiPaths.BACKOFFICE_TRANSACTIONS + "/tx-1").with(agent))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(ApiPaths.BACKOFFICE_TRANSACTIONS + "/tx-1").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("tx-1"));
    }

    @Test
    void audit_and_actor_listing_endpoints_return_cursor_page() throws Exception {
        when(auditEventQueryUseCase.list(any())).thenReturn(new QueryPage<>(List.of(
                new BackofficeAuditEventItem("e1", Instant.parse("2025-01-01T00:00:00Z"), "ADMIN", "adm-1", "ACTION", null, null, Map.of("k", "v"))
        ), null, false));
        when(actorQueryUseCase.listAgents(any())).thenReturn(new QueryPage<>(List.of(
                new BackofficeActorItem("a1", "AG001", "ACTIVE", Instant.parse("2025-01-01T00:00:00Z"))
        ), "next-a", true));
        when(actorQueryUseCase.listClients(any())).thenReturn(new QueryPage<>(List.of(), null, false));
        when(actorQueryUseCase.listMerchants(any())).thenReturn(new QueryPage<>(List.of(), null, false));

        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("roles", List.of("ADMIN")).claim("actor_type", "ADMIN").claim("actor_id", "adm-1"));

        mockMvc.perform(get(ApiPaths.BACKOFFICE_AUDIT_EVENTS).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].eventId").value("e1"))
                .andExpect(jsonPath("$.page.hasMore").value(false));

        mockMvc.perform(get(ApiPaths.BACKOFFICE_AGENTS).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].actorId").value("a1"))
                .andExpect(jsonPath("$.page.nextCursor").value("next-a"));

        mockMvc.perform(get(ApiPaths.BACKOFFICE_CLIENTS).with(admin)).andExpect(status().isOk());
        mockMvc.perform(get(ApiPaths.BACKOFFICE_MERCHANTS).with(admin)).andExpect(status().isOk());
    }

    @Test
    void actor_details_and_lookup_endpoints_are_admin_only() throws Exception {
        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("roles", List.of("ADMIN")).claim("actor_type", "ADMIN").claim("actor_id", "adm-1"));
        var agent = jwt().authorities(new SimpleGrantedAuthority("ROLE_AGENT")).jwt(j -> j.claim("roles", List.of("AGENT")).claim("actor_type", "AGENT").claim("actor_id", "agt-1"));

        when(actorDetailQueryUseCase.getAgentById(any())).thenReturn(new BackofficeActorDetails("a1", "AG001", "ACTIVE", Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-01-02T00:00:00Z")));
        when(actorDetailQueryUseCase.getClientById(any())).thenReturn(new BackofficeActorDetails("c1", "+26911111", "ACTIVE", Instant.parse("2025-01-01T00:00:00Z"), null));
        when(actorDetailQueryUseCase.getMerchantById(any())).thenReturn(new BackofficeActorDetails("m1", "M001", "ACTIVE", Instant.parse("2025-01-01T00:00:00Z"), null));
        when(lookupQueryUseCase.search(any())).thenReturn(List.of(new BackofficeLookupItem("AGENT", "a1", "AG001", "ACTIVE", "/api/v1/backoffice/agents/a1")));

        mockMvc.perform(get(ApiPaths.BACKOFFICE + "/agents/11111111-1111-1111-1111-111111111111").with(agent)).andExpect(status().isForbidden());
        mockMvc.perform(get(ApiPaths.BACKOFFICE + "/agents/11111111-1111-1111-1111-111111111111").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.display").value("AG001"));

        mockMvc.perform(get(ApiPaths.BACKOFFICE + "/clients/11111111-1111-1111-1111-111111111111").with(admin)).andExpect(status().isOk());
        mockMvc.perform(get(ApiPaths.BACKOFFICE + "/merchants/11111111-1111-1111-1111-111111111111").with(admin)).andExpect(status().isOk());

        mockMvc.perform(get(ApiPaths.BACKOFFICE + "/lookups").with(admin).param("q", "AG001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].entityType").value("AGENT"));

        when(actorDetailQueryUseCase.getAgentById(any())).thenThrow(new ValidationException("Invalid agentId", Map.of("field", "agentId")));
        mockMvc.perform(get(ApiPaths.BACKOFFICE + "/agents/not-a-uuid").with(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
