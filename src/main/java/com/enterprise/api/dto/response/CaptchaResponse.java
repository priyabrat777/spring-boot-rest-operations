package com.enterprise.api.dto.response;

/**
 * Response DTO for CAPTCHA operations.
 */
public class CaptchaResponse {
    
    private String sessionId;
    private String message;
    private boolean valid;
    
    public CaptchaResponse() {}
    
    public CaptchaResponse(String sessionId, String message) {
        this.sessionId = sessionId;
        this.message = message;
    }
    
    public CaptchaResponse(String sessionId, String message, boolean valid) {
        this.sessionId = sessionId;
        this.message = message;
        this.valid = valid;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
}