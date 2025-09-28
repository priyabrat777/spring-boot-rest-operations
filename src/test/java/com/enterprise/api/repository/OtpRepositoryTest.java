package com.enterprise.api.repository;

import com.enterprise.api.entity.Otp;
import com.enterprise.api.entity.OtpPurpose;
import com.enterprise.api.entity.OtpType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for OtpRepository.
 * Tests OTP repository operations with actual database interactions.
 */
@DataJpaTest
class OtpRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OtpRepository otpRepository;

    private Otp validOtp;
    private Otp expiredOtp;
    private Otp usedOtp;
    private LocalDateTime now;
    private LocalDateTime future;
    private LocalDateTime past;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        future = now.plusMinutes(5);
        past = now.minusMinutes(5);

        // Create valid OTP
        validOtp = new Otp(
            "test@example.com",
            "hashedCode1",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            future,
            "127.0.0.1",
            "Test-Agent"
        );

        // Create expired OTP
        expiredOtp = new Otp(
            "test@example.com",
            "hashedCode2",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            past,
            "127.0.0.1",
            "Test-Agent"
        );

        // Create used OTP
        usedOtp = new Otp(
            "test@example.com",
            "hashedCode3",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            future,
            "127.0.0.1",
            "Test-Agent"
        );
        usedOtp.setUsed(true);

        // Persist entities
        entityManager.persistAndFlush(validOtp);
        entityManager.persistAndFlush(expiredOtp);
        entityManager.persistAndFlush(usedOtp);
    }

    @Test
    void findValidOtpByIdentifierAndPurpose_ValidOtp() {
        // Act
        Optional<Otp> result = otpRepository.findValidOtpByIdentifierAndPurpose(
            "test@example.com", OtpPurpose.LOGIN, now);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(validOtp.getId(), result.get().getId());
        assertEquals("hashedCode1", result.get().getCode());
    }

    @Test
    void findValidOtpByIdentifierAndPurpose_NoValidOtp() {
        // Act
        Optional<Otp> result = otpRepository.findValidOtpByIdentifierAndPurpose(
            "nonexistent@example.com", OtpPurpose.LOGIN, now);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void findValidOtpByIdentifierAndPurpose_DifferentPurpose() {
        // Act
        Optional<Otp> result = otpRepository.findValidOtpByIdentifierAndPurpose(
            "test@example.com", OtpPurpose.PASSWORD_RESET, now);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void findValidOtpsByIdentifier() {
        // Act
        List<Otp> result = otpRepository.findValidOtpsByIdentifier("test@example.com", now);

        // Assert
        assertEquals(1, result.size());
        assertEquals(validOtp.getId(), result.get(0).getId());
    }

    @Test
    void findByIdentifierAndPurposeOrderByCreatedDateDesc() {
        // Act
        List<Otp> result = otpRepository.findByIdentifierAndPurposeOrderByCreatedDateDesc(
            "test@example.com", OtpPurpose.LOGIN);

        // Assert
        assertEquals(3, result.size()); // All OTPs for this identifier and purpose
        // Should be ordered by created date desc (most recent first)
    }

    @Test
    void findExpiredOtps() {
        // Act
        List<Otp> result = otpRepository.findExpiredOtps(now);

        // Assert
        assertEquals(1, result.size());
        assertEquals(expiredOtp.getId(), result.get(0).getId());
    }

    @Test
    void countOtpsGeneratedSince() {
        // Arrange
        LocalDateTime since = now.minusHours(1);

        // Act
        long count = otpRepository.countOtpsGeneratedSince("test@example.com", since);

        // Assert
        assertEquals(3, count); // All 3 OTPs were created after 'since'
    }

    @Test
    void countOtpsGeneratedSince_NoOtps() {
        // Arrange
        LocalDateTime since = now.plusHours(1); // Future time

        // Act
        long count = otpRepository.countOtpsGeneratedSince("test@example.com", since);

        // Assert
        assertEquals(0, count);
    }

    @Test
    void countFailedAttemptsSince() {
        // Arrange
        validOtp.setAttempts(2);
        usedOtp.setAttempts(3);
        entityManager.merge(validOtp);
        entityManager.merge(usedOtp);
        entityManager.flush();

        LocalDateTime since = now.minusHours(1);

        // Act
        long count = otpRepository.countFailedAttemptsSince("test@example.com", since);

        // Assert
        assertEquals(5, count); // 2 + 3 attempts
    }

    @Test
    void softDeleteExpiredOtps() {
        // Act
        int deletedCount = otpRepository.softDeleteExpiredOtps(now);

        // Assert
        assertEquals(1, deletedCount);

        // Verify the expired OTP is marked as deleted
        entityManager.refresh(expiredOtp);
        assertTrue(expiredOtp.isDeleted());
    }

    @Test
    void invalidateOtpsForIdentifierAndPurpose() {
        // Act
        int invalidatedCount = otpRepository.invalidateOtpsForIdentifierAndPurpose(
            "test@example.com", OtpPurpose.LOGIN, now);

        // Assert
        assertEquals(1, invalidatedCount); // Only the valid OTP should be invalidated

        // Verify the valid OTP is marked as used
        entityManager.refresh(validOtp);
        assertTrue(validOtp.isUsed());
    }

    @Test
    void findByTypeAndCreatedDateBetween() {
        // Arrange
        LocalDateTime from = now.minusHours(1);
        LocalDateTime to = now.plusHours(1);

        // Act
        List<Otp> result = otpRepository.findByTypeAndCreatedDateBetween(OtpType.EMAIL, from, to);

        // Assert
        assertEquals(3, result.size()); // All OTPs are EMAIL type and within date range
    }

    @Test
    void findByTypeAndCreatedDateBetween_DifferentType() {
        // Arrange
        LocalDateTime from = now.minusHours(1);
        LocalDateTime to = now.plusHours(1);

        // Act
        List<Otp> result = otpRepository.findByTypeAndCreatedDateBetween(OtpType.SMS, from, to);

        // Assert
        assertEquals(0, result.size()); // No SMS OTPs
    }

    @Test
    void hasValidOtps_True() {
        // Act
        boolean result = otpRepository.hasValidOtps("test@example.com", now);

        // Assert
        assertTrue(result);
    }

    @Test
    void hasValidOtps_False() {
        // Act
        boolean result = otpRepository.hasValidOtps("nonexistent@example.com", now);

        // Assert
        assertFalse(result);
    }

    @Test
    void complexScenario_MultipleIdentifiersAndPurposes() {
        // Arrange - Create OTPs for different identifiers and purposes
        Otp otp1 = new Otp("user1@example.com", "code1", OtpType.EMAIL, OtpPurpose.LOGIN, future, "127.0.0.1", "Agent");
        Otp otp2 = new Otp("user1@example.com", "code2", OtpType.SMS, OtpPurpose.PASSWORD_RESET, future, "127.0.0.1", "Agent");
        Otp otp3 = new Otp("user2@example.com", "code3", OtpType.EMAIL, OtpPurpose.LOGIN, future, "127.0.0.1", "Agent");

        entityManager.persistAndFlush(otp1);
        entityManager.persistAndFlush(otp2);
        entityManager.persistAndFlush(otp3);

        // Act & Assert - Find OTPs for user1 with LOGIN purpose
        Optional<Otp> loginOtp = otpRepository.findValidOtpByIdentifierAndPurpose(
            "user1@example.com", OtpPurpose.LOGIN, now);
        assertTrue(loginOtp.isPresent());
        assertEquals("code1", loginOtp.get().getCode());

        // Act & Assert - Find OTPs for user1 with PASSWORD_RESET purpose
        Optional<Otp> resetOtp = otpRepository.findValidOtpByIdentifierAndPurpose(
            "user1@example.com", OtpPurpose.PASSWORD_RESET, now);
        assertTrue(resetOtp.isPresent());
        assertEquals("code2", resetOtp.get().getCode());

        // Act & Assert - Count OTPs for user1
        long user1Count = otpRepository.countOtpsGeneratedSince("user1@example.com", now.minusHours(1));
        assertEquals(2, user1Count);

        // Act & Assert - Count OTPs for user2
        long user2Count = otpRepository.countOtpsGeneratedSince("user2@example.com", now.minusHours(1));
        assertEquals(1, user2Count);
    }

    @Test
    void testOtpLifecycle() {
        // Arrange - Create a fresh OTP
        Otp freshOtp = new Otp(
            "lifecycle@example.com",
            "lifecycleCode",
            OtpType.EMAIL,
            OtpPurpose.LOGIN,
            future,
            "127.0.0.1",
            "Test-Agent"
        );
        entityManager.persistAndFlush(freshOtp);

        // Act & Assert - Initially valid
        assertTrue(otpRepository.hasValidOtps("lifecycle@example.com", now));

        // Act - Increment attempts
        freshOtp.setAttempts(2);
        entityManager.merge(freshOtp);
        entityManager.flush();

        // Assert - Still valid with 2 attempts
        Optional<Otp> otpWithAttempts = otpRepository.findValidOtpByIdentifierAndPurpose(
            "lifecycle@example.com", OtpPurpose.LOGIN, now);
        assertTrue(otpWithAttempts.isPresent());
        assertEquals(2, otpWithAttempts.get().getAttempts());

        // Act - Reach max attempts
        freshOtp.setAttempts(3);
        entityManager.merge(freshOtp);
        entityManager.flush();

        // Assert - No longer valid due to max attempts
        Optional<Otp> maxAttemptsOtp = otpRepository.findValidOtpByIdentifierAndPurpose(
            "lifecycle@example.com", OtpPurpose.LOGIN, now);
        assertFalse(maxAttemptsOtp.isPresent());
    }

    @Test
    void testRateLimitingQueries() {
        // Arrange - Create multiple OTPs for rate limiting test
        LocalDateTime recentTime = now.minusMinutes(10);
        for (int i = 0; i < 5; i++) {
            Otp rateLimitOtp = new Otp(
                "ratelimit@example.com",
                "code" + i,
                OtpType.EMAIL,
                OtpPurpose.LOGIN,
                future,
                "127.0.0.1",
                "Test-Agent"
            );
            entityManager.persist(rateLimitOtp);
        }
        entityManager.flush();

        // Act & Assert - Count recent OTPs
        long recentCount = otpRepository.countOtpsGeneratedSince("ratelimit@example.com", recentTime);
        assertEquals(5, recentCount);

        // Act & Assert - Count OTPs from future (should be 0)
        long futureCount = otpRepository.countOtpsGeneratedSince("ratelimit@example.com", future);
        assertEquals(0, futureCount);
    }
}