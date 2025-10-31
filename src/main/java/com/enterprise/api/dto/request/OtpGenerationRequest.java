package com.enterprise.api.dto.request;

import com.enterprise.api.entity.OtpPurpose;
import com.enterprise.api.entity.OtpType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for OTP generation.
 * Contains the necessary information to generate and send an OTP.
 */
public class OtpGenerationRequest {

    /**
     * The identifier for which to generate the OTP (email, phone, username, etc.)
     */
    @NotBlank(message = "Identifier cannot be blank")
    private String identifier;

    /**
     * The type of OTP delivery method
     */
    @NotNull(message = "OTP type cannot be null")
    private OtpType type;

    /**
     * The purpose for which the OTP is being generated
     */
    @NotNull(message = "OTP purpose cannot be null")
    private OtpPurpose purpose;

    /**
     * Optional custom expiration time in minutes (default will be used if not provided)
     */
    private Integer expirationMinutes;

    /**
     * Optional custom message template for OTP delivery
     */
    private String customMessage;

    // Constructors
    public OtpGenerationRequest() {}

    public OtpGenerationRequest(String identifier, OtpType type, OtpPurpose purpose) {
        this.identifier = identifier;
        this.type = type;
        this.purpose = purpose;
    }

    public OtpGenerationRequest(String identifier, OtpType type, OtpPurpose purpose, 
                               Integer expirationMinutes, String customMessage) {
        this.identifier = identifier;
        this.type = type;
        this.purpose = purpose;
        this.expirationMinutes = expirationMinutes;
        this.customMessage = customMessage;
    }

    // Getters and Setters
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
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

    public Integer getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(Integer expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    public void setCustomMessage(String customMessage) {
        this.customMessage = customMessage;
    }

    // Validation methods
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isEmailType() {
        return type == OtpType.EMAIL;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isSmsType() {
        return type == OtpType.SMS;
    }

    @Override
    public String toString() {
        return "OtpGenerationRequest{" +
                "identifier='" + identifier + '\'' +
                ", type=" + type +
                ", purpose=" + purpose +
                ", expirationMinutes=" + expirationMinutes +
                ", customMessage='" + (customMessage != null ? "[REDACTED]" : null) + '\'' +
                '}';
    }
}