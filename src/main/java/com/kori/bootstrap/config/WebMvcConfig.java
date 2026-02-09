package com.kori.bootstrap.config;

import com.kori.adapters.in.rest.ActorContextArgumentResolver;
import com.kori.application.security.ActorContextClaimsExtractor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ActorContextClaimsExtractor actorContextClaimsExtractor = new ActorContextClaimsExtractor();

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new ActorContextArgumentResolver(actorContextClaimsExtractor));
    }
}
