package com.enterprise.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OpenAPI configuration.
 * Validates OpenAPI specification generation and security scheme configuration.
 */
@SpringBootTest
@ActiveProfiles("test")
class OpenApiConfigTest {

    @Autowired
    private OpenApiConfig openApiConfig;

    @Test
    void shouldCreateOpenAPIConfiguration() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        // Then
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getSecurity()).isNotNull();
        assertThat(openAPI.getServers()).isNotNull();
    }

    @Test
    void shouldConfigureAPIInfo() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        Info info = openAPI.getInfo();

        // Then
        assertThat(info.getTitle()).isEqualTo("Enterprise Spring Boot API");
        assertThat(info.getVersion()).isEqualTo("1.0.0");
        assertThat(info.getDescription()).contains("Comprehensive Spring Boot REST API");
        assertThat(info.getContact()).isNotNull();
        assertThat(info.getContact().getName()).isEqualTo("Enterprise API Team");
        assertThat(info.getContact().getEmail()).isEqualTo("api-team@enterprise.com");
        assertThat(info.getLicense()).isNotNull();
        assertThat(info.getLicense().getName()).isEqualTo("MIT License");
    }

    @Test
    void shouldConfigureSecuritySchemes() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        // Then
        assertThat(openAPI.getComponents().getSecuritySchemes()).hasSize(2);
        
        // JWT Security Scheme
        SecurityScheme jwtScheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(jwtScheme).isNotNull();
        assertThat(jwtScheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(jwtScheme.getScheme()).isEqualTo("bearer");
        assertThat(jwtScheme.getBearerFormat()).isEqualTo("JWT");
        assertThat(jwtScheme.getDescription()).contains("JWT Bearer token authentication");

        // API Key Security Scheme
        SecurityScheme apiKeyScheme = openAPI.getComponents().getSecuritySchemes().get("apiKeyAuth");
        assertThat(apiKeyScheme).isNotNull();
        assertThat(apiKeyScheme.getType()).isEqualTo(SecurityScheme.Type.APIKEY);
        assertThat(apiKeyScheme.getIn()).isEqualTo(SecurityScheme.In.HEADER);
        assertThat(apiKeyScheme.getName()).isEqualTo("X-API-Key");
        assertThat(apiKeyScheme.getDescription()).contains("API Key authentication");
    }

    @Test
    void shouldConfigureGlobalSecurity() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        // Then
        assertThat(openAPI.getSecurity()).hasSize(1);
        SecurityRequirement securityRequirement = openAPI.getSecurity().get(0);
        assertThat(securityRequirement.keySet()).contains("bearerAuth", "apiKeyAuth");
    }

    @Test
    void shouldConfigureServers() {
        // When
        OpenAPI openAPI = openApiConfig.customOpenAPI();

        // Then
        assertThat(openAPI.getServers()).hasSize(2);
        assertThat(openAPI.getServers().get(0).getUrl()).contains("localhost");
        assertThat(openAPI.getServers().get(0).getDescription()).isEqualTo("Development server");
        assertThat(openAPI.getServers().get(1).getUrl()).isEqualTo("https://api.enterprise.com");
        assertThat(openAPI.getServers().get(1).getDescription()).isEqualTo("Production server");
    }
}