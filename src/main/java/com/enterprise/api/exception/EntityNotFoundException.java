package com.enterprise.api.exception;

/**
 * Exception thrown when a requested entity is not found.
 */
public class EntityNotFoundException extends BusinessException {
    
    public EntityNotFoundException(String entityType, Long id) {
        super("ENTITY_NOT_FOUND", String.format("%s with id %s not found", entityType, id), entityType, id);
    }
    
    public EntityNotFoundException(String entityType, String id) {
        super("ENTITY_NOT_FOUND", String.format("%s with id %s not found", entityType, id), entityType, id);
    }
    
    public EntityNotFoundException(String message) {
        super("ENTITY_NOT_FOUND", message);
    }
}