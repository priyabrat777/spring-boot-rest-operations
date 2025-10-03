package com.enterprise.api.exception;

import java.util.Map;

/**
 * Exception thrown when validation fails.
 * Can contain field-level validation errors.
 */
public class ValidationException extends BusinessException {
    
    private final Map<String, String> fieldErrors;
    
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
        this.fieldErrors = Map.of();
    }
    
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super("VALIDATION_ERROR", message);
        this.fieldErrors = fieldErrors != null ? fieldErrors : Map.of();
    }
    
    public ValidationException(String errorCode, String message) {
        super(errorCode, message);
        this.fieldErrors = Map.of();
    }
    
    public ValidationException(String errorCode, String message, Map<String, String> fieldErrors) {
        super(errorCode, message);
        this.fieldErrors = fieldErrors != null ? fieldErrors : Map.of();
    }
    
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}