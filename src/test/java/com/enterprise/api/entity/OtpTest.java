package com.enterprise.api.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Otp entity.
 * Tests OTP entity behavior, validation, and business logic.
 */
class OtpTest {

    private Otp otp;
    private LocalDateTime futureTime;
    private LocalDateTime pastTime;

    @BeforeEach
    void setUp() {
        futureTime = LocalDateTime.now().plusMinutes(5);
        pastTime = LocalDateTime.now().minusMinutes(5);
        
        otp = new Otp(
            "test@example.com",
            "hashedCode",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            futureTime,
            "127.0.0.1",
            "Test-Agent"
        );
    }

    @Test
    void constructor_WithAllParameters() {
        // Assert
        assertEquals("test@example.com", otp.getIdentifier());
        assertEquals("hashedCode", otp.getCode());
        assertEquals(OtpType.EMAIL, otp.getType());
        assertEquals(OtpPurpose.LOGIN, otp.getPurpose());
        assertEquals(futureTime, otp.getExpiresAt());
        assertEquals("127.0.0.1", otp.getRequestIpAddress());
        assertEquals("Test-Agent", otp.getRequestUserAgent());
        assertFalse(otp.isUsed());
        assertEquals(0, otp.getAttempts());
        assertEquals(3, otp.getMaxAttempts());
    }

    @Test
    void constructor_Default() {
        // Act
        Otp defaultOtp = new Otp();

        // Assert
        assertNull(defaultOtp.getIdentifier());
        assertNull(defaultOtp.getCode());
        assertNull(defaultOtp.getType());
        assertNull(defaultOtp.getPurpose());
        assertNull(defaultOtp.getExpiresAt());
        assertFalse(defaultOtp.isUsed());
        assertEquals(0, defaultOtp.getAttempts());
        assertEquals(3, defaultOtp.getMaxAttempts());
    }

    @Test
    void isExpired_NotExpired() {
        // Act & Assert
        assertFalse(otp.isExpired());
    }

    @Test
    void isExpired_Expired() {
        // Arrange
        otp.setExpiresAt(pastTime);

        // Act & Assert
        assertTrue(otp.isExpired());
    }

    @Test
    void isValid_ValidOtp() {
        // Act & Assert
        assertTrue(otp.isValid());
    }

    @Test
    void isValid_UsedOtp() {
        // Arrange
        otp.setUsed(true);

        // Act & Assert
        assertFalse(otp.isValid());
    }

    @Test
    void isValid_ExpiredOtp() {
        // Arrange
        otp.setExpiresAt(pastTime);

        // Act & Assert
        assertFalse(otp.isValid());
    }

    @Test
    void isValid_MaxAttemptsReached() {
        // Arrange
        otp.setAttempts(3);

        // Act & Assert
        assertFalse(otp.isValid());
    }

    @Test
    void isValid_DeletedOtp() {
        // Arrange
        otp.setDeleted(true);

        // Act & Assert
        assertFalse(otp.isValid());
    }

    @Test
    void incrementAttempts() {
        // Arrange
        assertEquals(0, otp.getAttempts());

        // Act
        otp.incrementAttempts();

        // Assert
        assertEquals(1, otp.getAttempts());
    }

    @Test
    void markAsUsed() {
        // Arrange
        assertFalse(otp.isUsed());

        // Act
        otp.markAsUsed();

        // Assert
        assertTrue(otp.isUsed());
    }

    @Test
    void canAttempt_WithinLimit() {
        // Arrange
        otp.setAttempts(2);

        // Act & Assert
        assertTrue(otp.canAttempt());
    }

    @Test
    void canAttempt_AtLimit() {
        // Arrange
        otp.setAttempts(3);

        // Act & Assert
        assertFalse(otp.canAttempt());
    }

    @Test
    void canAttempt_ExceedsLimit() {
        // Arrange
        otp.setAttempts(5);

        // Act & Assert
        assertFalse(otp.canAttempt());
    }

