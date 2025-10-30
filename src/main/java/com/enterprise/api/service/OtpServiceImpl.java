package com.enterprise.api.service;

import com.enterprise.api.dto.request.OtpGenerationRequest;
import com.enterprise.api.dto.request.OtpValidationRequest;
import com.enterprise.api.dto.response.OtpGenerationResponse;
import com.enterprise.api.dto.response.OtpValidationResponse;
import com.enterprise.api.entity.Otp;
import com.enterprise.api.entity.OtpPurpose;
import com.enterprise.api.entity.OtpType;
import com.enterprise.api.repository.OtpRepository;
import com.enterprise.api.service.external.EmailServiceClient;
import com.enterprise.api.service.external.SmsServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementation of OTP service providing comprehensive OTP lifecycle management.
 * Handles generation, validation, rate limiting, and cleanup of OTPs.
 */
@Service
@Transactional
public class OtpServiceImpl implements OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpServiceImpl.class);

    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    private final EmailServiceClient emailServiceClient;
    private final SmsServiceClient smsServiceClient;

    // Configuration properties
    @Value("${app.otp.default-expiration-minutes:5}")
    private int defaultExpirationMinutes;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.otp.rate-limit-window-minutes:15}")
    private int rateLimitWindowMinutes;

    @Value("${app.otp.max-requests-per-window:5}")
    private int maxRequestsPerWindow;

    @Value("${app.otp.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    @Value("${app.otp.max-failed-attempts:10}")
    private int maxFailedAttempts;

    @Value("${app.otp.code-length:6}")
    private int codeLength;

    @Value("${app.otp.numeric-only:true}")
    private boolean numericOnly;

    @Autowired
    public OtpServiceImpl(OtpRepository otpRepository, PasswordEncoder passwordEncoder,
                         EmailServiceClient emailServiceClient, SmsServiceClient smsServiceClient) {
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailServiceClient = emailServiceClient;
        this.smsServiceClient = smsServiceClient;
        this.secureRandom = new SecureRandom();
    }

    @Override
    public OtpGenerationResponse generateOtp(OtpGenerationRequest request, String clientIp, String userAgent) {
        logger.info("Generating OTP for identifier: {} with purpose: {}", 
                   maskIdentifier(request.getIdentifier()), request.getPurpose());

        // Validate request
        validateGenerationRequest(request);

        // Check rate limiting
        if (isRateLimited(request.getIdentifier())) {
            throw new IllegalStateException("Rate limit exceeded for identifier: " + maskIdentifier(request.getIdentifier()));
        }

        // Check if identifier is locked
        if (isLocked(request.getIdentifier())) {
            throw new IllegalStateException("Identifier is temporarily locked: " + maskIdentifier(request.getIdentifier()));
        }

        // Invalidate existing OTPs for the same identifier and purpose
        invalidateOtps(request.getIdentifier(), request.getPurpose());

        // Generate OTP code
        String otpCode = generateOtpCode();
        String hashedCode = passwordEncoder.encode(otpCode);

        // Calculate expiration time
        int expirationMinutes = request.getExpirationMinutes() != null ? 
                               request.getExpirationMinutes() : defaultExpirationMinutes;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        // Create and save OTP entity
        Otp otp = new Otp(
            request.getIdentifier(),
            hashedCode,
            request.getType(),
            request.getPurpose(),
            expiresAt,
            clientIp,
            userAgent
        );
        otp.setMaxAttempts(maxAttempts);

        otp = otpRepository.save(otp);

        // Simulate OTP delivery
        boolean delivered = deliverOtp(request, otpCode);

        // Create response
        OtpGenerationResponse response = new OtpGenerationResponse(
            otp.getId().toString(),
            maskIdentifier(request.getIdentifier()),
            request.getType(),
            request.getPurpose(),
            expiresAt,
            maxAttempts,
            "OTP generated successfully",
            delivered,
            request.getType().getDisplayName()
        );

        logger.info("OTP generated successfully for identifier: {} with ID: {}", 
                   maskIdentifier(request.getIdentifier()), otp.getId());

        return response;
    }

    @Override
    public OtpValidationResponse validateOtp(OtpValidationRequest request, String clientIp) {
        logger.info("Validating OTP for identifier: {} with purpose: {}", 
                   maskIdentifier(request.getIdentifier()), request.getPurpose());

        // Check if identifier is locked
        if (isLocked(request.getIdentifier())) {
            return OtpValidationResponse.locked(LocalDateTime.now().plusMinutes(lockDurationMinutes));
        }

        // Find valid OTP
        Optional<Otp> otpOptional = otpRepository.findValidOtpByIdentifierAndPurpose(
            request.getIdentifier(), request.getPurpose(), LocalDateTime.now());

        if (otpOptional.isEmpty()) {
            logger.warn("No valid OTP found for identifier: {} with purpose: {}", 
                       maskIdentifier(request.getIdentifier()), request.getPurpose());
            return OtpValidationResponse.failure("No valid OTP found", null);
        }

        Otp otp = otpOptional.get();

        // Check if OTP can be attempted
        if (!otp.canAttempt()) {
            logger.warn("OTP attempt limit exceeded for identifier: {}", 
                       maskIdentifier(request.getIdentifier()));
            return OtpValidationResponse.failure("Maximum attempts exceeded", 0);
        }

        // Validate OTP code
        boolean isValid = passwordEncoder.matches(request.getCode(), otp.getCode());

        if (isValid) {
            // Mark as used if requested
            if (request.isConsumeOnValidation()) {
                otp.markAsUsed();
                otpRepository.save(otp);
            }

            logger.info("OTP validation successful for identifier: {}", 
                       maskIdentifier(request.getIdentifier()));
            return OtpValidationResponse.success(request.isConsumeOnValidation());
        } else {
            // Increment attempts
            otp.incrementAttempts();
            otpRepository.save(otp);

            int remainingAttempts = otp.getMaxAttempts() - otp.getAttempts();
            
            logger.warn("OTP validation failed for identifier: {}. Remaining attempts: {}", 
                       maskIdentifier(request.getIdentifier()), remainingAttempts);

            // Check if we should lock the identifier
            if (shouldLockIdentifier(request.getIdentifier())) {
                return OtpValidationResponse.locked(LocalDateTime.now().plusMinutes(lockDurationMinutes));
            }

            return OtpValidationResponse.failure("Invalid OTP code", remainingAttempts);
        }
    }

    @Override
    public boolean isRateLimited(String identifier) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(rateLimitWindowMinutes);
        long requestCount = otpRepository.countOtpsGeneratedSince(identifier, windowStart);
        return requestCount >= maxRequestsPerWindow;
    }

    @Override
    public boolean isLocked(String identifier) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(lockDurationMinutes);
        long failedAttempts = otpRepository.countFailedAttemptsSince(identifier, windowStart);
        return failedAttempts >= maxFailedAttempts;
    }

    @Override
    @Transactional
    public int invalidateOtps(String identifier, OtpPurpose purpose) {
        return otpRepository.invalidateOtpsForIdentifierAndPurpose(identifier, purpose, LocalDateTime.now());
    }

    @Override
    @Transactional
    public int cleanupExpiredOtps() {
        return otpRepository.softDeleteExpiredOtps(LocalDateTime.now());
    }

    @Override
    public long getRateLimitResetTime(String identifier) {
        if (!isRateLimited(identifier)) {
            return 0;
        }
        // Return remaining time in seconds until rate limit window resets
        return rateLimitWindowMinutes * 60;
    }

    @Override
    public long getLockResetTime(String identifier) {
        if (!isLocked(identifier)) {
            return 0;
        }
        // Return remaining time in seconds until lock expires
        return lockDurationMinutes * 60;
    }

    @Override
    public boolean hasValidOtps(String identifier) {
        return otpRepository.hasValidOtps(identifier, LocalDateTime.now());
    }

    @Override
    public String generateTestOtp(String identifier, OtpType type, OtpPurpose purpose) {
        logger.warn("Generating test OTP - this should only be used in test environments");
        return generateOtpCode();
    }

    // Private helper methods

    private void validateGenerationRequest(OtpGenerationRequest request) {
        if (request.getIdentifier() == null || request.getIdentifier().trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("OTP type cannot be null");
        }
        if (request.getPurpose() == null) {
            throw new IllegalArgumentException("OTP purpose cannot be null");
        }
        if (request.getExpirationMinutes() != null && request.getExpirationMinutes() <= 0) {
            throw new IllegalArgumentException("Expiration minutes must be positive");
        }
    }

    private String generateOtpCode() {
        StringBuilder code = new StringBuilder();
        String characters = numericOnly ? "0123456789" : "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        
        for (int i = 0; i < codeLength; i++) {
            code.append(characters.charAt(secureRandom.nextInt(characters.length())));
        }
        
        return code.toString();
    }

    private boolean deliverOtp(OtpGenerationRequest request, String otpCode) {
        // Simulate OTP delivery based on type
        switch (request.getType()) {
            case EMAIL:
                return simulateEmailDelivery(request.getIdentifier(), otpCode, request.getPurpose(), request.getCustomMessage());
            case SMS:
                return simulateSmsDelivery(request.getIdentifier(), otpCode, request.getPurpose(), request.getCustomMessage());
            case VOICE:
                return simulateVoiceDelivery(request.getIdentifier(), otpCode, request.getPurpose());
            case PUSH:
                return simulatePushDelivery(request.getIdentifier(), otpCode, request.getPurpose());
            default:
                logger.warn("Unknown OTP type: {}", request.getType());
                return false;
        }
    }

    private boolean simulateEmailDelivery(String email, String otpCode, OtpPurpose purpose, String customMessage) {
        logger.info("Simulating email delivery to: {} for purpose: {}", maskIdentifier(email), purpose);
        
        String subject = "Your OTP Code - " + purpose.getDisplayName();
        String message = customMessage != null ? customMessage : 
                        String.format("Your OTP code is: %s. This code will expire in %d minutes.", 
                                    otpCode, defaultExpirationMinutes);
        
        // Use external email service client
        boolean sent = emailServiceClient.sendEmail(email, subject, message);
        
        logger.info("Email sent to: {} with subject: {}", maskIdentifier(email), subject);
        return sent;
    }

    private boolean simulateSmsDelivery(String phone, String otpCode, OtpPurpose purpose, String customMessage) {
        logger.info("Simulating SMS delivery to: {} for purpose: {}", maskIdentifier(phone), purpose);
        
        String message = customMessage != null ? customMessage :
                        String.format("Your OTP code is: %s. Valid for %d minutes.", 
                                    otpCode, defaultExpirationMinutes);
        
        // Use external SMS service client
        boolean sent = smsServiceClient.sendSms(phone, message);
        
        logger.info("SMS sent to: {} with message length: {}", maskIdentifier(phone), message.length());
        return sent;
    }

    private boolean simulateVoiceDelivery(String phone, String otpCode, OtpPurpose purpose) {
        logger.info("Simulating voice delivery to: {} for purpose: {}", maskIdentifier(phone), purpose);
        
        // In a real implementation, this would integrate with a voice service
        logger.info("Voice call initiated to: {} with OTP code", maskIdentifier(phone));
        return true;
    }

    private boolean simulatePushDelivery(String identifier, String otpCode, OtpPurpose purpose) {
        logger.info("Simulating push notification to: {} for purpose: {}", maskIdentifier(identifier), purpose);
        
        // In a real implementation, this would integrate with a push notification service
        logger.info("Push notification sent to: {}", maskIdentifier(identifier));
        return true;
    }

    private boolean shouldLockIdentifier(String identifier) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(lockDurationMinutes);
        long failedAttempts = otpRepository.countFailedAttemptsSince(identifier, windowStart);
        return failedAttempts >= maxFailedAttempts;
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
}