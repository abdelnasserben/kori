package com.kori.adapters.in.rest;

import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorContextClaimsExtractor;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class ActorContextArgumentResolver implements HandlerMethodArgumentResolver {

    private final ActorContextClaimsExtractor actorContextClaimsExtractor;
    private final boolean devHeaderFallbackEnabled;

    public ActorContextArgumentResolver(ActorContextClaimsExtractor actorContextClaimsExtractor, boolean devHeaderFallbackEnabled) {
        this.actorContextClaimsExtractor = actorContextClaimsExtractor;
        this.devHeaderFallbackEnabled = devHeaderFallbackEnabled;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ActorContext.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AbstractAuthenticationToken token && token.getPrincipal() instanceof Jwt jwt) {
            try {
                return actorContextClaimsExtractor.extract(jwt.getClaims());
            } catch (IllegalArgumentException ex) {
                // In dev/test fallback mode, allow explicit headers when JWT does not carry actor claims.
                if (!devHeaderFallbackEnabled) {
                    throw ex;
                }
            }
        }

        if (devHeaderFallbackEnabled) {
            String actorType = webRequest.getHeader(RestActorContextResolver.ACTOR_TYPE_HEADER);
            String actorId = webRequest.getHeader(RestActorContextResolver.ACTOR_ID_HEADER);
            if (actorType != null && actorId != null) {
                return RestActorContextResolver.resolve(actorType, actorId);
            }
        }

        throw new IllegalArgumentException("ActorContext cannot be resolved from request");
    }
}
