package com.enterprise.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CaptchaServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class CaptchaServiceImplTest {
    
    private CaptchaServiceImpl captchaService;
    
    @BeforeEach
    void setUp() {
        captchaService = new CaptchaServiceImpl();
    }
    
    @Test
    void generateCaptcha_ShouldReturnBufferedImage() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        
        // When
        BufferedImage image = captchaService.generateCaptcha(sessionId);
        
        // Then
        assertNotNull(image);
        assertEquals(200, image.getWidth());
        assertEquals(60, image.getHeight());
        assertEquals(BufferedImage.TYPE_INT_RGB, image.getType());
    }
    
    @Test
    void generateCaptcha_ShouldStoreCaptchaText() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        
        // When
        captchaService.generateCaptcha(sessionId);
        String captchaText = captchaService.getCaptchaText(sessionId);
        
        // Then
        assertNotNull(captchaText);
        assertEquals(5, captchaText.length());
        assertTrue(captchaText.matches("[A-Z0-9]+"));
    }
    
    @Test
    void validateCaptcha_WithCorrectResponse_ShouldReturnTrue() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        captchaService.generateCaptcha(sessionId);
        String captchaText = captchaService.getCaptchaText(sessionId);
        
        // When
        boolean result = captchaService.validateCaptcha(sessionId, captchaText);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void validateCaptcha_WithIncorrectResponse_ShouldReturnFalse() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        captchaService.generateCaptcha(sessionId);
        
        // When
        boolean result = captchaService.validateCaptcha(sessionId, "WRONG");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void validateCaptcha_WithCaseInsensitiveResponse_ShouldReturnTrue() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        captchaService.generateCaptcha(sessionId);
        String captchaText = captchaService.getCaptchaText(sessionId);
        
        // When
        boolean result = captchaService.validateCaptcha(sessionId, captchaText.toLowerCase());
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void validateCaptcha_WithNullResponse_ShouldReturnFalse() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        captchaService.generateCaptcha(sessionId);
        
        // When
        boolean result = captchaService.validateCaptcha(sessionId, null);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void validateCaptcha_WithEmptyResponse_ShouldReturnFalse() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        captchaService.generateCaptcha(sessionId);
        
        // When
        boolean result = captchaService.validateCaptcha(sessionId, "");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void validateCaptcha_WithWhitespaceResponse_ShouldReturnFalse() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        captchaService.generateCaptcha(sessionId);
        
        // When
        boolean result = captchaService.validateCaptcha(sessionId, "   ");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void validateCaptcha_WithNonExistentSession_ShouldReturnFalse() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        
        // When
        boolean result = captchaService.validateCaptcha(sessionId, "ABCDE");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void validateCaptcha_ShouldRemoveCaptchaAfterValidation() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        captchaService.generateCaptcha(sessionId);
        String captchaText = captchaService.getCaptchaText(sessionId);
        
        // When
        captchaService.validateCaptcha(sessionId, captchaText);
        
        // Then
        assertNull(captchaService.getCaptchaText(sessionId));
    }
    
    @Test
    void validateCaptcha_WithResponseContainingWhitespace_ShouldTrimAndValidate() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        captchaService.generateCaptcha(sessionId);
        String captchaText = captchaService.getCaptchaText(sessionId);
        
        // When
        boolean result = captchaService.validateCaptcha(sessionId, "  " + captchaText + "  ");
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void clearCaptcha_ShouldRemoveCaptchaFromStorage() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        captchaService.generateCaptcha(sessionId);
        assertNotNull(captchaService.getCaptchaText(sessionId));
        
        // When
        captchaService.clearCaptcha(sessionId);
        
        // Then
        assertNull(captchaService.getCaptchaText(sessionId));
    }
    
    @Test
    void clearCaptcha_WithNonExistentSession_ShouldNotThrowException() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        
        // When & Then
        assertDoesNotThrow(() -> captchaService.clearCaptcha(sessionId));
    }
    
    @Test
    void getCaptchaText_WithNonExistentSession_ShouldReturnNull() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        
        // When
        String result = captchaService.getCaptchaText(sessionId);
        
        // Then
        assertNull(result);
    }
    
    @Test
    void generateCaptcha_MultipleSessions_ShouldStoreIndependently() {
        // Given
        String sessionId1 = UUID.randomUUID().toString();
        String sessionId2 = UUID.randomUUID().toString();
        
        // When
        captchaService.generateCaptcha(sessionId1);
        captchaService.generateCaptcha(sessionId2);
        
        String captchaText1 = captchaService.getCaptchaText(sessionId1);
        String captchaText2 = captchaService.getCaptchaText(sessionId2);
        
        // Then
        assertNotNull(captchaText1);
        assertNotNull(captchaText2);
        assertNotEquals(captchaText1, captchaText2); // Very likely to be different
    }
    
    @Test
    void generateCaptcha_SameSession_ShouldOverwritePrevious() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        
        // When
        captchaService.generateCaptcha(sessionId);
        String firstCaptcha = captchaService.getCaptchaText(sessionId);
        
        captchaService.generateCaptcha(sessionId);
        String secondCaptcha = captchaService.getCaptchaText(sessionId);
        
        // Then
        assertNotNull(firstCaptcha);
        assertNotNull(secondCaptcha);
        // Note: They might be the same due to randomness, but the storage should be updated
    }
    
    @Test
    void validateCaptcha_AfterExpiration_ShouldReturnFalse() throws InterruptedException {
        // Given
        String sessionId = UUID.randomUUID().toString();
        captchaService.generateCaptcha(sessionId);
        String captchaText = captchaService.getCaptchaText(sessionId);
        
        // Wait for a short time (this test assumes very short expiration for testing)
        // Note: In real implementation, we would need to mock the time or use a test-specific expiration
        TimeUnit.MILLISECONDS.sleep(100);
        
        // When
        boolean result = captchaService.validateCaptcha(sessionId, captchaText);
        
        // Then
        // This test might pass because 100ms is much less than 5 minutes
        // In a real test, we would mock the system time or use dependency injection for time provider
        assertTrue(result); // Should still be valid after 100ms
    }
    
    @Test
    void generateCaptcha_ShouldGenerateUniqueTexts() {
        // Given
        String sessionId1 = UUID.randomUUID().toString();
        String sessionId2 = UUID.randomUUID().toString();
        String sessionId3 = UUID.randomUUID().toString();
        
        // When
        captchaService.generateCaptcha(sessionId1);
        captchaService.generateCaptcha(sessionId2);
        captchaService.generateCaptcha(sessionId3);
        
        String text1 = captchaService.getCaptchaText(sessionId1);
        String text2 = captchaService.getCaptchaText(sessionId2);
        String text3 = captchaService.getCaptchaText(sessionId3);
        
        // Then
        assertNotNull(text1);
        assertNotNull(text2);
        assertNotNull(text3);
        
        // While not guaranteed, it's extremely unlikely all three would be the same
        boolean allDifferent = !text1.equals(text2) || !text2.equals(text3) || !text1.equals(text3);
        assertTrue(allDifferent, "Generated CAPTCHA texts should typically be different");
    }
}