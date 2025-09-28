package com.enterprise.api.dto.response;

import com.enterprise.api.entity.OtpPurpose;
import com.enterprise.api.entity.OtpType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Response DTO for OTP generation.
 * Contains information about the generated OTP without exposing the actual code.
 */
public class OtpGenerationResponse {

    /**
     * Unique identifier for the OTP request
     */
    private String otpId;

    /**
     * The identifier for which the OTP was generated (masked for security)
     */
    private String maskedIdentifier;

    /**
     * The type of OTP delivery method
     */
    private OtpType type;

    /**
     * The purpose for which the OTP was generated
     */
    private OtpPurpose purpose;

    /**
     * When the OTP expires
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    /**
     * Maximum number of validation attempts allowed
     */
    private int maxAttempts;

    /**
     * Message indicating successful generation
     */
    private String message;

    /**
     * Whether the OTP was successfully delivered
     */
    private boolean delivered;

    /**
     * Delivery method used
     */
    private String deliveryMethod;

    // Constructors
    public OtpGenerationResponse() {}

    public OtpGenerationResponse(String otpId, String maskedIdentifier, OtpType type, 
                                OtpPurpose purpose, LocalDateTime expiresAt, int maxAttempts,
                                String message, boolean delivered, String deliveryMethod) {
        this.otpId = otpId;
        this.maskedIdentifier = maskedIdentifier;
        this.type = type;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
        this.maxAttempts = maxAttempts;
        this.message = message;
        this.delivered = delivered;
        this.deliveryMethod = deliveryMethod;
    }

    // Getters and Setters
    public String getOtpId() {
        return otpId;
    }

    public void setOtpId(String otpId) {
        this.otpId = otpId;
    }

    public String getMaskedIdentifier() {
        return maskedIdentifier;
    }

    public void setMaskedIdentifier(String maskedIdentifier) {
        this.maskedIdentifier = maskedIdentifier;
    }

    public OtpType getType() {
        return type;
    }

    public void setType(OtpType type) {
        this.type = type;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = purpose;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    @Override
    public String toString() {
        return "OtpGenerationResponse{" +
                "otpId='" + otpId + '\'' +
                ", maskedIdentifier='" + maskedIdentifier + '\'' +
                ", type=" + type +
                ", purpose=" + purpose +
                ", expiresAt=" + expiresAt +
                ", maxAttempts=" + maxAttempts +
                ", message='" + message + '\'' +
                ", delivered=" + delivered +
                ", deliveryMethod='" + deliveryMethod + '\'' +
                '}';
    }
}