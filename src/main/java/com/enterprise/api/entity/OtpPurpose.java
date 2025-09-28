package com.enterprise.api.entity;

/**
 * Enumeration representing the purpose for which an OTP is generated.
 */
public enum OtpPurpose {
    /**
     * OTP for user login/authentication
     */
    LOGIN("Login Authentication"),
    
    /**
     * OTP for password reset
     */
    PASSWORD_RESET("Password Reset"),
    
    /**
     * OTP for account verification
     */
    ACCOUNT_VERIFICATION("Account Verification"),
    
    /**
     * OTP for two-factor authentication
     */
    TWO_FACTOR_AUTH("Two-Factor Authentication"),
    
    /**
     * OTP for sensitive operations
     */
    SENSITIVE_OPERATION("Sensitive Operation"),
    
    /**
     * OTP for email verification
     */
    EMAIL_VERIFICATION("Email Verification"),
    
    /**
     * OTP for phone verification
     */
    PHONE_VERIFICATION("Phone Verification");

    private final String displayName;

    OtpPurpose(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}