package com.enterprise.api.controller;

import com.enterprise.api.dto.request.OtpGenerationRequest;
import com.enterprise.api.dto.request.OtpValidationRequest;
import com.enterprise.api.dto.response.ErrorResponse;
import com.enterprise.api.dto.response.OtpGenerationResponse;
import com.enterprise.api.dto.response.OtpValidationResponse;
import com.enterprise.api.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for OTP (One-Time Password) operations.
 * Provides endpoints for generating and validating OTPs with rate limiting.
 */
@Tag(name = "OTP (One-Time Password)", description = "OTP generation and validation for secure authentication workflows")
@RestController
@RequestMapping("/api/v1/otp")
public class OtpController {

    private static final Logger logger = LoggerFactory.getLogger(OtpController.class);

    private final OtpService otpService;

    @Autowired
    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    /**
     * Generate and send an OTP.
     * 
     * @param request the OTP generation request
     * @param httpRequest the HTTP request for extracting client info
     * @return response containing OTP generation details
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateOtp(@Valid @RequestBody OtpGenerationRequest request,
                                        HttpServletRequest httpRequest) {
        try {
            String clientIp = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            logger.info("OTP generation request received for identifier: {} with purpose: {}", 
                       maskIdentifier(request.getIdentifier()), request.getPurpose());

            OtpGenerationResponse response = otpService.generateOtp(request, clientIp, userAgent);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid OTP generation request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("INVALID_REQUEST", e.getMessage()));
                    
        } catch (IllegalStateException e) {
            logger.warn("OTP generation failed due to rate limiting or lock: {}", e.getMessage());
            
            if (e.getMessage().contains("Rate limit")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(createErrorResponse("RATE_LIMITED", e.getMessage()));
            } else if (e.getMessage().contains("locked")) {
                return ResponseEntity.status(HttpStatus.LOCKED)
                        .body(createErrorResponse("ACCOUNT_LOCKED", e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(createErrorResponse("GENERATION_FAILED", e.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during OTP generation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }

    /**
     * Validate an OTP code.
     * 
     * @param request the OTP validation request
     * @param httpRequest the HTTP request for extracting client info
     * @return response containing validation result
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateOtp(@Valid @RequestBody OtpValidationRequest request,
                                        HttpServletRequest httpRequest) {
        try {
            String clientIp = getClientIpAddress(httpRequest);

            logger.info("OTP validation request received for identifier: {} with purpose: {}", 
                       maskIdentifier(request.getIdentifier()), request.getPurpose());

            OtpValidationResponse response = otpService.validateOtp(request, clientIp);
            
            if (response.isValid()) {
                return ResponseEntity.ok(response);
            } else if (response.isLocked()) {
                return ResponseEntity.status(HttpStatus.LOCKED).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid OTP validation request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("INVALID_REQUEST", e.getMessage()));
                    
        } catch (Exception e) {
            logger.error("Unexpected error during OTP validation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }

    /**
     * Check if an identifier is rate limited.
     * 
     * @param identifier the identifier to check
     * @return rate limit status
     */
    @GetMapping("/rate-limit/{identifier}")
    public ResponseEntity<Map<String, Object>> checkRateLimit(@PathVariable String identifier) {
        try {
            boolean rateLimited = otpService.isRateLimited(identifier);
            long resetTime = otpService.getRateLimitResetTime(identifier);
            
            Map<String, Object> response = new HashMap<>();
            response.put("rateLimited", rateLimited);
            response.put("resetTimeSeconds", resetTime);
            response.put("identifier", maskIdentifier(identifier));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking rate limit for identifier: {}", maskIdentifier(identifier), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }

    /**
     * Check if an identifier is locked.
     * 
     * @param identifier the identifier to check
     * @return lock status
     */
    @GetMapping("/lock-status/{identifier}")
    public ResponseEntity<Map<String, Object>> checkLockStatus(@PathVariable String identifier) {
        try {
            boolean locked = otpService.isLocked(identifier);
            long resetTime = otpService.getLockResetTime(identifier);
            
            Map<String, Object> response = new HashMap<>();
            response.put("locked", locked);
            response.put("resetTimeSeconds", resetTime);
            response.put("identifier", maskIdentifier(identifier));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking lock status for identifier: {}", maskIdentifier(identifier), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }

    /**
     * Invalidate all OTPs for a given identifier and purpose.
     * This endpoint requires authentication and appropriate permissions.
     * 
     * @param identifier the identifier
     * @param purpose the purpose
     * @return number of OTPs invalidated
     */
    @DeleteMapping("/invalidate/{identifier}/{purpose}")
    public ResponseEntity<Map<String, Object>> invalidateOtps(@PathVariable String identifier,
                                                             @PathVariable String purpose) {
        try {
            // Convert string to enum
            com.enterprise.api.entity.OtpPurpose otpPurpose = 
                com.enterprise.api.entity.OtpPurpose.valueOf(purpose.toUpperCase());
            
            int invalidatedCount = otpService.invalidateOtps(identifier, otpPurpose);
            
            Map<String, Object> response = new HashMap<>();
            response.put("invalidatedCount", invalidatedCount);
            response.put("identifier", maskIdentifier(identifier));
            response.put("purpose", otpPurpose);
            response.put("message", "OTPs invalidated successfully");
            
            logger.info("Invalidated {} OTPs for identifier: {} with purpose: {}", 
                       invalidatedCount, maskIdentifier(identifier), otpPurpose);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid purpose provided: {}", purpose);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("INVALID_PURPOSE", "Invalid OTP purpose: " + purpose));
                    
        } catch (Exception e) {
            logger.error("Error invalidating OTPs for identifier: {}", maskIdentifier(identifier), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }

    /**
     * Clean up expired OTPs (admin endpoint).
     * This endpoint requires admin authentication.
     * 
     * @return number of expired OTPs cleaned up
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupExpiredOtps() {
        try {
            int cleanedUpCount = otpService.cleanupExpiredOtps();
            
            Map<String, Object> response = new HashMap<>();
            response.put("cleanedUpCount", cleanedUpCount);
            response.put("message", "Expired OTPs cleaned up successfully");
            
            logger.info("Cleaned up {} expired OTPs", cleanedUpCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error cleaning up expired OTPs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }

    /**
     * Handle OPTIONS requests for CORS preflight.
     */
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .build();
    }

    /**
     * Handle HEAD requests.
     */
    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<Void> handleHead() {
        return ResponseEntity.ok().build();
    }

    // Private helper methods

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() <= 4) {
            return "****";
        }
        
        if (identifier.contains("@")) {
            // Email masking
            String[] parts = identifier.split("@");
            String localPart = parts[0];
            String domain = parts[1];
            
            if (localPart.length() <= 2) {
                return "**@" + domain;
            } else {
                return localPart.substring(0, 2) + "****@" + domain;
            }
        } else {
            // Phone or other identifier masking
            return identifier.substring(0, 2) + "****" + identifier.substring(Math.max(2, identifier.length() - 2));
        }
    }

    private Map<String, Object> createErrorResponse(String errorCode, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", errorCode);
        error.put("message", message);
        error.put("timestamp", java.time.LocalDateTime.now());
        return error;
    }
}