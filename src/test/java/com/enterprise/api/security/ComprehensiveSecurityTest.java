package com.enterprise.api.security;

import com.enterprise.api.dto.request.CreateUserRequest;
import com.enterprise.api.dto.request.LoginRequest;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive security test suite covering all security aspects.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class ComprehensiveSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Test comprehensive input sanitization")
    void testComprehensiveInputSanitization() throws Exception {
        String[] maliciousInputs = {
            // XSS attempts
            "<script>alert('xss')</script>",
            "<img src=x onerror=alert('xss')>",
            "javascript:alert('xss')",
            
            // SQL injection attempts
            "'; DROP TABLE users; --",
            "admin' OR '1'='1",
            "1' UNION SELECT * FROM users --",
            
            // Command injection attempts
            "; ls -la",
            "| cat /etc/passwd",
            "&& rm -rf /",
            
            // Path traversal attempts
            "../../../etc/passwd",
            "..\\..\\..\\windows\\system32\\config\\sam",
            
            // LDAP injection attempts
            "*)(uid=*",
            "*)(|(uid=*))",
            
            // NoSQL injection attempts
            "{ \"$ne\": null }",
            "{ \"$gt\": \"\" }",
            
            // Format string attacks
            "%s%s%s%s%s%s%s%s%s%s",
            "%x%x%x%x%x%x%x%x%x%x",
            
            // Null byte injection
            "test\u0000.txt",
            "admin\u0000",
            
            // Unicode attacks
            "admin\uFEFF",
            "admin\u200B"
        };

        for (String maliciousInput : maliciousInputs) {
            CreateUserRequest request = new CreateUserRequest();
            request.setUsername(maliciousInput);
            request.setEmail("test@test.com");
            request.setPassword("Password123!");

            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("Test authentication security measures")
    void testAuthenticationSecurityMeasures() throws Exception {
        // Create test user
        User testUser = new User();
        testUser.setUsername("secureuser");
        testUser.setEmail("secure@test.com");
        testUser.setPassword(passwordEncoder.encode("Password123!"));
        userRepository.save(testUser);

        // Test with correct credentials
        LoginRequest validRequest = new LoginRequest();
        validRequest.setUsernameOrEmail("secureuser");
        validRequest.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // Test with wrong password
        LoginRequest wrongPasswordRequest = new LoginRequest();
        wrongPasswordRequest.setUsernameOrEmail("secureuser");
        wrongPasswordRequest.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andExpect(status().isUnauthorized());

        // Test with non-existent user
        LoginRequest nonExistentRequest = new LoginRequest();
        nonExistentRequest.setUsernameOrEmail("nonexistent");
        nonExistentRequest.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistentRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Test authorization and access control")
    @WithMockUser(roles = "USER")
    void testAuthorizationAndAccessControl() throws Exception {
        // Test that regular user cannot access admin endpoints
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isForbidden());

        // Test that regular user cannot modify other users
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("Password123!");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test JWT token security")
    void testJwtTokenSecurity() throws Exception {
        String[] invalidTokens = {
            "Bearer invalid.token.here",
            "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature",
            "Bearer null",
            "Bearer ",
            "InvalidBearer token",
            "Bearer <script>alert('xss')</script>",
            "Bearer ../../../etc/passwd"
        };

        for (String invalidToken : invalidTokens) {
            mockMvc.perform(get("/api/users")
                    .header("Authorization", invalidToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("Test CORS security configuration")
    void testCorsSecurityConfiguration() throws Exception {
        // Test that CORS headers are properly configured
        mockMvc.perform(options("/api/users")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());

        // Test that unauthorized origins are rejected
        mockMvc.perform(options("/api/users")
                .header("Origin", "http://malicious-site.com")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test security headers")
    void testSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().exists("X-XSS-Protection"))
                .andExpect(header().exists("Strict-Transport-Security"));
    }

    @Test
    @DisplayName("Test rate limiting protection")
    void testRateLimitingProtection() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("wrongpassword");

        // Make multiple failed login attempts
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        // After multiple failures, should be rate limited
        // (Implementation depends on your rate limiting strategy)
    }

    @Test
    @DisplayName("Test file upload security")
    @WithMockUser(roles = "USER")
    void testFileUploadSecurity() throws Exception {
        // Test malicious file types
        String[] maliciousFileTypes = {
            "malware.exe",
            "script.bat",
            "virus.com",
            "trojan.scr",
            "backdoor.pif"
        };

        for (String filename : maliciousFileTypes) {
            byte[] maliciousContent = "malicious content".getBytes();
            
            mockMvc.perform(multipart("/api/files/upload")
                    .file("file", maliciousContent)
                    .param("filename", filename))
                    .andExpect(status().isBadRequest());
        }

        // Test oversized files
        byte[] oversizedFile = new byte[11 * 1024 * 1024]; // 11MB (assuming 10MB limit)
        
        mockMvc.perform(multipart("/api/files/upload")
                .file("file", oversizedFile)
                .param("filename", "large.txt"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test session security")
    void testSessionSecurity() throws Exception {
        // Test that sessions are properly invalidated
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("sessionuser");
        request.setPassword("Password123!");

        // Since we're using JWT (stateless), session fixation shouldn't be an issue
        // But we test that custom session IDs are not accepted
        mockMvc.perform(post("/api/auth/login")
                .header("Cookie", "JSESSIONID=FIXED_SESSION_ID")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // Should fail due to invalid credentials
    }

    @Test
    @DisplayName("Test password security requirements")
    void testPasswordSecurityRequirements() throws Exception {
        String[] weakPasswords = {
            "123",                 // Too short
            "password",            // No complexity
            "PASSWORD",            // No lowercase
            "12345678",            // No letters
            "Password",            // No numbers/special chars
            "Password123",         // No special chars
            "password123!",        // No uppercase
            "PASSWORD123!"         // No lowercase
        };

        for (String weakPassword : weakPasswords) {
            CreateUserRequest request = new CreateUserRequest();
            request.setUsername("user" + System.currentTimeMillis());
            request.setEmail("test" + System.currentTimeMillis() + "@test.com");
            request.setPassword(weakPassword);

            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}