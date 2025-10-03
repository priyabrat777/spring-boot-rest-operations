package com.enterprise.api.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 */
public class DuplicateResourceException extends BusinessException {
    
    public DuplicateResourceException(String resourceType, String identifier) {
        super("DUPLICATE_RESOURCE", String.format("%s with identifier '%s' already exists", resourceType, identifier), resourceType, identifier);
    }
    
    public DuplicateResourceException(String message) {
        super("DUPLICATE_RESOURCE", message);
    }
}