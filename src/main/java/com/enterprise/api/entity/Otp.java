package com.enterprise.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Entity representing a One-Time Password (OTP) for user verification.
 * OTPs are time-limited codes used for enhanced security during authentication
 * or sensitive operations.
 */
@Entity
@Table(name = "otps", indexes = {
    @Index(name = "idx_otp_identifier", columnList = "identifier"),
    @Index(name = "idx_otp_expires_at", columnList = "expiresAt")
})
public class Otp extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The identifier for which the OTP was generated (email, phone, username, etc.)
     */
    @Column(nullable = false)
    @NotBlank(message = "Identifier cannot be blank")
    private String identifier;

    /**
     * The OTP code (hashed for security)
     */
    @Column(nullable = false)
    @NotBlank(message = "OTP code cannot be blank")
    private String code;

    /**
     * The type of OTP (EMAIL, SMS, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "OTP type cannot be null")
    private OtpType type;

    /**
     * The purpose of the OTP (LOGIN, PASSWORD_RESET, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "OTP purpose cannot be null")
    private OtpPurpose purpose;

    /**
     * When the OTP expires
     */
    @Column(nullable = false)
    @NotNull(message = "Expiration time cannot be null")
    private LocalDateTime expiresAt;

    /**
     * Whether the OTP has been used
     */
    @Column(nullable = false)
    private boolean used = false;

    /**
     * Number of verification attempts
     */
    @Column(nullable = false)
    private int attempts = 0;

    /**
     * Maximum allowed attempts before OTP is invalidated
     */
    @Column(nullable = false)
    private int maxAttempts = 3;

    /**
     * IP address from which the OTP was requested
     */
    private String requestIpAddress;

    /**
     * User agent from which the OTP was requested
     */
    private String requestUserAgent;

    // Constructors
    public Otp() {}

    public Otp(String identifier, String code, OtpType type, OtpPurpose purpose, 
               LocalDateTime expiresAt, String requestIpAddress, String requestUserAgent) {
        this.identifier = identifier;
        this.code = code;
        this.type = type;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
        this.requestIpAddress = requestIpAddress;
        this.requestUserAgent = requestUserAgent;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getRequestIpAddress() {
        return requestIpAddress;
    }

    public void setRequestIpAddress(String requestIpAddress) {
        this.requestIpAddress = requestIpAddress;
    }

    public String getRequestUserAgent() {
        return requestUserAgent;
    }

    public void setRequestUserAgent(String requestUserAgent) {
        this.requestUserAgent = requestUserAgent;
    }

    // Business methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired() && attempts < maxAttempts && !isDeleted();
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markAsUsed() {
        this.used = true;
    }

    public boolean canAttempt() {
        return attempts < maxAttempts;
    }

    @Override
    public String toString() {
        return "Otp{" +
                "id=" + id +
                ", identifier='" + identifier + '\'' +
                ", type=" + type +
                ", purpose=" + purpose +
                ", expiresAt=" + expiresAt +
                ", used=" + used +
                ", attempts=" + attempts +
                ", maxAttempts=" + maxAttempts +
                '}';
    }
}