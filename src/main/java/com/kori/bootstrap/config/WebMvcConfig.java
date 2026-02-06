package com.kori.bootstrap.config;

import com.kori.adapters.in.rest.ActorContextArgumentResolver;
import com.kori.application.security.ActorContextClaimsExtractor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableConfigurationProperties(KoriSecurityProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final ActorContextClaimsExtractor actorContextClaimsExtractor = new ActorContextClaimsExtractor();
    private final KoriSecurityProperties securityProperties;

    public WebMvcConfig(KoriSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        boolean devHeaderFallbackEnabled = securityProperties.getActorContext().isDevHeaderFallbackEnabled();
        resolvers.add(new ActorContextArgumentResolver(actorContextClaimsExtractor, devHeaderFallbackEnabled));
    }
}
