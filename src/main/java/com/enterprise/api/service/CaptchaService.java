package com.enterprise.api.service;

import java.awt.image.BufferedImage;

/**
 * Service interface for CAPTCHA generation and validation.
 * Provides methods to create visual challenges and validate user responses.
 */
public interface CaptchaService {
    
    /**
     * Generates a new CAPTCHA challenge and stores it in session.
     * 
     * @param sessionId the session identifier to associate with the CAPTCHA
     * @return BufferedImage containing the visual CAPTCHA challenge
     */
    BufferedImage generateCaptcha(String sessionId);
    
    /**
     * Validates a CAPTCHA response against the stored challenge.
     * 
     * @param sessionId the session identifier
     * @param response the user's response to the CAPTCHA challenge
     * @return true if the response is correct, false otherwise
     */
    boolean validateCaptcha(String sessionId, String response);
    
    /**
     * Removes the CAPTCHA challenge from session storage.
     * 
     * @param sessionId the session identifier
     */
    void clearCaptcha(String sessionId);
    
    /**
     * Gets the current CAPTCHA text for a session (for testing purposes).
     * 
     * @param sessionId the session identifier
     * @return the CAPTCHA text or null if not found
     */
    String getCaptchaText(String sessionId);
}