package com.enterprise.api.entity;

/**
 * Enumeration representing the delivery method for OTP codes.
 */
public enum OtpType {
    /**
     * OTP delivered via email
     */
    EMAIL("Email"),
    
    /**
     * OTP delivered via SMS
     */
    SMS("SMS"),
    
    /**
     * OTP delivered via voice call
     */
    VOICE("Voice"),
    
    /**
     * OTP delivered via push notification
     */
    PUSH("Push Notification");

    private final String displayName;

    OtpType(String displayName) {
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