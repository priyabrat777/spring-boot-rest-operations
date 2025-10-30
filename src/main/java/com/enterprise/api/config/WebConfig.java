package com.enterprise.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.Set;

/**
 * Web configuration for HTTP method validation and CORS settings.
 * Provides additional HTTP method support and validation.
 * 
 * Requirements addressed:
 * - 1.7: OPTIONS and HEAD method handlers
 * - 1.8: HTTP method validation and error handling
 * - 1.4: Proper HTTP status codes and response handling
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configures ObjectMapper with JSR310 module for LocalDateTime serialization.
     * 
     * @return configured ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Configures CORS mappings for all endpoints.
     * This works in conjunction with the SecurityConfig CORS configuration.
     * 
     * @param registry the CORS registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "https://localhost:*")
                .allowedMethods(
                    HttpMethod.GET.name(),
                    HttpMethod.POST.name(),
                    HttpMethod.PUT.name(),
                    HttpMethod.PATCH.name(),
                    HttpMethod.DELETE.name(),
                    HttpMethod.OPTIONS.name(),
                    HttpMethod.HEAD.name()
                )
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Authorization", "Content-Disposition", "X-Total-Count")
                .maxAge(3600);
    }

    /**
     * Adds HTTP method validation interceptor.
     * 
     * @param registry the interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HttpMethodValidationInterceptor())
                .addPathPatterns("/api/**");
    }

    /**
     * Interceptor for HTTP method validation and logging.
     */
    public static class HttpMethodValidationInterceptor implements HandlerInterceptor {

        private static final Logger logger = LoggerFactory.getLogger(HttpMethodValidationInterceptor.class);

        // Define allowed methods for different endpoint patterns
        private static final Set<String> STANDARD_METHODS = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name(),
            HttpMethod.HEAD.name()
        );

        private static final Set<String> READ_ONLY_METHODS = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.OPTIONS.name(),
            HttpMethod.HEAD.name()
        );



        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String method = request.getMethod();
            String uri = request.getRequestURI();

            logger.debug("HTTP {} request to {}", method, uri);

            // Validate HTTP method
            if (!isMethodAllowed(method, uri)) {
                logger.warn("HTTP method {} not allowed for URI: {}", method, uri);
                response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
                response.setHeader("Allow", getAllowedMethods(uri));
                return false;
            }

            // Add security headers for all requests
            addSecurityHeaders(response);

            // Handle preflight OPTIONS requests
            if (HttpMethod.OPTIONS.name().equals(method)) {
                handleOptionsRequest(request, response, uri);
                return false; // Don't continue to controller
            }

            return true;
        }

        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                              Object handler, ModelAndView modelAndView) {
            // Add response headers after processing
            String method = request.getMethod();
            if (!HttpMethod.OPTIONS.name().equals(method) && !HttpMethod.HEAD.name().equals(method)) {
                response.setHeader("X-HTTP-Method-Override-Supported", "true");
            }
        }

        /**
         * Checks if the HTTP method is allowed for the given URI.
         * 
         * @param method the HTTP method
         * @param uri the request URI
         * @return true if method is allowed, false otherwise
         */
        private boolean isMethodAllowed(String method, String uri) {
            // Allow all standard methods for API endpoints
            if (uri.startsWith("/api/")) {
                return STANDARD_METHODS.contains(method);
            }

            // For non-API endpoints, allow only safe methods
            return READ_ONLY_METHODS.contains(method);
        }

        /**
         * Gets the allowed methods for a given URI.
         * 
         * @param uri the request URI
         * @return comma-separated list of allowed methods
         */
        private String getAllowedMethods(String uri) {
            if (uri.startsWith("/api/")) {
                // Different endpoints support different methods
                if (uri.matches(".*/\\d+$")) {
                    // Resource by ID endpoints
                    return "GET, PUT, PATCH, DELETE, OPTIONS, HEAD";
                } else if (uri.endsWith("/search") || uri.matches(".*/name/.*") || uri.matches(".*/username/.*")) {
                    // Search and lookup endpoints
                    return "GET, OPTIONS, HEAD";
                } else if (uri.matches(".*/\\d+/(status|lock|password|roles|permissions)$")) {
                    // Sub-resource modification endpoints
                    return "PATCH, DELETE, OPTIONS, HEAD";
                } else {
                    // Collection endpoints
                    return "GET, POST, OPTIONS, HEAD";
                }
            }
            
            return String.join(", ", READ_ONLY_METHODS);
        }

        /**
         * Handles OPTIONS preflight requests.
         * 
         * @param request the HTTP request
         * @param response the HTTP response
         * @param uri the request URI
         */
        private void handleOptionsRequest(HttpServletRequest request, HttpServletResponse response, String uri) {
            response.setStatus(HttpStatus.OK.value());
            response.setHeader("Allow", getAllowedMethods(uri));
            response.setHeader("Access-Control-Allow-Methods", getAllowedMethods(uri));
            response.setHeader("Access-Control-Allow-Headers", 
                "Authorization, Content-Type, X-Requested-With, Accept, Origin, X-HTTP-Method-Override");
            response.setHeader("Access-Control-Max-Age", "3600");
            
            logger.debug("Handled OPTIONS request for URI: {}", uri);
        }

        /**
         * Adds security headers to the response.
         * 
         * @param response the HTTP response
         */
        private void addSecurityHeaders(HttpServletResponse response) {
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("X-XSS-Protection", "1; mode=block");
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        }
    }
}