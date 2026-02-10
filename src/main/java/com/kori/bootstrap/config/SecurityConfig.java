package com.kori.bootstrap.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kori.adapters.in.rest.error.SecurityAccessDeniedHandler;
import com.kori.adapters.in.rest.error.SecurityAuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(KoriSecurityProperties.class)
public class SecurityConfig {

    private static final String API_VERSION = "/api/v1";

    private final KoriSecurityProperties securityProperties;

    public SecurityConfig(KoriSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityAuthenticationEntryPoint authenticationEntryPoint,
            SecurityAccessDeniedHandler accessDeniedHandler) throws Exception {
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
                        .requestMatchers("/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health/**").permitAll()

                        // Admin controllers
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
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/client-refunds/requests").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/client-refunds/*/complete").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/client-refunds/*/fail").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payments/agent-bank-deposits").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payments/reversals").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payouts/requests").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payouts/*/complete").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payouts/*/fail").hasRole("ADMIN")

                        // Agent-only endpoints
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/cards/enroll").hasRole("AGENT")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/cards/add").hasRole("AGENT")
                        .requestMatchers(HttpMethod.PATCH, API_VERSION + "/cards/*/status/agent").hasRole("AGENT")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payments/merchant-withdraw").hasRole("AGENT")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payments/cash-in").hasRole("AGENT")

                        // Terminal-only endpoint
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/payments/card").hasRole("TERMINAL")

                        // Mixed ledger read endpoints
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/ledger/balance").hasAnyRole("ADMIN", "AGENT", "MERCHANT", "CLIENT")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/ledger/transactions/search").hasAnyRole("ADMIN", "AGENT", "MERCHANT", "CLIENT")

                        // Backoffice query endpoints
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/backoffice/transactions/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/backoffice/audit-events").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/backoffice/agents/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/backoffice/clients/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/backoffice/merchants/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/backoffice/lookups").hasRole("ADMIN")

                        // Agent query endpoints
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/agent/me/**").hasRole("AGENT")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/agent/search").hasRole("AGENT")

                        // "ME" query endpoints
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/client/me/**").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.GET, API_VERSION + "/merchant/me/**").hasRole("MERCHANT")

                        // Card admin operations
                        .requestMatchers(HttpMethod.PATCH, API_VERSION + "/cards/*/status/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, API_VERSION + "/cards/*/unblock").hasRole("ADMIN")

                        .anyRequest().denyAll())
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
        var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        var authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> directAuthorities = authoritiesConverter.convert(jwt);
            Set<GrantedAuthority> mergedAuthorities = new LinkedHashSet<>();
            if (directAuthorities != null) {
                mergedAuthorities.addAll(directAuthorities);
            }
            mergedAuthorities.addAll(extractRealmRoles(jwt));
            mergedAuthorities.addAll(extractResourceRoles(jwt, securityProperties.getKeycloak().getClientId()));

            return mergedAuthorities;
        });

        return authenticationConverter;
    }

    private Collection<? extends GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        return toAuthorities(extractRolesFromAccessMap(realmAccess));
    }

    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt, String clientId) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            return List.of();
        }

        Object clientAccess = resourceAccess.get(clientId);
        if (!(clientAccess instanceof Map<?, ?> clientAccessMap)) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> typedClientAccess = (Map<String, Object>) clientAccessMap;
        return toAuthorities(extractRolesFromAccessMap(typedClientAccess));
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
