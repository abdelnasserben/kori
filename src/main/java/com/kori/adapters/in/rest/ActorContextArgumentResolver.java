package com.kori.adapters.in.rest;

import com.kori.application.exception.ActorContextAuthenticationException;
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

    public ActorContextArgumentResolver(ActorContextClaimsExtractor actorContextClaimsExtractor) {
        this.actorContextClaimsExtractor = actorContextClaimsExtractor;
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
            return actorContextClaimsExtractor.extract(jwt.getClaims());
        }

        throw new ActorContextAuthenticationException("Authentication required");
    }
}
