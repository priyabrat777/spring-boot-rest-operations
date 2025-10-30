package com.enterprise.api.service;

import com.enterprise.api.dto.request.OtpGenerationRequest;
import com.enterprise.api.dto.request.OtpValidationRequest;
import com.enterprise.api.dto.response.OtpGenerationResponse;
import com.enterprise.api.dto.response.OtpValidationResponse;
import com.enterprise.api.entity.Otp;
import com.enterprise.api.entity.OtpPurpose;
import com.enterprise.api.entity.OtpType;
import com.enterprise.api.repository.OtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OtpServiceImpl.
 * Tests OTP generation, validation, rate limiting, and lifecycle management.
 */
@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.enterprise.api.service.external.EmailServiceClient emailServiceClient;

    @Mock
    private com.enterprise.api.service.external.SmsServiceClient smsServiceClient;

    @InjectMocks
    private OtpServiceImpl otpService;

    private OtpGenerationRequest generationRequest;
    private OtpValidationRequest validationRequest;
    private Otp mockOtp;

    @BeforeEach
    void setUp() {
        // Set up configuration properties
        ReflectionTestUtils.setField(otpService, "defaultExpirationMinutes", 5);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 3);
        ReflectionTestUtils.setField(otpService, "rateLimitWindowMinutes", 15);
        ReflectionTestUtils.setField(otpService, "maxRequestsPerWindow", 5);
        ReflectionTestUtils.setField(otpService, "lockDurationMinutes", 30);
        ReflectionTestUtils.setField(otpService, "maxFailedAttempts", 10);
        ReflectionTestUtils.setField(otpService, "codeLength", 6);
        ReflectionTestUtils.setField(otpService, "numericOnly", true);

        // Mock external service calls to return success (lenient to avoid unnecessary stubbing)
        lenient().when(emailServiceClient.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);
        lenient().when(smsServiceClient.sendSms(anyString(), anyString())).thenReturn(true);

        // Set up test data
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

        mockOtp = new Otp(
            "test@example.com",
            "hashedCode",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            LocalDateTime.now().plusMinutes(5),
            "127.0.0.1",
            "Test-Agent"
        );
        mockOtp.setId(1L);
    }

    @Test
    void generateOtp_Success() {
        // Arrange
        when(otpRepository.countOtpsGeneratedSince(anyString(), any(LocalDateTime.class))).thenReturn(0L);
        when(otpRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class))).thenReturn(0L);
        when(otpRepository.invalidateOtpsForIdentifierAndPurpose(anyString(), any(OtpPurpose.class), any(LocalDateTime.class))).thenReturn(0);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedCode");
        when(otpRepository.save(any(Otp.class))).thenReturn(mockOtp);

        // Act
        OtpGenerationResponse response = otpService.generateOtp(generationRequest, "127.0.0.1", "Test-Agent");

        // Assert
        assertNotNull(response);
        assertEquals("1", response.getOtpId());
        assertEquals("te****@example.com", response.getMaskedIdentifier());
        assertEquals(OtpType.EMAIL, response.getType());
        assertEquals(OtpPurpose.LOGIN, response.getPurpose());
        assertTrue(response.isDelivered());
        assertEquals(3, response.getMaxAttempts());

        verify(otpRepository).invalidateOtpsForIdentifierAndPurpose(eq("test@example.com"), eq(OtpPurpose.LOGIN), any(LocalDateTime.class));
        verify(otpRepository).save(any(Otp.class));
    }

    @Test
    void generateOtp_RateLimited() {
        // Arrange
        when(otpRepository.countOtpsGeneratedSince(anyString(), any(LocalDateTime.class))).thenReturn(6L);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            otpService.generateOtp(generationRequest, "127.0.0.1", "Test-Agent")
        );

        assertTrue(exception.getMessage().contains("Rate limit exceeded"));
        verify(otpRepository, never()).save(any(Otp.class));
    }

    @Test
    void generateOtp_AccountLocked() {
        // Arrange
        when(otpRepository.countOtpsGeneratedSince(anyString(), any(LocalDateTime.class))).thenReturn(0L);
        when(otpRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class))).thenReturn(15L);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            otpService.generateOtp(generationRequest, "127.0.0.1", "Test-Agent")
        );

        assertTrue(exception.getMessage().contains("temporarily locked"));
        verify(otpRepository, never()).save(any(Otp.class));
    }

    @Test
    void generateOtp_InvalidRequest() {
        // Arrange
        OtpGenerationRequest invalidRequest = new OtpGenerationRequest();
        invalidRequest.setIdentifier("");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            otpService.generateOtp(invalidRequest, "127.0.0.1", "Test-Agent")
        );

        assertTrue(exception.getMessage().contains("Identifier cannot be null or empty"));
    }

    @Test
    void validateOtp_Success() {
        // Arrange
        when(otpRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class))).thenReturn(0L);
        when(otpRepository.findValidOtpByIdentifierAndPurpose(anyString(), any(OtpPurpose.class), any(LocalDateTime.class)))
            .thenReturn(Optional.of(mockOtp));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(otpRepository.save(any(Otp.class))).thenReturn(mockOtp);

        // Act
        OtpValidationResponse response = otpService.validateOtp(validationRequest, "127.0.0.1");

        // Assert
        assertNotNull(response);
        assertTrue(response.isValid());
        assertTrue(response.isConsumed());
        assertNull(response.getFailureReason());

        verify(otpRepository).save(any(Otp.class));
    }

    @Test
    void validateOtp_InvalidCode() {
        // Arrange
        when(otpRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class))).thenReturn(0L);
        when(otpRepository.findValidOtpByIdentifierAndPurpose(anyString(), any(OtpPurpose.class), any(LocalDateTime.class)))
            .thenReturn(Optional.of(mockOtp));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(otpRepository.save(any(Otp.class))).thenReturn(mockOtp);

        // Act
        OtpValidationResponse response = otpService.validateOtp(validationRequest, "127.0.0.1");

        // Assert
        assertNotNull(response);
        assertFalse(response.isValid());
        assertFalse(response.isConsumed());
        assertEquals("Invalid OTP code", response.getFailureReason());
        assertEquals(2, response.getRemainingAttempts()); // 3 max - 1 attempt = 2 remaining

        verify(otpRepository).save(any(Otp.class));
    }

    @Test
    void validateOtp_NoValidOtpFound() {
        // Arrange
        when(otpRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class))).thenReturn(0L);
        when(otpRepository.findValidOtpByIdentifierAndPurpose(anyString(), any(OtpPurpose.class), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        // Act
        OtpValidationResponse response = otpService.validateOtp(validationRequest, "127.0.0.1");

        // Assert
        assertNotNull(response);
        assertFalse(response.isValid());
        assertEquals("No valid OTP found", response.getFailureReason());

        verify(otpRepository, never()).save(any(Otp.class));
    }

    @Test
    void validateOtp_AccountLocked() {
        // Arrange
        when(otpRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class))).thenReturn(15L);

        // Act
        OtpValidationResponse response = otpService.validateOtp(validationRequest, "127.0.0.1");

        // Assert
        assertNotNull(response);
        assertFalse(response.isValid());
        assertTrue(response.isLocked());
        assertNotNull(response.getLockExpiresAt());
        assertEquals("TOO_MANY_ATTEMPTS", response.getFailureReason());

        verify(otpRepository, never()).findValidOtpByIdentifierAndPurpose(anyString(), any(OtpPurpose.class), any(LocalDateTime.class));
    }

    @Test
    void validateOtp_MaxAttemptsExceeded() {
        // Arrange
        mockOtp.setAttempts(3); // Max attempts reached
        when(otpRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class))).thenReturn(0L);
        when(otpRepository.findValidOtpByIdentifierAndPurpose(anyString(), any(OtpPurpose.class), any(LocalDateTime.class)))
            .thenReturn(Optional.of(mockOtp));

        // Act
        OtpValidationResponse response = otpService.validateOtp(validationRequest, "127.0.0.1");

        // Assert
        assertNotNull(response);
        assertFalse(response.isValid());
        assertEquals("Maximum attempts exceeded", response.getFailureReason());
        assertEquals(0, response.getRemainingAttempts());

        verify(otpRepository, never()).save(any(Otp.class));
    }

    @Test
    void validateOtp_WithoutConsumption() {
        // Arrange
        validationRequest.setConsumeOnValidation(false);
        when(otpRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class))).thenReturn(0L);
        when(otpRepository.findValidOtpByIdentifierAndPurpose(anyString(), any(OtpPurpose.class), any(LocalDateTime.class)))
            .thenReturn(Optional.of(mockOtp));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // Act
        OtpValidationResponse response = otpService.validateOtp(validationRequest, "127.0.0.1");

        // Assert
        assertNotNull(response);
        assertTrue(response.isValid());
        assertFalse(response.isConsumed());

        verify(otpRepository, never()).save(any(Otp.class));
    }

    @Test
    void isRateLimited_True() {
        // Arrange
        when(otpRepository.countOtpsGeneratedSince(anyString(), any(LocalDateTime.class))).thenReturn(6L);

        // Act
        boolean result = otpService.isRateLimited("test@example.com");

        // Assert
        assertTrue(result);
    }

    @Test
    void isRateLimited_False() {
        // Arrange
        when(otpRepository.countOtpsGeneratedSince(anyString(), any(LocalDateTime.class))).thenReturn(3L);

        // Act
        boolean result = otpService.isRateLimited("test@example.com");

        // Assert
        assertFalse(result);
    }

    @Test
    void isLocked_True() {
        // Arrange
        when(otpRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class))).thenReturn(15L);

        // Act
        boolean result = otpService.isLocked("test@example.com");

        // Assert
        assertTrue(result);
    }

    @Test
    void isLocked_False() {
        // Arrange
        when(otpRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class))).thenReturn(5L);

        // Act
        boolean result = otpService.isLocked("test@example.com");

        // Assert
        assertFalse(result);
    }

    @Test
    void invalidateOtps() {
        // Arrange
        when(otpRepository.invalidateOtpsForIdentifierAndPurpose(anyString(), any(OtpPurpose.class), any(LocalDateTime.class)))
            .thenReturn(2);

        // Act
        int result = otpService.invalidateOtps("test@example.com", OtpPurpose.LOGIN);

        // Assert
        assertEquals(2, result);
        verify(otpRepository).invalidateOtpsForIdentifierAndPurpose(eq("test@example.com"), eq(OtpPurpose.LOGIN), any(LocalDateTime.class));
    }

    @Test
    void cleanupExpiredOtps() {
        // Arrange
        when(otpRepository.softDeleteExpiredOtps(any(LocalDateTime.class))).thenReturn(5);

        // Act
        int result = otpService.cleanupExpiredOtps();

        // Assert
        assertEquals(5, result);
        verify(otpRepository).softDeleteExpiredOtps(any(LocalDateTime.class));
    }

    @Test
    void getRateLimitResetTime_NotRateLimited() {
        // Arrange
        when(otpRepository.countOtpsGeneratedSince(anyString(), any(LocalDateTime.class))).thenReturn(3L);

        // Act
        long result = otpService.getRateLimitResetTime("test@example.com");

        // Assert
        assertEquals(0, result);
    }

    @Test
    void getRateLimitResetTime_RateLimited() {
        // Arrange
        when(otpRepository.countOtpsGeneratedSince(anyString(), any(LocalDateTime.class))).thenReturn(6L);

        // Act
        long result = otpService.getRateLimitResetTime("test@example.com");

        // Assert
        assertEquals(900, result); // 15 minutes * 60 seconds
    }

    @Test
    void getLockResetTime_NotLocked() {
        // Arrange
        when(otpRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class))).thenReturn(5L);

        // Act
        long result = otpService.getLockResetTime("test@example.com");

        // Assert
        assertEquals(0, result);
    }

    @Test
    void getLockResetTime_Locked() {
        // Arrange
        when(otpRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class))).thenReturn(15L);

        // Act
        long result = otpService.getLockResetTime("test@example.com");

        // Assert
        assertEquals(1800, result); // 30 minutes * 60 seconds
    }

    @Test
    void hasValidOtps_True() {
        // Arrange
        when(otpRepository.hasValidOtps(anyString(), any(LocalDateTime.class))).thenReturn(true);

        // Act
        boolean result = otpService.hasValidOtps("test@example.com");

        // Assert
        assertTrue(result);
    }

    @Test
    void hasValidOtps_False() {
        // Arrange
        when(otpRepository.hasValidOtps(anyString(), any(LocalDateTime.class))).thenReturn(false);

        // Act
        boolean result = otpService.hasValidOtps("test@example.com");

        // Assert
        assertFalse(result);
    }

    @Test
    void generateTestOtp() {
        // Act
        String result = otpService.generateTestOtp("test@example.com", OtpType.EMAIL, OtpPurpose.LOGIN);

        // Assert
        assertNotNull(result);
        assertEquals(6, result.length());
        assertTrue(result.matches("\\d+"));
    }

    @Test
    void generateOtp_CustomExpirationMinutes() {
        // Arrange
        generationRequest.setExpirationMinutes(10);
        when(otpRepository.countOtpsGeneratedSince(anyString(), any(LocalDateTime.class))).thenReturn(0L);
        when(otpRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class))).thenReturn(0L);
        when(otpRepository.invalidateOtpsForIdentifierAndPurpose(anyString(), any(OtpPurpose.class), any(LocalDateTime.class))).thenReturn(0);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedCode");
        when(otpRepository.save(any(Otp.class))).thenReturn(mockOtp);

        // Act
        OtpGenerationResponse response = otpService.generateOtp(generationRequest, "127.0.0.1", "Test-Agent");

        // Assert
        assertNotNull(response);
        verify(otpRepository).save(argThat(otp -> 
            otp.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(9)) &&
            otp.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(11))
        ));
    }

    @Test
    void generateOtp_SmsType() {
        // Arrange
        generationRequest.setType(OtpType.SMS);
        generationRequest.setIdentifier("+1234567890");
        when(otpRepository.countOtpsGeneratedSince(anyString(), any(LocalDateTime.class))).thenReturn(0L);
        when(otpRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class))).thenReturn(0L);
        when(otpRepository.invalidateOtpsForIdentifierAndPurpose(anyString(), any(OtpPurpose.class), any(LocalDateTime.class))).thenReturn(0);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedCode");
        when(otpRepository.save(any(Otp.class))).thenReturn(mockOtp);

        // Act
        OtpGenerationResponse response = otpService.generateOtp(generationRequest, "127.0.0.1", "Test-Agent");

        // Assert
        assertNotNull(response);
        assertEquals(OtpType.SMS, response.getType());
        assertEquals("SMS", response.getDeliveryMethod());
    }

    @Test
    void generateOtp_WithCustomMessage() {
        // Arrange
        generationRequest.setCustomMessage("Your verification code is: {code}");
        when(otpRepository.countOtpsGeneratedSince(anyString(), any(LocalDateTime.class))).thenReturn(0L);
        when(otpRepository.countFailedAttemptsSince(anyString(), any(LocalDateTime.class))).thenReturn(0L);
        when(otpRepository.invalidateOtpsForIdentifierAndPurpose(anyString(), any(OtpPurpose.class), any(LocalDateTime.class))).thenReturn(0);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedCode");
        when(otpRepository.save(any(Otp.class))).thenReturn(mockOtp);

        // Act
        OtpGenerationResponse response = otpService.generateOtp(generationRequest, "127.0.0.1", "Test-Agent");

        // Assert
        assertNotNull(response);
        assertTrue(response.isDelivered());
    }
}