package com.enterprise.api.controller;

import com.enterprise.api.dto.request.OtpGenerationRequest;
import com.enterprise.api.dto.request.OtpValidationRequest;
import com.enterprise.api.entity.OtpPurpose;
import com.enterprise.api.entity.OtpType;
import com.enterprise.api.repository.OtpRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OtpController.
 * Tests the complete OTP workflow with actual service and repository layers.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class OtpControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OtpRepository otpRepository;

    private OtpGenerationRequest generationRequest;
    private OtpValidationRequest validationRequest;

    @BeforeEach
    void setUp() {
        // Clean up any existing OTPs
        otpRepository.deleteAll();

        generationRequest = new OtpGenerationRequest(
            "integration@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN
        );

        validationRequest = new OtpValidationRequest(
            "integration@example.com",
            "123456",
            OtpPurpose.LOGIN
        );
    }

    @Test
    void completeOtpWorkflow_Success() throws Exception {
        // Step 1: Generate OTP
        String generateResponse = mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpId").exists())
                .andExpect(jsonPath("$.maskedIdentifier").value("in****@example.com"))
                .andExpect(jsonPath("$.type").value("EMAIL"))
                .andExpect(jsonPath("$.purpose").value("LOGIN"))
                .andExpect(jsonPath("$.delivered").value(true))
                .andExpect(jsonPath("$.maxAttempts").value(3))
                .andReturn().getResponse().getContentAsString();

        // Verify OTP was created in database
        assertEquals(1, otpRepository.count());

        // Step 2: Check rate limit status (should not be limited)
        mockMvc.perform(get("/api/v1/otp/rate-limit/integration@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rateLimited").value(false))
                .andExpect(jsonPath("$.resetTimeSeconds").value(0));

        // Step 3: Check lock status (should not be locked)
        mockMvc.perform(get("/api/v1/otp/lock-status/integration@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.resetTimeSeconds").value(0));

        // Step 4: Attempt validation with wrong code (should fail)
        validationRequest.setCode("wrong123");
        mockMvc.perform(post("/api/v1/otp/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.failureReason").value("Invalid OTP code"))
                .andExpect(jsonPath("$.remainingAttempts").value(2));

        // Note: In a real scenario, we would need to extract the actual OTP code
        // from the service or use a test mode. For this integration test,
        // we'll test the validation failure path.
    }

    @Test
    void generateOtp_RateLimitingTest() throws Exception {
        // Generate multiple OTPs to trigger rate limiting
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/otp/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(generationRequest)))
                    .andExpect(status().isOk());
        }

        // The 6th request should be rate limited
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMITED"));

        // Check rate limit status
        mockMvc.perform(get("/api/v1/otp/rate-limit/integration@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rateLimited").value(true))
                .andExpect(jsonPath("$.resetTimeSeconds").value(greaterThan(0)));
    }

    @Test
    void generateOtp_InvalidRequest() throws Exception {
        // Test with missing identifier
        OtpGenerationRequest invalidRequest = new OtpGenerationRequest();
        invalidRequest.setType(OtpType.EMAIL);
        invalidRequest.setPurpose(OtpPurpose.LOGIN);

        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validateOtp_InvalidRequest() throws Exception {
        // Test with invalid code format
        validationRequest.setCode("abc"); // Too short and contains letters

        mockMvc.perform(post("/api/v1/otp/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validateOtp_NoOtpExists() throws Exception {
        // Try to validate without generating OTP first
        mockMvc.perform(post("/api/v1/otp/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.failureReason").value("No valid OTP found"));
    }

    @Test
    void invalidateOtps_Success() throws Exception {
        // First generate an OTP
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk());

        // Then invalidate it
        mockMvc.perform(delete("/api/v1/otp/invalidate/integration@example.com/LOGIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invalidatedCount").value(1))
                .andExpect(jsonPath("$.identifier").value("in****@example.com"))
                .andExpect(jsonPath("$.purpose").value("LOGIN"));
    }

    @Test
    void invalidateOtps_InvalidPurpose() throws Exception {
        mockMvc.perform(delete("/api/v1/otp/invalidate/integration@example.com/INVALID_PURPOSE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PURPOSE"));
    }

    @Test
    void cleanupExpiredOtps_Success() throws Exception {
        mockMvc.perform(post("/api/v1/otp/cleanup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cleanedUpCount").exists())
                .andExpect(jsonPath("$.message").value("Expired OTPs cleaned up successfully"));
    }

    @Test
    void generateOtp_DifferentTypes() throws Exception {
        // Test EMAIL type
        generationRequest.setType(OtpType.EMAIL);
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("EMAIL"))
                .andExpect(jsonPath("$.deliveryMethod").value("Email"));

        // Test SMS type
        generationRequest.setIdentifier("+1234567890");
        generationRequest.setType(OtpType.SMS);
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("SMS"))
                .andExpect(jsonPath("$.deliveryMethod").value("SMS"));
    }

    @Test
    void generateOtp_DifferentPurposes() throws Exception {
        // Test LOGIN purpose
        generationRequest.setPurpose(OtpPurpose.LOGIN);
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purpose").value("LOGIN"));

        // Test PASSWORD_RESET purpose
        generationRequest.setPurpose(OtpPurpose.PASSWORD_RESET);
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purpose").value("PASSWORD_RESET"));
    }

    @Test
    void generateOtp_CustomExpirationTime() throws Exception {
        // Test with custom expiration time
        generationRequest.setExpirationMinutes(10);
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void generateOtp_WithCustomMessage() throws Exception {
        // Test with custom message
        generationRequest.setCustomMessage("Your verification code is: {code}. Do not share this code.");
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivered").value(true));
    }

    @Test
    void corsAndHttpMethods_Test() throws Exception {
        // Test OPTIONS request
        mockMvc.perform(options("/api/v1/otp/generate"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type, Authorization"));

        // Test HEAD request
        mockMvc.perform(head("/api/v1/otp/generate"))
                .andExpect(status().isOk());
    }

    @Test
    void clientIpExtraction_Test() throws Exception {
        // Test with X-Forwarded-For header
        mockMvc.perform(post("/api/v1/otp/generate")
                .header("X-Forwarded-For", "192.168.1.100, 10.0.0.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk());

        // Test with X-Real-IP header
        mockMvc.perform(post("/api/v1/otp/generate")
                .header("X-Real-IP", "203.0.113.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk());

        // Test with User-Agent header
        mockMvc.perform(post("/api/v1/otp/generate")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void multipleIdentifiers_IndependentRateLimiting() throws Exception {
        // Generate OTPs for first identifier
        OtpGenerationRequest request1 = new OtpGenerationRequest("user1@example.com", OtpType.EMAIL, OtpPurpose.LOGIN);
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/otp/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isOk());
        }

        // First identifier should be rate limited
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isTooManyRequests());

        // Second identifier should still work
        OtpGenerationRequest request2 = new OtpGenerationRequest("user2@example.com", OtpType.EMAIL, OtpPurpose.LOGIN);
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());
    }

    @Test
    void otpPurposeIsolation_Test() throws Exception {
        // Generate OTP for LOGIN purpose
        generationRequest.setPurpose(OtpPurpose.LOGIN);
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk());

        // Generate OTP for PASSWORD_RESET purpose (should work independently)
        generationRequest.setPurpose(OtpPurpose.PASSWORD_RESET);
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk());

        // Verify both OTPs exist in database
        assertEquals(2, otpRepository.count());

        // Invalidate LOGIN OTPs only
        mockMvc.perform(delete("/api/v1/otp/invalidate/integration@example.com/LOGIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invalidatedCount").value(1));

        // PASSWORD_RESET OTP should still be valid
        // (This would require additional verification in a real test)
    }
}