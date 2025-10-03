package com.enterprise.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for comprehensive API documentation.
 * Configures Swagger UI with JWT authentication support and detailed API information.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${spring.application.name:Enterprise Spring Boot API}")
    private String applicationName;

    private static final String JWT_SECURITY_SCHEME = "bearerAuth";
    private static final String API_KEY_SECURITY_SCHEME = "apiKeyAuth";

    /**
     * Configures OpenAPI specification with security schemes and API information.
     *
     * @return OpenAPI configuration
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Development server"),
                        new Server()
                                .url("https://api.enterprise.com")
                                .description("Production server")
                ))
                .components(new Components()
                        .addSecuritySchemes(JWT_SECURITY_SCHEME, jwtSecurityScheme())
                        .addSecuritySchemes(API_KEY_SECURITY_SCHEME, apiKeySecurityScheme())
                )
                .addSecurityItem(new SecurityRequirement()
                        .addList(JWT_SECURITY_SCHEME)
                        .addList(API_KEY_SECURITY_SCHEME)
                );
    }

    /**
     * Creates API information including title, description, version, and contact details.
     *
     * @return API info configuration
     */
    private Info apiInfo() {
        return new Info()
                .title("Enterprise Spring Boot API")
                .description("""
                        Comprehensive Spring Boot REST API demonstrating enterprise-level development practices.
                        
                        ## Features
                        - JWT-based authentication and RBAC authorization
                        - Complete CRUD operations with audit trails
                        - File upload and management
                        - CAPTCHA and OTP functionality
                        - Spring Batch job processing
                        - Comprehensive error handling
                        
                        ## Authentication
                        This API uses JWT Bearer tokens for authentication. To access protected endpoints:
                        1. Login using the `/api/auth/login` endpoint
                        2. Copy the JWT token from the response
                        3. Click the 'Authorize' button and enter: `Bearer <your-jwt-token>`
                        
                        ## Rate Limiting
                        Some endpoints have rate limiting applied. Check response headers for rate limit information.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Enterprise API Team")
                        .email("api-team@enterprise.com")
                        .url("https://enterprise.com/api-docs")
                )
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT")
                );
    }

    /**
     * Configures JWT Bearer token security scheme.
     *
     * @return JWT security scheme configuration
     */
    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Bearer token authentication. Format: Bearer <token>");
    }

    /**
     * Configures API Key security scheme for alternative authentication.
     *
     * @return API Key security scheme configuration
     */
    private SecurityScheme apiKeySecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("API Key authentication for service-to-service communication");
    }
}