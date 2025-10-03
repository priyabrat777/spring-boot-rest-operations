package com.enterprise.api.exception;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends BusinessException {
    
    public AuthenticationException(String message) {
        super("AUTHENTICATION_FAILED", message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super("AUTHENTICATION_FAILED", message, cause);
    }
    
    public AuthenticationException(String errorCode, String message) {
        super(errorCode, message);
    }
}