    @Test
    void settersAndGetters() {
        // Arrange
        Long id = 123L;
        String newIdentifier = "new@example.com";
        String newCode = "newHashedCode";
        OtpType newType = OtpType.SMS;
        OtpPurpose newPurpose = OtpPurpose.PASSWORD_RESET;
        LocalDateTime newExpiresAt = LocalDateTime.now().plusMinutes(10);
        boolean newUsed = true;
        int newAttempts = 2;
        int newMaxAttempts = 5;
        String newIpAddress = "192.168.1.1";
        String newUserAgent = "New-Agent";

        // Act
        otp.setId(id);
        otp.setIdentifier(newIdentifier);
        otp.setCode(newCode);
        otp.setType(newType);
        otp.setPurpose(newPurpose);
        otp.setExpiresAt(newExpiresAt);
        otp.setUsed(newUsed);
        otp.setAttempts(newAttempts);
        otp.setMaxAttempts(newMaxAttempts);
        otp.setRequestIpAddress(newIpAddress);
        otp.setRequestUserAgent(newUserAgent);

        // Assert
        assertEquals(id, otp.getId());
        assertEquals(newIdentifier, otp.getIdentifier());
        assertEquals(newCode, otp.getCode());
        assertEquals(newType, otp.getType());
        assertEquals(newPurpose, otp.getPurpose());
        assertEquals(newExpiresAt, otp.getExpiresAt());
        assertEquals(newUsed, otp.isUsed());
        assertEquals(newAttempts, otp.getAttempts());
        assertEquals(newMaxAttempts, otp.getMaxAttempts());
        assertEquals(newIpAddress, otp.getRequestIpAddress());
        assertEquals(newUserAgent, otp.getRequestUserAgent());
    }

    @Test
    void toString_ContainsExpectedFields() {
        // Arrange
        otp.setId(1L);

        // Act
        String result = otp.toString();

        // Assert
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("identifier='test@example.com'"));
        assertTrue(result.contains("type=Email"));
        assertTrue(result.contains("purpose=Login Authentication"));
        assertTrue(result.contains("used=false"));
        assertTrue(result.contains("attempts=0"));
        assertTrue(result.contains("maxAttempts=3"));
    }

    @Test
    void businessLogic_CompleteLifecycle() {
        // Arrange - Fresh OTP
        assertTrue(otp.isValid());
        assertTrue(otp.canAttempt());
        assertFalse(otp.isUsed());
        assertFalse(otp.isExpired());

        // Act - First failed attempt
        otp.incrementAttempts();

        // Assert - Still valid after first attempt
        assertTrue(otp.isValid());
        assertTrue(otp.canAttempt());
        assertEquals(1, otp.getAttempts());

        // Act - Second failed attempt
        otp.incrementAttempts();

        // Assert - Still valid after second attempt
        assertTrue(otp.isValid());
        assertTrue(otp.canAttempt());
        assertEquals(2, otp.getAttempts());

        // Act - Third failed attempt (max reached)
        otp.incrementAttempts();

        // Assert - No longer valid after max attempts
        assertFalse(otp.isValid());
        assertFalse(otp.canAttempt());
        assertEquals(3, otp.getAttempts());
    }

    @Test
    void businessLogic_SuccessfulValidation() {
        // Arrange - Fresh OTP
        assertTrue(otp.isValid());

        // Act - Successful validation
        otp.markAsUsed();

        // Assert - No longer valid after being used
        assertFalse(otp.isValid());
        assertTrue(otp.isUsed());
    }

    @Test
    void businessLogic_ExpiredOtp() {
        // Arrange - Fresh OTP
        assertTrue(otp.isValid());

        // Act - Time passes and OTP expires
        otp.setExpiresAt(pastTime);

        // Assert - No longer valid after expiration
        assertFalse(otp.isValid());
        assertTrue(otp.isExpired());
    }

    @Test
    void customMaxAttempts() {
        // Arrange
        otp.setMaxAttempts(5);

        // Act & Assert - Can attempt up to 5 times
        for (int i = 0; i < 5; i++) {
            assertTrue(otp.canAttempt());
            assertTrue(otp.isValid());
            otp.incrementAttempts();
        }

        // Assert - No longer valid after 5 attempts
        assertFalse(otp.canAttempt());
        assertFalse(otp.isValid());
        assertEquals(5, otp.getAttempts());
    }
}