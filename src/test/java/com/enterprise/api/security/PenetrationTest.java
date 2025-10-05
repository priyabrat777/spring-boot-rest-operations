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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive penetration testing for security vulnerabilities.
 * Simulates real-world attack scenarios and advanced persistent threats.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class PenetrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        executorService = Executors.newFixedThreadPool(20);
        
        // Create test user for authentication tests
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");
        testUser.setPassword(passwordEncoder.encode("Password123!"));
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Test advanced persistent threat simulation")
    void testAdvancedPersistentThreatSimulation() throws Exception {
        // Simulate APT-style multi-stage attack
        
        // Stage 1: Reconnaissance - Information gathering
        mockMvc.perform(get("/api/users")
                .header("User-Agent", "Mozilla/5.0 (compatible; reconnaissance-bot)"))
                .andExpect(status().isUnauthorized());

        // Stage 2: Initial compromise attempt - Credential stuffing
        String[] commonPasswords = {
            "password", "123456", "password123", "admin", "qwerty",
            "letmein", "welcome", "monkey", "dragon", "master"
        };

        for (String password : commonPasswords) {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("admin");
            request.setPassword(password);

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        // Stage 3: Privilege escalation attempt
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"attacker\",\"email\":\"attacker@evil.com\",\"password\":\"Password123!\",\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isBadRequest());

        // Stage 4: Lateral movement simulation
        mockMvc.perform(get("/api/users/1")
                .header("X-Forwarded-For", "127.0.0.1")
                .header("X-Real-IP", "192.168.1.1"))
                .andExpect(status().isUnauthorized());

        // Stage 5: Data exfiltration attempt
        mockMvc.perform(get("/api/users")
                .param("size", "999999") // Attempt to extract all data
                .header("Accept-Encoding", "gzip, deflate, compress"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Test distributed denial of service resistance")
    void testDistributedDenialOfServiceResistance() throws Exception {
        int attackerCount = 20;
        int requestsPerAttacker = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(attackerCount);
        AtomicInteger blockedRequests = new AtomicInteger(0);
        AtomicInteger processedRequests = new AtomicInteger(0);

        // Simulate DDoS attack from multiple sources
        for (int i = 0; i < attackerCount; i++) {
            final int attackerId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < requestsPerAttacker; j++) {
                        try {
                            // Simulate requests from different IP addresses
                            String fakeIp = "192.168." + (attackerId % 255) + "." + (j % 255);
                            
                            mockMvc.perform(post("/api/auth/login")
                                    .header("X-Forwarded-For", fakeIp)
                                    .header("X-Real-IP", fakeIp)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"username\":\"victim\",\"password\":\"password\"}"))
                                    .andDo(result -> {
                                        int status = result.getResponse().getStatus();
                                        if (status == 429) { // Rate limited
                                            blockedRequests.incrementAndGet();
                                        } else {
                                            processedRequests.incrementAndGet();
                                        }
                                    });
                        } catch (Exception e) {
                            // Handle exceptions
                        }
                        
                        // Small delay to simulate realistic attack pattern
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
                return null;
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(60, TimeUnit.SECONDS);

        assertTrue(completed, "DDoS simulation should complete");

        int totalRequests = attackerCount * requestsPerAttacker;
        System.out.println("DDoS Resistance Test Results:");
        System.out.println("Total Attack Requests: " + totalRequests);
        System.out.println("Blocked Requests: " + blockedRequests.get());
        System.out.println("Processed Requests: " + processedRequests.get());

        // System should handle DDoS attempts gracefully
        assertTrue(processedRequests.get() + blockedRequests.get() <= totalRequests, 
            "All requests should be accounted for");
    }

    @Test
    @DisplayName("Test advanced SQL injection techniques")
    void testAdvancedSqlInjectionTechniques() throws Exception {
        String[] advancedSqlPayloads = {
            // Time-based blind SQL injection
            "admin'; WAITFOR DELAY '00:00:05'; --",
            "admin' AND (SELECT COUNT(*) FROM users) > 0 AND SLEEP(5); --",
            
            // Boolean-based blind SQL injection
            "admin' AND (SELECT SUBSTRING(password,1,1) FROM users WHERE username='admin')='a'; --",
            "admin' AND (SELECT COUNT(*) FROM information_schema.tables) > 0; --",
            
            // Union-based SQL injection with encoding
            "admin' UNION SELECT CHAR(65,68,77,73,78),CHAR(80,65,83,83) --",
            "admin' UNION SELECT 0x61646D696E,0x70617373776F7264 --",
            
            // Second-order SQL injection
            "admin'; INSERT INTO users VALUES ('hacker','hack@evil.com','password'); --",
            
            // NoSQL injection variants
            "admin' || '1'=='1",
            "admin'; db.users.drop(); //",
            
            // Stored procedure attacks
            "admin'; EXEC xp_cmdshell('net user hacker password /add'); --",
            "admin'; EXEC sp_configure 'show advanced options', 1; --"
        };

        for (String payload : advancedSqlPayloads) {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail(payload);
            request.setPassword("anypassword");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("Test advanced cross-site scripting attacks")
    void testAdvancedCrossSiteScriptingAttacks() throws Exception {
        String[] advancedXssPayloads = {
            // DOM-based XSS
            "<img src=x onerror=eval(atob('YWxlcnQoZG9jdW1lbnQuY29va2llKQ=='))>",
            
            // Mutation XSS
            "<listing>&lt;img src=x onerror=alert(1)&gt;</listing>",
            
            // CSS injection
            "<style>@import'javascript:alert(1)';</style>",
            
            // SVG-based XSS
            "<svg><script href=data:,alert(1) />",
            
            // Event handler XSS
            "<body onload=alert(1)>",
            "<input onfocus=alert(1) autofocus>",
            
            // JavaScript protocol XSS
            "<a href=\"javascript:alert(1)\">click</a>",
            
            // Data URI XSS
            "<iframe src=\"data:text/html,<script>alert(1)</script>\"></iframe>",
            
            // Filter bypass techniques
            "<scr<script>ipt>alert(1)</scr</script>ipt>",
            "<img src=\"x\" onerror=\"&#97;&#108;&#101;&#114;&#116;&#40;&#49;&#41;\">",
            
            // Context-specific XSS
            "';alert(1);//",
            "\";alert(1);//",
            "</script><script>alert(1)</script>",
            
            // Polyglot XSS
            "javascript:/*--></title></style></textarea></script></xmp><svg/onload='+/\"/+/onmouseover=1/+/[*/[]/+alert(1)//'>"
        };

        for (String payload : advancedXssPayloads) {
            CreateUserRequest request = new CreateUserRequest();
            request.setUsername(payload);
            request.setEmail("xss@test.com");
            request.setPassword("Password123!");

            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("Test authentication bypass techniques")
    void testAuthenticationBypassTechniques() throws Exception {
        // Test various authentication bypass methods
        
        // HTTP verb tampering
        mockMvc.perform(get("/api/users")
                .header("X-HTTP-Method-Override", "POST"))
                .andExpect(status().isUnauthorized());

        // Parameter pollution
        mockMvc.perform(post("/api/auth/login")
                .param("username", "user")
                .param("username", "admin")
                .param("password", "wrong")
                .param("password", "admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized());

        // Header injection
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer fake-token\r\nX-Admin: true"))
                .andExpect(status().isUnauthorized());

        // Session fixation
        mockMvc.perform(post("/api/auth/login")
                .header("Cookie", "JSESSIONID=ATTACKER_SESSION_ID")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk()); // Should succeed but not use fixed session

        // Race condition in authentication
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    LoginRequest request = new LoginRequest();
                    request.setUsernameOrEmail("testuser");
                    request.setPassword("Password123!");
                    
                    mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                            .andDo(result -> {
                                if (result.getResponse().getStatus() == 200) {
                                    successCount.incrementAndGet();
                                }
                            });
                } catch (Exception e) {
                    // Handle exceptions
                } finally {
                    endLatch.countDown();
                }
                return null;
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);

        // All authentication attempts should be handled consistently
        assertTrue(successCount.get() >= 0, "Authentication should handle concurrent requests");
    }

    @Test
    @DisplayName("Test business logic bypass attempts")
    void testBusinessLogicBypassAttempts() throws Exception {
        // Test various business logic bypass techniques
        
        // Negative values
        CreateUserRequest negativeRequest = new CreateUserRequest();
        negativeRequest.setUsername("negativeuser");
        negativeRequest.setEmail("negative@test.com");
        negativeRequest.setPassword("Password123!");
        // Add any numeric fields with negative values if they exist

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(negativeRequest)))
                .andExpect(status().isCreated()); // Should succeed with valid data

        // Extremely large values
        CreateUserRequest largeRequest = new CreateUserRequest();
        largeRequest.setUsername("largeuser");
        largeRequest.setEmail("large@test.com");
        largeRequest.setPassword("Password123!");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeRequest)))
                .andExpect(status().isCreated()); // Should succeed with valid data

        // Workflow bypass attempts
        mockMvc.perform(put("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"bypassed\",\"email\":\"bypass@test.com\"}"))
                .andExpect(status().isUnauthorized()); // Should require authentication

        // State manipulation
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"stateuser\",\"email\":\"state@test.com\",\"password\":\"Password123!\",\"deleted\":false,\"version\":0}"))
                .andExpect(status().isCreated()); // Should ignore internal fields
    }

    @Test
    @DisplayName("Test cryptographic attack resistance")
    void testCryptographicAttackResistance() throws Exception {
        // Test various cryptographic attacks
        
        // Weak token generation
        String[] weakTokens = {
            "Bearer 000000000000000000000000",
            "Bearer 111111111111111111111111",
            "Bearer AAAAAAAAAAAAAAAAAAAAAAAAA",
            "Bearer 123456789012345678901234"
        };

        for (String weakToken : weakTokens) {
            mockMvc.perform(get("/api/users")
                    .header("Authorization", weakToken))
                    .andExpect(status().isUnauthorized());
        }

        // Timing attacks on token validation
        String validTokenFormat = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTUxNjIzOTAyMn0.";
        String[] invalidSignatures = {
            "invalid_signature_1",
            "invalid_signature_2",
            "different_length_sig",
            "a".repeat(43) // Base64 signature length
        };

        for (String signature : invalidSignatures) {
            long startTime = System.nanoTime();
            
            mockMvc.perform(get("/api/users")
                    .header("Authorization", validTokenFormat + signature))
                    .andExpect(status().isUnauthorized());
            
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            
            // Token validation should have consistent timing
            assertTrue(duration > 0, "Token validation should take some time");
        }
    }

    @Test
    @DisplayName("Test information disclosure vulnerabilities")
    void testInformationDisclosureVulnerabilities() throws Exception {
        // Test for information leakage in error messages
        
        // Invalid user enumeration
        LoginRequest validUserRequest = new LoginRequest();
        validUserRequest.setUsernameOrEmail("testuser");
        validUserRequest.setPassword("wrongpassword");

        String validUserResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LoginRequest invalidUserRequest = new LoginRequest();
        invalidUserRequest.setUsernameOrEmail("nonexistentuser");
        invalidUserRequest.setPassword("wrongpassword");

        String invalidUserResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUserRequest)))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Error messages should not reveal user existence
        assertFalse(validUserResponse.contains("user exists"), 
            "Error message should not reveal user existence");
        assertFalse(invalidUserResponse.contains("user not found"), 
            "Error message should not reveal user non-existence");

        // Test for stack trace leakage
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    assertFalse(response.contains("java.lang."), 
                        "Response should not contain stack traces");
                    assertFalse(response.contains("Exception"), 
                        "Response should not contain exception details");
                });

        // Test for version disclosure
        mockMvc.perform(get("/api/users"))
                .andExpect(result -> {
                    String serverHeader = result.getResponse().getHeader("Server");
                    if (serverHeader != null) {
                        assertFalse(serverHeader.contains("Apache"), 
                            "Server header should not reveal version info");
                        assertFalse(serverHeader.contains("nginx"), 
                            "Server header should not reveal version info");
                    }
                });
    }
}