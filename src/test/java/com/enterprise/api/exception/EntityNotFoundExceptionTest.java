package com.enterprise.api.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityNotFoundExceptionTest {
    
    @Test
    void shouldCreateExceptionWithEntityTypeAndId() {
        // Given
        String entityType = "User";
        Long id = 123L;
        
        // When
        EntityNotFoundException exception = new EntityNotFoundException(entityType, id);
        
        // Then
        assertThat(exception.getErrorCode()).isEqualTo("ENTITY_NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("User with id 123 not found");
        assertThat(exception.getArgs()).containsExactly("User", 123L);
    }
    
    @Test
    void shouldCreateExceptionWithStringId() {
        // Given
        String entityType = "Role";
        String id = "admin";
        
        // When
        EntityNotFoundException exception = new EntityNotFoundException(entityType, id);
        
        // Then
        assertThat(exception.getErrorCode()).isEqualTo("ENTITY_NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("Role with id admin not found");
        assertThat(exception.getArgs()).containsExactly("Role", "admin");
    }
    
    @Test
    void shouldCreateExceptionWithCustomMessage() {
        // Given
        String message = "Custom entity not found";
        
        // When
        EntityNotFoundException exception = new EntityNotFoundException(message);
        
        // Then
        assertThat(exception.getErrorCode()).isEqualTo("ENTITY_NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo(message);
    }
}