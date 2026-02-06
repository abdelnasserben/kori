package com.kori.bootstrap.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "kori.security")
public class KoriSecurityProperties {

    private final ActorContext actorContext = new ActorContext();
    private final Jwt jwt = new Jwt();
    private final Keycloak keycloak = new Keycloak();

    @Setter @Getter
    public static class ActorContext {
        private boolean devHeaderFallbackEnabled;
    }

    @Setter @Getter
    public static class Jwt {
        private String mode = "issuer";
        private String hmacSecret;

    }

    @Setter @Getter
    public static class Keycloak {
        private String clientId = "kori-api";

    }
}
