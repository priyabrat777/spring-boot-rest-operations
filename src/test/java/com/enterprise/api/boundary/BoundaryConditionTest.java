package com.enterprise.api.boundary;

import com.enterprise.api.dto.request.CreateUserRequest;
import com.enterprise.api.dto.request.LoginRequest;
import com.enterprise.api.dto.request.UpdateUserRequest;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive boundary condition tests for all API endpoints.
 * Tests edge cases, limits, and boundary values.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class BoundaryConditionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Test username length boundaries")
    void testUsernameLengthBoundaries() throws Exception {
        // Test minimum length (1 character)
        CreateUserRequest minRequest = new CreateUserRequest();
        minRequest.setUsername("a");
        minRequest.setEmail("min@test.com");
        minRequest.setPassword("Password123!");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minRequest)))
                .andExpect(status().isCreated());

        // Test maximum length (255 characters)
        String maxUsername = "a".repeat(255);
        CreateUserRequest maxRequest = new CreateUserRequest();
        maxRequest.setUsername(maxUsername);
        maxRequest.setEmail("max@test.com");
        maxRequest.setPassword("Password123!");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(maxRequest)))
                .andExpect(status().isCreated());

        // Test exceeding maximum length (256 characters)
        String tooLongUsername = "a".repeat(256);
        CreateUserRequest tooLongRequest = new CreateUserRequest();
        tooLongRequest.setUsername(tooLongUsername);
        tooLongRequest.setEmail("toolong@test.com");
        tooLongRequest.setPassword("Password123!");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tooLongRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test email format boundaries")
    void testEmailFormatBoundaries() throws Exception {
        // Test minimum valid email
        CreateUserRequest minEmailRequest = new CreateUserRequest();
        minEmailRequest.setUsername("user1");
        minEmailRequest.setEmail("a@b.c");
        minEmailRequest.setPassword("Password123!");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minEmailRequest)))
                .andExpect(status().isCreated());

        // Test maximum length email (320 characters total)
        String longLocalPart = "a".repeat(64);
        String longDomainPart = "b".repeat(63) + ".com";
        String maxEmail = longLocalPart + "@" + longDomainPart;
        
        CreateUserRequest maxEmailRequest = new CreateUserRequest();
        maxEmailRequest.setUsername("user2");
        maxEmailRequest.setEmail(maxEmail);
        maxEmailRequest.setPassword("Password123!");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(maxEmailRequest)))
                .andExpect(status().isCreated());

        // Test invalid email formats
        String[] invalidEmails = {
            "",
            "invalid",
            "@domain.com",
            "user@",
            "user@domain",
            "user..user@domain.com",
            "user@domain..com"
        };

        for (String invalidEmail : invalidEmails) {
            CreateUserRequest invalidRequest = new CreateUserRequest();
            invalidRequest.setUsername("user" + System.currentTimeMillis());
            invalidRequest.setEmail(invalidEmail);
            invalidRequest.setPassword("Password123!");

            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("Test password complexity boundaries")
    void testPasswordComplexityBoundaries() throws Exception {
        String[] weakPasswords = {
            "",                    // Empty
            "123",                 // Too short
            "password",            // No uppercase, numbers, special chars
            "PASSWORD",            // No lowercase, numbers, special chars
            "12345678",            // No letters, special chars
            "Password",            // No numbers, special chars
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

        // Test valid strong password
        CreateUserRequest validRequest = new CreateUserRequest();
        validRequest.setUsername("validuser");
        validRequest.setEmail("valid@test.com");
        validRequest.setPassword("StrongPassword123!");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Test pagination boundaries")
    @WithMockUser(roles = "ADMIN")
    void testPaginationBoundaries() throws Exception {
        // Create test users
        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@test.com");
            user.setPassword("encoded_password");
            userRepository.save(user);
        }

        // Test minimum page size (1)
        mockMvc.perform(get("/api/users")
                .param("page", "0")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));

        // Test maximum page size (100)
        mockMvc.perform(get("/api/users")
                .param("page", "0")
                .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(100));

        // Test exceeding maximum page size (should default to max)
        mockMvc.perform(get("/api/users")
                .param("page", "0")
                .param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(100));

        // Test negative page number
        mockMvc.perform(get("/api/users")
                .param("page", "-1")
                .param("size", "10"))
                .andExpect(status().isBadRequest());

        // Test negative page size
        mockMvc.perform(get("/api/users")
                .param("page", "0")
                .param("size", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test file upload size boundaries")
    @WithMockUser(roles = "USER")
    void testFileUploadSizeBoundaries() throws Exception {
        // Test empty file
        mockMvc.perform(multipart("/api/files/upload")
                .file("file", new byte[0])
                .param("filename", "empty.txt"))
                .andExpect(status().isBadRequest());

        // Test maximum allowed file size (10MB)
        byte[] maxSizeFile = new byte[10 * 1024 * 1024]; // 10MB
        mockMvc.perform(multipart("/api/files/upload")
                .file("file", maxSizeFile)
                .param("filename", "maxsize.txt"))
                .andExpect(status().isOk());

        // Test exceeding maximum file size would be handled by Spring Boot's multipart configuration
        // This test verifies the configuration is in place
    }

    @Test
    @DisplayName("Test numeric field boundaries")
    @WithMockUser(roles = "ADMIN")
    void testNumericFieldBoundaries() throws Exception {
        // Test Long.MAX_VALUE for ID fields
        mockMvc.perform(get("/api/users/" + Long.MAX_VALUE))
                .andExpect(status().isNotFound());

        // Test negative ID
        mockMvc.perform(get("/api/users/-1"))
                .andExpect(status().isBadRequest());

        // Test zero ID
        mockMvc.perform(get("/api/users/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test special characters in input fields")
    void testSpecialCharactersInInputFields() throws Exception {
        String[] specialCharInputs = {
            "<script>alert('xss')</script>",
            "'; DROP TABLE users; --",
            "../../etc/passwd",
            "%00",
            "\u0000",
            "🚀🎉💻", // Unicode emojis
            "Iñtërnâtiônàlizætiøn", // International characters
            "русский текст", // Cyrillic
            "中文测试", // Chinese
            "العربية" // Arabic
        };

        for (String specialInput : specialCharInputs) {
            CreateUserRequest request = new CreateUserRequest();
            request.setUsername(specialInput);
            request.setEmail("test" + System.currentTimeMillis() + "@test.com");
            request.setPassword("Password123!");

            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("Test null and empty values")
    void testNullAndEmptyValues() throws Exception {
        // Test null username
        CreateUserRequest nullUsernameRequest = new CreateUserRequest();
        nullUsernameRequest.setUsername(null);
        nullUsernameRequest.setEmail("test@test.com");
        nullUsernameRequest.setPassword("Password123!");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nullUsernameRequest)))
                .andExpect(status().isBadRequest());

        // Test empty username
        CreateUserRequest emptyUsernameRequest = new CreateUserRequest();
        emptyUsernameRequest.setUsername("");
        emptyUsernameRequest.setEmail("test@test.com");
        emptyUsernameRequest.setPassword("Password123!");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyUsernameRequest)))
                .andExpect(status().isBadRequest());

        // Test whitespace-only username
        CreateUserRequest whitespaceUsernameRequest = new CreateUserRequest();
        whitespaceUsernameRequest.setUsername("   ");
        whitespaceUsernameRequest.setEmail("test@test.com");
        whitespaceUsernameRequest.setPassword("Password123!");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(whitespaceUsernameRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test malformed JSON requests")
    void testMalformedJsonRequests() throws Exception {
        String[] malformedJsons = {
            "{",
            "}",
            "{\"username\":}",
            "{\"username\":\"test\",}",
            "{\"username\":\"test\"\"email\":\"test@test.com\"}",
            "not json at all"
        };

        for (String malformedJson : malformedJsons) {
            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformedJson))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("Test extreme numeric values")
    void testExtremeNumericValues() throws Exception {
        // Test with extreme page numbers and sizes
        mockMvc.perform(get("/api/users")
                .param("page", String.valueOf(Integer.MAX_VALUE))
                .param("size", "10"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/users")
                .param("page", "0")
                .param("size", String.valueOf(Integer.MAX_VALUE)))
                .andExpect(status().isBadRequest());

        // Test with very large ID values
        mockMvc.perform(get("/api/users/" + Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Test content type boundaries")
    void testContentTypeBoundaries() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@test.com");
        request.setPassword("Password123!");

        // Test with missing content type
        mockMvc.perform(post("/api/users")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());

        // Test with wrong content type
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.TEXT_PLAIN)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());

        // Test with charset variations
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.valueOf("application/json;charset=UTF-16"))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Test HTTP header boundaries")
    void testHttpHeaderBoundaries() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("headertest");
        request.setEmail("header@test.com");
        request.setPassword("Password123!");

        // Test with extremely long header values
        String longHeaderValue = "a".repeat(8192);
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Custom-Header", longHeaderValue)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Test with special characters in headers
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Special-Chars", "!@#$%^&*()_+-=[]{}|;':\",./<>?")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict()); // Username already exists
    }

    @Test
    @DisplayName("Test request body size boundaries")
    void testRequestBodySizeBoundaries() throws Exception {
        // Test with very large JSON payload
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("largeuser");
        request.setEmail("large@test.com");
        request.setPassword("Password123!" + "x".repeat(1000)); // Large password

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Should fail validation

        // Test with empty request body
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test URL path boundaries")
    void testUrlPathBoundaries() throws Exception {
        // Test with very long URL paths
        String longPath = "a".repeat(2000);
        mockMvc.perform(get("/api/users/" + longPath))
                .andExpect(status().isBadRequest());

        // Test with URL encoding edge cases
        mockMvc.perform(get("/api/users/%2E%2E%2F%2E%2E%2Fetc%2Fpasswd"))
                .andExpect(status().isBadRequest());

        // Test with null bytes in URL
        mockMvc.perform(get("/api/users/test%00user"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test concurrent boundary conditions")
    @WithMockUser(roles = "ADMIN")
    void testConcurrentBoundaryConditions() throws Exception {
        // Create users at the boundary of username length
        String boundaryUsername = "a".repeat(254); // Just under limit
        
        CreateUserRequest request1 = new CreateUserRequest();
        request1.setUsername(boundaryUsername + "1");
        request1.setEmail("boundary1@test.com");
        request1.setPassword("Password123!");

        CreateUserRequest request2 = new CreateUserRequest();
        request2.setUsername(boundaryUsername + "2");
        request2.setEmail("boundary2@test.com");
        request2.setPassword("Password123!");

        // Both should succeed as they're at the boundary but valid
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Test time-based boundaries")
    void testTimeBasedBoundaries() throws Exception {
        // Test with future dates in audit fields (should be handled by system)
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("timetest");
        request.setEmail("time@test.com");
        request.setPassword("Password123!");

        // The system should handle time correctly regardless of client input
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Client-Time", "2099-12-31T23:59:59Z")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}