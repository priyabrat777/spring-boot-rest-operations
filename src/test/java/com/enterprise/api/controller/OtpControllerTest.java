package com.enterprise.api.controller;

import com.enterprise.api.dto.request.OtpGenerationRequest;
import com.enterprise.api.dto.request.OtpValidationRequest;
import com.enterprise.api.dto.response.OtpGenerationResponse;
import com.enterprise.api.dto.response.OtpValidationResponse;
import com.enterprise.api.entity.OtpPurpose;
import com.enterprise.api.entity.OtpType;
import com.enterprise.api.service.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for OtpController.
 * Tests OTP generation and validation endpoints with various scenarios.
 */
@WebMvcTest(OtpController.class)
class OtpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OtpService otpService;

    @Autowired
    private ObjectMapper objectMapper;

    private OtpGenerationRequest generationRequest;
    private OtpValidationRequest validationRequest;
    private OtpGenerationResponse generationResponse;
    private OtpValidationResponse validationResponse;

    @BeforeEach
    void setUp() {
        generationRequest = new OtpGenerationRequest(
            "test@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN
        );

        validationRequest = new OtpValidationRequest(
            "test@example.com",
            "123456",
            OtpPurpose.LOGIN
        );

        generationResponse = new OtpGenerationResponse(
            "1",
            "te****@example.com",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            LocalDateTime.now().plusMinutes(5),
            3,
            "OTP generated successfully",
            true,
            "Email"
        );

        validationResponse = OtpValidationResponse.success(true);
    }

    @Test
    void generateOtp_Success() throws Exception {
        // Arrange
        when(otpService.generateOtp(any(OtpGenerationRequest.class), anyString(), anyString()))
            .thenReturn(generationResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpId").value("1"))
                .andExpect(jsonPath("$.maskedIdentifier").value("te****@example.com"))
                .andExpect(jsonPath("$.type").value("EMAIL"))
                .andExpect(jsonPath("$.purpose").value("LOGIN"))
                .andExpect(jsonPath("$.maxAttempts").value(3))
                .andExpect(jsonPath("$.delivered").value(true))
                .andExpect(jsonPath("$.deliveryMethod").value("Email"));

        verify(otpService).generateOtp(any(OtpGenerationRequest.class), anyString(), anyString());
    }

    @Test
    void generateOtp_InvalidRequest() throws Exception {
        // Arrange
        OtpGenerationRequest invalidRequest = new OtpGenerationRequest();
        // Missing required fields

        // Act & Assert
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).generateOtp(any(OtpGenerationRequest.class), anyString(), anyString());
    }

    @Test
    void generateOtp_RateLimited() throws Exception {
        // Arrange
        when(otpService.generateOtp(any(OtpGenerationRequest.class), anyString(), anyString()))
            .thenThrow(new IllegalStateException("Rate limit exceeded for identifier: te****@example.com"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMITED"));

        verify(otpService).generateOtp(any(OtpGenerationRequest.class), anyString(), anyString());
    }

    @Test
    void generateOtp_AccountLocked() throws Exception {
        // Arrange
        when(otpService.generateOtp(any(OtpGenerationRequest.class), anyString(), anyString()))
            .thenThrow(new IllegalStateException("Identifier is temporarily locked: te****@example.com"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.error").value("ACCOUNT_LOCKED"));

        verify(otpService).generateOtp(any(OtpGenerationRequest.class), anyString(), anyString());
    }

    @Test
    void generateOtp_InternalError() throws Exception {
        // Arrange
        when(otpService.generateOtp(any(OtpGenerationRequest.class), anyString(), anyString()))
            .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/otp/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));

        verify(otpService).generateOtp(any(OtpGenerationRequest.class), anyString(), anyString());
    }

    @Test
    void validateOtp_Success() throws Exception {
        // Arrange
        when(otpService.validateOtp(any(OtpValidationRequest.class), anyString()))
            .thenReturn(validationResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/otp/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.consumed").value(true));

        verify(otpService).validateOtp(any(OtpValidationRequest.class), anyString());
    }

    @Test
    void validateOtp_InvalidCode() throws Exception {
        // Arrange
        OtpValidationResponse failureResponse = OtpValidationResponse.failure("Invalid OTP code", 2);
        when(otpService.validateOtp(any(OtpValidationRequest.class), anyString()))
            .thenReturn(failureResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/otp/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.failureReason").value("Invalid OTP code"))
                .andExpect(jsonPath("$.remainingAttempts").value(2));

        verify(otpService).validateOtp(any(OtpValidationRequest.class), anyString());
    }

    @Test
    void validateOtp_AccountLocked() throws Exception {
        // Arrange
        OtpValidationResponse lockedResponse = OtpValidationResponse.locked(LocalDateTime.now().plusMinutes(30));
        when(otpService.validateOtp(any(OtpValidationRequest.class), anyString()))
            .thenReturn(lockedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/otp/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.failureReason").value("TOO_MANY_ATTEMPTS"));

        verify(otpService).validateOtp(any(OtpValidationRequest.class), anyString());
    }

    @Test
    void validateOtp_InvalidRequest() throws Exception {
        // Arrange
        OtpValidationRequest invalidRequest = new OtpValidationRequest();
        // Missing required fields

        // Act & Assert
        mockMvc.perform(post("/api/v1/otp/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).validateOtp(any(OtpValidationRequest.class), anyString());
    }

    @Test
    void validateOtp_InvalidCodeFormat() throws Exception {
        // Arrange
        validationRequest.setCode("abc"); // Too short and contains letters

        // Act & Assert
        mockMvc.perform(post("/api/v1/otp/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isBadRequest());

        verify(otpService, never()).validateOtp(any(OtpValidationRequest.class), anyString());
    }

    @Test
    void checkRateLimit_NotLimited() throws Exception {
        // Arrange
        when(otpService.isRateLimited("test@example.com")).thenReturn(false);
        when(otpService.getRateLimitResetTime("test@example.com")).thenReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/api/v1/otp/rate-limit/test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rateLimited").value(false))
                .andExpect(jsonPath("$.resetTimeSeconds").value(0))
                .andExpect(jsonPath("$.identifier").value("te****@example.com"));

        verify(otpService).isRateLimited("test@example.com");
        verify(otpService).getRateLimitResetTime("test@example.com");
    }

    @Test
    void checkRateLimit_Limited() throws Exception {
        // Arrange
        when(otpService.isRateLimited("test@example.com")).thenReturn(true);
        when(otpService.getRateLimitResetTime("test@example.com")).thenReturn(900L);

        // Act & Assert
        mockMvc.perform(get("/api/v1/otp/rate-limit/test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rateLimited").value(true))
                .andExpect(jsonPath("$.resetTimeSeconds").value(900));

        verify(otpService).isRateLimited("test@example.com");
        verify(otpService).getRateLimitResetTime("test@example.com");
    }

    @Test
    void checkLockStatus_NotLocked() throws Exception {
        // Arrange
        when(otpService.isLocked("test@example.com")).thenReturn(false);
        when(otpService.getLockResetTime("test@example.com")).thenReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/api/v1/otp/lock-status/test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.resetTimeSeconds").value(0));

        verify(otpService).isLocked("test@example.com");
        verify(otpService).getLockResetTime("test@example.com");
    }

    @Test
    void checkLockStatus_Locked() throws Exception {
        // Arrange
        when(otpService.isLocked("test@example.com")).thenReturn(true);
        when(otpService.getLockResetTime("test@example.com")).thenReturn(1800L);

        // Act & Assert
        mockMvc.perform(get("/api/v1/otp/lock-status/test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.resetTimeSeconds").value(1800));

        verify(otpService).isLocked("test@example.com");
        verify(otpService).getLockResetTime("test@example.com");
    }

    @Test
    void invalidateOtps_Success() throws Exception {
        // Arrange
        when(otpService.invalidateOtps("test@example.com", OtpPurpose.LOGIN)).thenReturn(2);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/otp/invalidate/test@example.com/LOGIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invalidatedCount").value(2))
                .andExpect(jsonPath("$.identifier").value("te****@example.com"))
                .andExpect(jsonPath("$.purpose").value("LOGIN"));

        verify(otpService).invalidateOtps("test@example.com", OtpPurpose.LOGIN);
    }

    @Test
    void invalidateOtps_InvalidPurpose() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/otp/invalidate/test@example.com/INVALID_PURPOSE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PURPOSE"));

        verify(otpService, never()).invalidateOtps(anyString(), any(OtpPurpose.class));
    }

    @Test
    void cleanupExpiredOtps_Success() throws Exception {
        // Arrange
        when(otpService.cleanupExpiredOtps()).thenReturn(5);

        // Act & Assert
        mockMvc.perform(post("/api/v1/otp/cleanup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cleanedUpCount").value(5))
                .andExpect(jsonPath("$.message").value("Expired OTPs cleaned up successfully"));

        verify(otpService).cleanupExpiredOtps();
    }

    @Test
    void handleOptions_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(options("/api/v1/otp/generate"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type, Authorization"));
    }

    @Test
    void handleHead_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(head("/api/v1/otp/generate"))
                .andExpect(status().isOk());
    }

    @Test
    void generateOtp_WithXForwardedForHeader() throws Exception {
        // Arrange
        when(otpService.generateOtp(any(OtpGenerationRequest.class), eq("192.168.1.100"), anyString()))
            .thenReturn(generationResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/otp/generate")
                .header("X-Forwarded-For", "192.168.1.100, 10.0.0.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk());

        verify(otpService).generateOtp(any(OtpGenerationRequest.class), eq("192.168.1.100"), anyString());
    }

    @Test
    void generateOtp_WithXRealIpHeader() throws Exception {
        // Arrange
        when(otpService.generateOtp(any(OtpGenerationRequest.class), eq("203.0.113.1"), anyString()))
            .thenReturn(generationResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/otp/generate")
                .header("X-Real-IP", "203.0.113.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk());

        verify(otpService).generateOtp(any(OtpGenerationRequest.class), eq("203.0.113.1"), anyString());
    }

    @Test
    void generateOtp_WithUserAgentHeader() throws Exception {
        // Arrange
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        when(otpService.generateOtp(any(OtpGenerationRequest.class), anyString(), eq(userAgent)))
            .thenReturn(generationResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/otp/generate")
                .header("User-Agent", userAgent)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isOk());

        verify(otpService).generateOtp(any(OtpGenerationRequest.class), anyString(), eq(userAgent));
    }
}