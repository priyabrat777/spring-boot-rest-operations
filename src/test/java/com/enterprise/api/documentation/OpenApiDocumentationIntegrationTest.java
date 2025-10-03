package com.enterprise.api.documentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * Integration tests for OpenAPI documentation generation.
 * Validates that the OpenAPI specification is correctly generated and accessible.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGenerateOpenApiSpecification() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode openApiSpec = objectMapper.readTree(content);

        // Validate OpenAPI version
        assertThat(openApiSpec.get("openapi").asText()).startsWith("3.0");

        // Validate info section
        JsonNode info = openApiSpec.get("info");
        assertThat(info.get("title").asText()).isEqualTo("Enterprise Spring Boot API");
        assertThat(info.get("version").asText()).isEqualTo("1.0.0");
        assertThat(info.get("description").asText()).contains("Comprehensive Spring Boot REST API");

        // Validate security schemes
        JsonNode components = openApiSpec.get("components");
        JsonNode securitySchemes = components.get("securitySchemes");
        assertThat(securitySchemes.has("bearerAuth")).isTrue();
        assertThat(securitySchemes.has("apiKeyAuth")).isTrue();

        // Validate JWT security scheme
        JsonNode bearerAuth = securitySchemes.get("bearerAuth");
        assertThat(bearerAuth.get("type").asText()).isEqualTo("http");
        assertThat(bearerAuth.get("scheme").asText()).isEqualTo("bearer");
        assertThat(bearerAuth.get("bearerFormat").asText()).isEqualTo("JWT");

        // Validate API Key security scheme
        JsonNode apiKeyAuth = securitySchemes.get("apiKeyAuth");
        assertThat(apiKeyAuth.get("type").asText()).isEqualTo("apiKey");
        assertThat(apiKeyAuth.get("in").asText()).isEqualTo("header");
        assertThat(apiKeyAuth.get("name").asText()).isEqualTo("X-API-Key");
    }

    @Test
    void shouldIncludeAuthenticationEndpoints() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode openApiSpec = objectMapper.readTree(content);
        JsonNode paths = openApiSpec.get("paths");

        // Validate authentication endpoints
        assertThat(paths.has("/api/v1/auth/login")).isTrue();
        assertThat(paths.has("/api/v1/auth/logout")).isTrue();
        assertThat(paths.has("/api/v1/auth/refresh")).isTrue();
        assertThat(paths.has("/api/v1/auth/validate")).isTrue();
        assertThat(paths.has("/api/v1/auth/me")).isTrue();

        // Validate login endpoint details
        JsonNode loginEndpoint = paths.get("/api/v1/auth/login").get("post");
        assertThat(loginEndpoint.get("summary").asText()).isEqualTo("User Login");
        assertThat(loginEndpoint.get("tags").get(0).asText()).isEqualTo("Authentication");
        
        // Validate response codes
        JsonNode responses = loginEndpoint.get("responses");
        assertThat(responses.has("200")).isTrue();
        assertThat(responses.has("401")).isTrue();
        assertThat(responses.has("400")).isTrue();
    }

    @Test
    void shouldIncludeUserManagementEndpoints() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode openApiSpec = objectMapper.readTree(content);
        JsonNode paths = openApiSpec.get("paths");

        // Validate user management endpoints
        assertThat(paths.has("/api/v1/users")).isTrue();
        assertThat(paths.has("/api/v1/users/{id}")).isTrue();

        // Validate user endpoints have proper HTTP methods
        JsonNode usersEndpoint = paths.get("/api/v1/users");
        assertThat(usersEndpoint.has("get")).isTrue(); // List users
        assertThat(usersEndpoint.has("post")).isTrue(); // Create user

        JsonNode userByIdEndpoint = paths.get("/api/v1/users/{id}");
        assertThat(userByIdEndpoint.has("get")).isTrue(); // Get user
        assertThat(userByIdEndpoint.has("put")).isTrue(); // Update user
        assertThat(userByIdEndpoint.has("delete")).isTrue(); // Delete user
        assertThat(userByIdEndpoint.has("patch")).isTrue(); // Partial update
    }

    @Test
    void shouldIncludeFileManagementEndpoints() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode openApiSpec = objectMapper.readTree(content);
        JsonNode paths = openApiSpec.get("paths");

        // Validate file management endpoints
        assertThat(paths.has("/api/files/upload")).isTrue();
        assertThat(paths.has("/api/files/{fileId}")).isTrue();
        assertThat(paths.has("/api/files")).isTrue();

        // Validate file upload endpoint
        JsonNode uploadEndpoint = paths.get("/api/files/upload").get("post");
        assertThat(uploadEndpoint.get("tags").get(0).asText()).isEqualTo("File Management");
        assertThat(uploadEndpoint.get("requestBody")).isNotNull();
    }

    @Test
    void shouldIncludeSecurityFeatureEndpoints() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode openApiSpec = objectMapper.readTree(content);
        JsonNode paths = openApiSpec.get("paths");

        // Validate CAPTCHA endpoints
        assertThat(paths.has("/api/captcha/generate")).isTrue();
        assertThat(paths.has("/api/captcha/validate")).isTrue();

        // Validate OTP endpoints
        assertThat(paths.has("/api/v1/otp/generate")).isTrue();
        assertThat(paths.has("/api/v1/otp/validate")).isTrue();

        // Validate CAPTCHA generate endpoint
        JsonNode captchaEndpoint = paths.get("/api/captcha/generate").get("get");
        assertThat(captchaEndpoint.get("tags").get(0).asText()).isEqualTo("CAPTCHA");
    }

    @Test
    void shouldIncludeBatchProcessingEndpoints() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode openApiSpec = objectMapper.readTree(content);
        JsonNode paths = openApiSpec.get("paths");

        // Validate batch processing endpoints
        assertThat(paths.has("/api/v1/batch/jobs")).isTrue();
        assertThat(paths.has("/api/v1/batch/jobs/{jobName}/start")).isTrue();
        assertThat(paths.has("/api/v1/batch/jobs/{jobName}/stop")).isTrue();
        assertThat(paths.has("/api/v1/batch/executions/{executionId}")).isTrue();

        // Validate batch job endpoint
        JsonNode batchJobsEndpoint = paths.get("/api/v1/batch/jobs").get("get");
        assertThat(batchJobsEndpoint.get("tags").get(0).asText()).isEqualTo("Batch Processing");
    }

    @Test
    void shouldIncludeRoleManagementEndpoints() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode openApiSpec = objectMapper.readTree(content);
        JsonNode paths = openApiSpec.get("paths");

        // Validate role management endpoints
        assertThat(paths.has("/api/v1/roles")).isTrue();
        assertThat(paths.has("/api/v1/roles/{id}")).isTrue();

        // Validate role endpoints have proper HTTP methods
        JsonNode rolesEndpoint = paths.get("/api/v1/roles");
        assertThat(rolesEndpoint.has("get")).isTrue(); // List roles
        assertThat(rolesEndpoint.has("post")).isTrue(); // Create role

        JsonNode roleByIdEndpoint = paths.get("/api/v1/roles/{id}");
        assertThat(roleByIdEndpoint.has("get")).isTrue(); // Get role
        assertThat(roleByIdEndpoint.has("put")).isTrue(); // Update role
        assertThat(roleByIdEndpoint.has("delete")).isTrue(); // Delete role
    }

    @Test
    void shouldIncludeExampleRequestsAndResponses() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode openApiSpec = objectMapper.readTree(content);
        JsonNode paths = openApiSpec.get("paths");

        // Validate login endpoint has examples
        JsonNode loginEndpoint = paths.get("/api/v1/auth/login").get("post");
        JsonNode responses = loginEndpoint.get("responses");
        JsonNode successResponse = responses.get("200");
        JsonNode successContent = successResponse.get("content").get("application/json");
        
        assertThat(successContent.has("schema")).isTrue();
        assertThat(successContent.has("examples")).isTrue();
        
        JsonNode example = successContent.get("examples").get("Successful Login");
        assertThat(example.get("value").asText()).contains("accessToken");
        assertThat(example.get("value").asText()).contains("refreshToken");
    }

    @Test
    void shouldAccessSwaggerUI() throws Exception {
        // When & Then - swagger-ui.html should redirect to swagger-ui/index.html
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));

        // The actual Swagger UI page should be accessible
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldValidateSecurityRequirements() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode openApiSpec = objectMapper.readTree(content);
        JsonNode paths = openApiSpec.get("paths");

        // Validate protected endpoints have security requirements
        JsonNode logoutEndpoint = paths.get("/api/v1/auth/logout").get("post");
        assertThat(logoutEndpoint.has("security")).isTrue();
        
        JsonNode security = logoutEndpoint.get("security").get(0);
        assertThat(security.has("bearerAuth")).isTrue();

        // Validate public endpoints don't require authentication
        JsonNode loginEndpoint = paths.get("/api/v1/auth/login").get("post");
        // Login endpoint should not have security requirements or should be empty
        if (loginEndpoint.has("security")) {
            assertThat(loginEndpoint.get("security").isEmpty()).isTrue();
        }
    }
}