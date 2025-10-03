package com.enterprise.api.exception;

/**
 * Exception thrown when authorization fails.
 */
public class AuthorizationException extends BusinessException {
    
    public AuthorizationException(String message) {
        super("ACCESS_DENIED", message);
    }
    
    public AuthorizationException(String resource, String action) {
        super("ACCESS_DENIED", String.format("Access denied for action '%s' on resource '%s'", action, resource), resource, action);
    }
}