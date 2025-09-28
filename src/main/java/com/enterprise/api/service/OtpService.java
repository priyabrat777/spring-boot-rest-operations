package com.enterprise.api.service;

import com.enterprise.api.dto.request.OtpGenerationRequest;
import com.enterprise.api.dto.request.OtpValidationRequest;
import com.enterprise.api.dto.response.OtpGenerationResponse;
import com.enterprise.api.dto.response.OtpValidationResponse;
import com.enterprise.api.entity.OtpPurpose;
import com.enterprise.api.entity.OtpType;

/**
 * Service interface for OTP (One-Time Password) operations.
 * Provides methods for generating, validating, and managing OTPs.
 */
public interface OtpService {

    /**
     * Generate and send an OTP for the specified identifier and purpose.
     * 
     * @param request the OTP generation request
     * @param clientIp the client IP address
     * @param userAgent the client user agent
     * @return response containing OTP generation details
     * @throws IllegalArgumentException if the request is invalid
     * @throws RuntimeException if OTP generation fails
     */
    OtpGenerationResponse generateOtp(OtpGenerationRequest request, String clientIp, String userAgent);

    /**
     * Validate an OTP code for the specified identifier and purpose.
     * 
     * @param request the OTP validation request
     * @param clientIp the client IP address
     * @return response containing validation result
     * @throws IllegalArgumentException if the request is invalid
     */
    OtpValidationResponse validateOtp(OtpValidationRequest request, String clientIp);

    /**
     * Check if an identifier is rate limited for OTP generation.
     * 
     * @param identifier the identifier to check
     * @return true if rate limited, false otherwise
     */
    boolean isRateLimited(String identifier);

    /**
     * Check if an identifier is temporarily locked due to too many failed attempts.
     * 
     * @param identifier the identifier to check
     * @return true if locked, false otherwise
     */
    boolean isLocked(String identifier);

    /**
     * Invalidate all OTPs for a given identifier and purpose.
     * 
     * @param identifier the identifier
     * @param purpose the purpose
     * @return number of OTPs invalidated
     */
    int invalidateOtps(String identifier, OtpPurpose purpose);

    /**
     * Clean up expired OTPs from the database.
     * 
     * @return number of expired OTPs cleaned up
     */
    int cleanupExpiredOtps();

    /**
     * Get the remaining time until rate limit reset for an identifier.
     * 
     * @param identifier the identifier
     * @return remaining time in seconds, or 0 if not rate limited
     */
    long getRateLimitResetTime(String identifier);

    /**
     * Get the remaining time until lock expires for an identifier.
     * 
     * @param identifier the identifier
     * @return remaining time in seconds, or 0 if not locked
     */
    long getLockResetTime(String identifier);

    /**
     * Check if the identifier has any valid OTPs.
     * 
     * @param identifier the identifier
     * @return true if valid OTPs exist
     */
    boolean hasValidOtps(String identifier);

    /**
     * Generate a simple OTP for testing purposes (without delivery).
     * This method should only be used in test environments.
     * 
     * @param identifier the identifier
     * @param type the OTP type
     * @param purpose the purpose
     * @return the generated OTP code (for testing only)
     */
    String generateTestOtp(String identifier, OtpType type, OtpPurpose purpose);
}