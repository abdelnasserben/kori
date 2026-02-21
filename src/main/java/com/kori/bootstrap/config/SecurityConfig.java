package com.kori.bootstrap.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kori.adapters.in.rest.error.SecurityAccessDeniedHandler;
import com.kori.adapters.in.rest.error.SecurityAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String API_VERSION = "/api/v1";

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityAuthenticationEntryPoint authenticationEntryPoint,
            SecurityAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health/**"
                        ).permitAll()

                        // Admin endpoints
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/admins").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, API_VERSION + "/admins/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/agents").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, API_VERSION + "/agents/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/merchants").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, API_VERSION + "/merchants/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/terminals").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, API_VERSION + "/terminals/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, API_VERSION + "/clients/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, API_VERSION + "/account-profiles/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, API_VERSION + "/config/fees").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, API_VERSION + "/config/commissions").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, API_VERSION + "/config/platform").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/client-refunds/requests").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/client-refunds/*/complete").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/client-refunds/*/fail").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payments/agent-bank-deposits").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payments/reversals").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payouts/requests").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payouts/*/complete").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payouts/*/fail").hasRole("ADMIN")

                        // Agent endpoints
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/cards/enroll").hasRole("AGENT")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/cards/add").hasRole("AGENT")
                        .requestMatchers(HttpMethod.PATCH, API_VERSION + "/cards/*/status/agent").hasRole("AGENT")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payments/merchant-withdraw").hasRole("AGENT")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payments/cash-in").hasRole("AGENT")

                        // Terminal endpoints
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payments/card").hasRole("TERMINAL")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/terminal/me/**").hasRole("TERMINAL")

                        // Ledger read endpoints
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/ledger/balance").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/ledger/transactions/search").hasRole("ADMIN")

                        // Backoffice endpoints
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/backoffice/transactions/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/backoffice/audit-events").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/backoffice/agents").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/backoffice/clients").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/backoffice/merchants").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/backoffice/actors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/backoffice/lookups").hasRole("ADMIN")

                        // Agent endpoints
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/agent/me/**").hasRole("AGENT")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/agent/search").hasRole("AGENT")

                        // "ME" endpoints
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/client/me/**").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payments/client-transfer").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payments/merchant-transfer").hasRole("MERCHANT")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/merchant/me/**").hasRole("MERCHANT")

                        // Card admin operations
                        .requestMatchers(HttpMethod.PATCH, API_VERSION + "/cards/*/status/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/cards/*/unblock").hasRole("ADMIN")

                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new SecurityAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    SecurityAccessDeniedHandler securityAccessDeniedHandler(ObjectMapper objectMapper) {
        return new SecurityAccessDeniedHandler(objectMapper);
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> merged = new LinkedHashSet<>();
            merged.addAll(extractRealmRoles(jwt)); // <-- Keycloak realm roles: realm_access.roles
            return merged;
        });
        return authenticationConverter;
    }

    private Collection<? extends GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        return toAuthorities(extractRolesFromAccessMap(realmAccess));
    }

    private Collection<String> extractRolesFromAccessMap(Map<String, Object> accessMap) {
        if (accessMap == null) {
            return List.of();
        }

        Object rolesObject = accessMap.get("roles");
        if (!(rolesObject instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    private Collection<? extends GrantedAuthority> toAuthorities(Collection<String> roles) {
        return roles.stream()
                .filter(Objects::nonNull)
                .flatMap(role -> Stream.of(role, role.toUpperCase()))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
