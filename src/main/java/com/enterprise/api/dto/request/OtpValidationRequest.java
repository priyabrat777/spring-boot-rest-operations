package com.enterprise.api.dto.request;

import com.enterprise.api.entity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for OTP validation.
 * Contains the necessary information to validate an OTP code.
 */
public class OtpValidationRequest {

    /**
     * The identifier for which the OTP was generated
     */
    @NotBlank(message = "Identifier cannot be blank")
    private String identifier;

    /**
     * The OTP code to validate
     */
    @NotBlank(message = "OTP code cannot be blank")
    @Size(min = 4, max = 10, message = "OTP code must be between 4 and 10 characters")
    @Pattern(regexp = "^[0-9A-Za-z]+$", message = "OTP code must contain only alphanumeric characters")
    private String code;

    /**
     * The purpose for which the OTP was generated
     */
    @NotNull(message = "OTP purpose cannot be null")
    private OtpPurpose purpose;

    /**
     * Whether to consume (mark as used) the OTP upon successful validation
     */
    private boolean consumeOnValidation = true;

    // Constructors
    public OtpValidationRequest() {}

    public OtpValidationRequest(String identifier, String code, OtpPurpose purpose) {
        this.identifier = identifier;
        this.code = code;
        this.purpose = purpose;
    }

    public OtpValidationRequest(String identifier, String code, OtpPurpose purpose, 
                               boolean consumeOnValidation) {
        this.identifier = identifier;
        this.code = code;
        this.purpose = purpose;
        this.consumeOnValidation = consumeOnValidation;
    }

    // Getters and Setters
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = purpose;
    }

    public boolean isConsumeOnValidation() {
        return consumeOnValidation;
    }

    public void setConsumeOnValidation(boolean consumeOnValidation) {
        this.consumeOnValidation = consumeOnValidation;
    }

    @Override
    public String toString() {
        return "OtpValidationRequest{" +
                "identifier='" + identifier + '\'' +
                ", code='[REDACTED]'" +
                ", purpose=" + purpose +
                ", consumeOnValidation=" + consumeOnValidation +
                '}';
    }
}