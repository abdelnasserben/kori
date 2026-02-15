package com.kori.adapters.in.rest;

import com.kori.application.exception.ValidationException;
import com.kori.query.model.QueryPage;
import com.kori.query.model.me.AgentQueryModels;
import com.kori.query.port.in.AgentMeQueryUseCase;
import com.kori.query.port.in.AgentSearchUseCase;
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
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentQueryControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentMeQueryUseCase agentMeQueryUseCase;

    @MockitoBean
    private AgentSearchUseCase agentSearchUseCase;

    @Test
    void agent_endpoints_are_agent_only() throws Exception {
        when(agentMeQueryUseCase.getSummary(any())).thenReturn(new AgentQueryModels.AgentSummary(
                "A-000001", "AG001", "ACTIVE", new BigDecimal("1200.00"), new BigDecimal("300.00"), 4L));

        var agent = jwt().authorities(new SimpleGrantedAuthority("ROLE_AGENT")).jwt(j -> j.claim("roles", List.of("AGENT")).claim("actor_type", "AGENT").claim("actor_id", "A-000001"));
        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("roles", List.of("ADMIN")).claim("actor_type", "ADMIN").claim("actor_id", "admin-1"));

        mockMvc.perform(get(ApiPaths.AGENT_ME + "/summary"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(ApiPaths.AGENT_ME + "/summary").with(admin))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(ApiPaths.AGENT_ME + "/summary").with(agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value("A-000001"));
    }

    @Test
    void transactions_and_activities_return_cursor_page_shape() throws Exception {
        when(agentMeQueryUseCase.listTransactions(any(), any())).thenReturn(new QueryPage<>(List.of(
                new AgentQueryModels.AgentTransactionItem("tx-1", "CASH_IN", "COMPLETED", new BigDecimal("100.00"), "KMF", Instant.parse("2025-01-01T00:00:00Z"))
        ), "cursor-2", true));

        when(agentMeQueryUseCase.listActivities(any(), any())).thenReturn(new QueryPage<>(List.of(
                new AgentQueryModels.AgentActivityItem("evt-1", Instant.parse("2025-01-02T00:00:00Z"), "LOGIN", "AGENT", "A-000001", Map.of("ip", "127.0.0.1"))
        ), null, false));

        var agent = jwt().authorities(new SimpleGrantedAuthority("ROLE_AGENT")).jwt(j -> j.claim("roles", List.of("AGENT")).claim("actor_type", "AGENT").claim("actor_id", "A-000001"));

        mockMvc.perform(get(ApiPaths.AGENT_ME + "/transactions").with(agent).param("limit", "1").param("sort", "createdAt:desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].transactionId").value("tx-1"))
                .andExpect(jsonPath("$.page.nextCursor").value("cursor-2"))
                .andExpect(jsonPath("$.page.hasMore").value(true));

        mockMvc.perform(get(ApiPaths.AGENT_ME + "/activities").with(agent).param("sort", "occurredAt:desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].eventId").value("evt-1"))
                .andExpect(jsonPath("$.page.nextCursor", nullValue()))
                .andExpect(jsonPath("$.page.hasMore").value(false));
    }

    @Test
    void search_requires_agent_role_and_valid_lookup_inputs() throws Exception {
        when(agentSearchUseCase.search(any(), any())).thenReturn(List.of(
                new AgentQueryModels.AgentSearchItem("CLIENT", "client-1", "26900001", "ACTIVE", Map.of("client", "/api/v1/clients/client-1"))
        ));

        var agent = jwt().authorities(new SimpleGrantedAuthority("ROLE_AGENT")).jwt(j -> j.claim("roles", List.of("AGENT")).claim("actor_type", "AGENT").claim("actor_id", "A-000001"));
        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(j -> j.claim("roles", List.of("ADMIN")).claim("actor_type", "ADMIN").claim("actor_id", "admin-1"));

        mockMvc.perform(get(ApiPaths.AGENT_SEARCH).with(admin).param("phone", "26900001"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(ApiPaths.AGENT_SEARCH).with(agent).param("phone", "26900001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].entityType").value("CLIENT"));

        when(agentSearchUseCase.search(any(), any())).thenThrow(new ValidationException("One lookup parameter is required"));
        mockMvc.perform(get(ApiPaths.AGENT_SEARCH).with(agent))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void validation_errors_are_mapped_for_invalid_sort_limit_and_cursor() throws Exception {
        when(agentMeQueryUseCase.listTransactions(any(), any()))
                .thenThrow(new ValidationException("Invalid sort format. Use <field>:<asc|desc>"));

        var agent = jwt().authorities(new SimpleGrantedAuthority("ROLE_AGENT")).jwt(j -> j.claim("roles", List.of("AGENT")).claim("actor_type", "AGENT").claim("actor_id", "A-000001"));

        mockMvc.perform(get(ApiPaths.AGENT_ME + "/transactions").with(agent).param("sort", "createdAt:wrong"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
