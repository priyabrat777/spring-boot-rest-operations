package com.enterprise.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for CAPTCHA validation.
 */
public class CaptchaValidationRequest {
    
    @NotBlank(message = "Session ID is required")
    private String sessionId;
    
    @NotBlank(message = "CAPTCHA response is required")
    @Size(min = 1, max = 10, message = "CAPTCHA response must be between 1 and 10 characters")
    private String response;
    
    public CaptchaValidationRequest() {}
    
    public CaptchaValidationRequest(String sessionId, String response) {
        this.sessionId = sessionId;
        this.response = response;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
}