package com.enterprise.api.controller;

import com.enterprise.api.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for advanced HTTP method support across all controllers.
 * Tests OPTIONS, HEAD, PATCH operations and HTTP method validation.
 * 
 * Requirements addressed:
 * - 1.7: OPTIONS and HEAD method handlers
 * - 1.8: HTTP method validation and error handling
 * - 1.4: Proper HTTP status codes and response handling
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class HttpMethodIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Create test tokens
        adminToken = createTestToken("admin", List.of("ROLE_ADMIN"));
        userToken = createTestToken("user", List.of("ROLE_USER"));
    }

    @Test
    @DisplayName("OPTIONS requests should return allowed methods for auth endpoints")
    void testOptionsRequestsForAuthEndpoints() throws Exception {
        // Test login endpoint
        mockMvc.perform(options("/api/v1/auth/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "POST, OPTIONS, HEAD"))
                .andExpect(header().string("Access-Control-Allow-Methods", "POST, OPTIONS, HEAD"))
                .andExpect(header().exists("Access-Control-Allow-Headers"))
                .andExpect(header().string("Access-Control-Max-Age", "3600"));

        // Test validate endpoint
        mockMvc.perform(options("/api/v1/auth/validate"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "GET, OPTIONS, HEAD"));

        // Test password endpoint
        mockMvc.perform(options("/api/v1/auth/password"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "PATCH, OPTIONS, HEAD"));
    }

    @Test
    @DisplayName("HEAD requests should return headers without body for auth endpoints")
    void testHeadRequestsForAuthEndpoints() throws Exception {
        // Test validate endpoint with authentication
        mockMvc.perform(head("/api/v1/auth/validate")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(header().string("X-Endpoint-Type", "authentication-info"))
                .andExpect(content().string(""));

        // Test login endpoint
        mockMvc.perform(head("/api/v1/auth/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(header().string("X-Endpoint-Type", "authentication-action"))
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("PATCH requests should work for password changes")
    void testPatchRequestsForPasswordChange() throws Exception {
        Map<String, String> passwordRequest = Map.of(
                "currentPassword", "oldPassword123",
                "newPassword", "newPassword123"
        );

        mockMvc.perform(patch("/api/v1/auth/password")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists());
    }

    @Test
    @DisplayName("PATCH requests should work for profile updates")
    void testPatchRequestsForProfileUpdate() throws Exception {
        Map<String, Object> profileRequest = Map.of(
                "firstName", "Updated",
                "lastName", "Name"
        );

        mockMvc.perform(patch("/api/v1/auth/profile")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists());
    }

    @Test
    @DisplayName("OPTIONS requests should return allowed methods for user endpoints")
    void testOptionsRequestsForUserEndpoints() throws Exception {
        // Test users collection endpoint
        mockMvc.perform(options("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "GET, POST, OPTIONS, HEAD"));

        // Test specific user endpoint
        mockMvc.perform(options("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "GET, PUT, PATCH, DELETE, OPTIONS, HEAD"));

        // Test user status endpoint
        mockMvc.perform(options("/api/v1/users/1/status"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "PATCH, DELETE, OPTIONS, HEAD"));

        // Test search endpoint
        mockMvc.perform(options("/api/v1/users/search"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "GET, OPTIONS, HEAD"));
    }

    @Test
    @DisplayName("HEAD requests should return headers without body for user endpoints")
    void testHeadRequestsForUserEndpoints() throws Exception {
        // Test users collection
        mockMvc.perform(head("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(header().string("X-Resource-Type", "user-collection"))
                .andExpect(header().string("X-Supports-Pagination", "true"))
                .andExpect(content().string(""));

        // Test specific user
        mockMvc.perform(head("/api/v1/users/1")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Resource-Type", "user"))
                .andExpect(header().string("X-Supports-Partial-Update", "true"));

        // Test search endpoint
        mockMvc.perform(head("/api/v1/users/search")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Resource-Type", "user-search"))
                .andExpect(header().string("X-Supports-Pagination", "true"));
    }

    @Test
    @DisplayName("PATCH requests should work for partial user updates")
    void testPatchRequestsForUserUpdates() throws Exception {
        Map<String, Object> partialUpdate = Map.of(
                "firstName", "Updated",
                "enabled", true
        );

        mockMvc.perform(patch("/api/v1/users/1")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("OPTIONS requests should return allowed methods for role endpoints")
    void testOptionsRequestsForRoleEndpoints() throws Exception {
        // Test roles collection endpoint
        mockMvc.perform(options("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "GET, POST, OPTIONS, HEAD"));

        // Test specific role endpoint
        mockMvc.perform(options("/api/v1/roles/1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "GET, PUT, PATCH, DELETE, OPTIONS, HEAD"));

        // Test role permissions endpoint
        mockMvc.perform(options("/api/v1/roles/1/permissions"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "PATCH, DELETE, OPTIONS, HEAD"));
    }

    @Test
    @DisplayName("HEAD requests should return headers without body for role endpoints")
    void testHeadRequestsForRoleEndpoints() throws Exception {
        // Test roles collection
        mockMvc.perform(head("/api/v1/roles")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(header().string("X-Resource-Type", "role-collection"))
                .andExpect(header().string("X-Supports-Pagination", "true"))
                .andExpect(content().string(""));

        // Test specific role
        mockMvc.perform(head("/api/v1/roles/1")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Resource-Type", "role"))
                .andExpect(header().string("X-Supports-Partial-Update", "true"));
    }

    @Test
    @DisplayName("PATCH requests should work for partial role updates")
    void testPatchRequestsForRoleUpdates() throws Exception {
        Map<String, Object> partialUpdate = Map.of(
                "description", "Updated description",
                "enabled", true
        );

        mockMvc.perform(patch("/api/v1/roles/1")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Invalid HTTP methods should return 405 Method Not Allowed")
    void testInvalidHttpMethods() throws Exception {
        // Test invalid method on auth endpoint
        mockMvc.perform(put("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"));

        // Test invalid method on user endpoint
        mockMvc.perform(post("/api/v1/users/1")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"));
    }

    @Test
    @DisplayName("CORS headers should be present in all responses")
    void testCorsHeaders() throws Exception {
        // Test preflight OPTIONS request
        mockMvc.perform(options("/api/v1/users")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().exists("Access-Control-Allow-Headers"))
                .andExpect(header().string("Access-Control-Max-Age", "3600"));

        // Test actual request with CORS headers
        mockMvc.perform(get("/api/v1/auth/validate")
                .header("Authorization", "Bearer " + adminToken)
                .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security headers should be present in all responses")
    void testSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/auth/validate")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    @DisplayName("HTTP method override should be supported")
    void testHttpMethodOverride() throws Exception {
        mockMvc.perform(post("/api/v1/users/1")
                .header("Authorization", "Bearer " + adminToken)
                .header("X-HTTP-Method-Override", "PATCH")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\": \"Override Test\"}"))
                .andExpect(header().string("X-HTTP-Method-Override-Supported", "true"));
    }

    /**
     * Creates a test JWT token for the given username and authorities.
     * 
     * @param username the username
     * @param authorities the authorities
     * @return JWT token
     */
    private String createTestToken(String username, List<String> authorities) {
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                        username, 
                        null, 
                        authorities.stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList()
                );
        
        return jwtTokenProvider.generateToken(authentication);
    }
}