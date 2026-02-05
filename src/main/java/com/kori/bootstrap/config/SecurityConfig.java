package com.kori.bootstrap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health/**").permitAll()

                        // Admin controllers
                        .requestMatchers(HttpMethod.POST, "/api/v1/admins").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/admins/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/agents").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/agents/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/merchants").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/merchants/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/terminals").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/terminals/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/clients/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/account-profiles/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/config/fees").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/config/commissions").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/client-refunds/requests").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/client-refunds/*/complete").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/client-refunds/*/fail").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/agent-bank-deposits").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/reversals").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payouts/requests").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payouts/*/complete").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payouts/*/fail").hasRole("ADMIN")

                        // Agent-only endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/cards/enroll").hasRole("AGENT")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/cards/*/status/agent").hasRole("AGENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/merchant-withdraw").hasRole("AGENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/cash-in").hasRole("AGENT")

                        // Terminal-only endpoint
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/card").hasRole("TERMINAL")

                        // Mixed ledger read endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/ledger/balance").hasAnyRole("ADMIN", "AGENT", "MERCHANT", "CLIENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/ledger/transactions/search").hasAnyRole("ADMIN", "AGENT", "MERCHANT", "CLIENT")

                        // Card admin operations
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/cards/*/status/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/cards/*/unblock").hasRole("ADMIN")

                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.security.jwt-secret}") String jwtSecret) {
        var key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        var authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }
}
