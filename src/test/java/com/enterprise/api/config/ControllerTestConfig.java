package com.enterprise.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Test configuration for controller tests that provides Authentication parameter injection
 * when security filters are disabled.
 */
@TestConfiguration
public class ControllerTestConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthenticationArgumentResolver());
    }

    /**
     * Custom argument resolver that injects Authentication from SecurityContext
     * when security filters are disabled in tests.
     */
    public static class AuthenticationArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return Authentication.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            // Try to get authentication from SecurityContext first
            SecurityContext securityContext = SecurityContextHolder.getContext();
            Authentication auth = securityContext.getAuthentication();
            
            if (auth != null) {
                return auth;
            }
            
            // Fallback to a default admin authentication for tests
            return new UsernamePasswordAuthenticationToken(
                "testuser", 
                null, 
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
        }
    }
}