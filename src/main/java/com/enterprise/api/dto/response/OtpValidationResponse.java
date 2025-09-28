package com.enterprise.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Response DTO for OTP validation.
 * Contains the result of OTP validation attempt.
 */
public class OtpValidationResponse {

    /**
     * Whether the OTP validation was successful
     */
    private boolean valid;

    /**
     * Message describing the validation result
     */
    private String message;

    /**
     * Number of remaining attempts (if validation failed)
     */
    private Integer remainingAttempts;

    /**
     * Whether the OTP was consumed (marked as used)
     */
    private boolean consumed;

    /**
     * Timestamp when the validation was performed
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime validatedAt;

    /**
     * Reason for validation failure (if applicable)
     */
    private String failureReason;

    /**
     * Whether the identifier is temporarily locked due to too many failed attempts
     */
    private boolean locked;

    /**
     * When the lock will be released (if locked)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lockExpiresAt;

    // Constructors
    public OtpValidationResponse() {}

    public OtpValidationResponse(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
        this.validatedAt = LocalDateTime.now();
    }

    public OtpValidationResponse(boolean valid, String message, Integer remainingAttempts, 
                                boolean consumed, String failureReason) {
        this.valid = valid;
        this.message = message;
        this.remainingAttempts = remainingAttempts;
        this.consumed = consumed;
        this.failureReason = failureReason;
        this.validatedAt = LocalDateTime.now();
    }

    // Static factory methods
    public static OtpValidationResponse success(boolean consumed) {
        OtpValidationResponse response = new OtpValidationResponse(true, "OTP validation successful");
        response.setConsumed(consumed);
        return response;
    }

    public static OtpValidationResponse failure(String reason, Integer remainingAttempts) {
        OtpValidationResponse response = new OtpValidationResponse(false, "OTP validation failed");
        response.setFailureReason(reason);
        response.setRemainingAttempts(remainingAttempts);
        return response;
    }

    public static OtpValidationResponse locked(LocalDateTime lockExpiresAt) {
        OtpValidationResponse response = new OtpValidationResponse(false, "Account temporarily locked due to too many failed attempts");
        response.setLocked(true);
        response.setLockExpiresAt(lockExpiresAt);
        response.setFailureReason("TOO_MANY_ATTEMPTS");
        return response;
    }

    // Getters and Setters
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getRemainingAttempts() {
        return remainingAttempts;
    }

    public void setRemainingAttempts(Integer remainingAttempts) {
        this.remainingAttempts = remainingAttempts;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(LocalDateTime validatedAt) {
        this.validatedAt = validatedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public LocalDateTime getLockExpiresAt() {
        return lockExpiresAt;
    }

    public void setLockExpiresAt(LocalDateTime lockExpiresAt) {
        this.lockExpiresAt = lockExpiresAt;
    }

    @Override
    public String toString() {
        return "OtpValidationResponse{" +
                "valid=" + valid +
                ", message='" + message + '\'' +
                ", remainingAttempts=" + remainingAttempts +
                ", consumed=" + consumed +
                ", validatedAt=" + validatedAt +
                ", failureReason='" + failureReason + '\'' +
                ", locked=" + locked +
                ", lockExpiresAt=" + lockExpiresAt +
                '}';
    }
}