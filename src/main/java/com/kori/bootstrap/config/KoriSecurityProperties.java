package com.kori.bootstrap.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "kori.security")
public class KoriSecurityProperties {

    private final Keycloak keycloak = new Keycloak();

    @Setter @Getter
    public static class Keycloak {
        private String clientId = "kori-api";

    }
}
