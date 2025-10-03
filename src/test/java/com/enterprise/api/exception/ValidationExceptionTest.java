package com.enterprise.api.exception;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationExceptionTest {
    
    @Test
    void shouldCreateValidationExceptionWithMessage() {
        // Given
        String message = "Validation failed";
        
        // When
        ValidationException exception = new ValidationException(message);
        
        // Then
        assertThat(exception.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getFieldErrors()).isEmpty();
    }
    
    @Test
    void shouldCreateValidationExceptionWithFieldErrors() {
        // Given
        String message = "Validation failed";
        Map<String, String> fieldErrors = Map.of(
            "username", "Username is required",
            "email", "Email format is invalid"
        );
        
        // When
        ValidationException exception = new ValidationException(message, fieldErrors);
        
        // Then
        assertThat(exception.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getFieldErrors()).containsExactlyInAnyOrderEntriesOf(fieldErrors);
    }
    
    @Test
    void shouldCreateValidationExceptionWithCustomErrorCode() {
        // Given
        String errorCode = "CUSTOM_VALIDATION_ERROR";
        String message = "Custom validation failed";
        
        // When
        ValidationException exception = new ValidationException(errorCode, message);
        
        // Then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getFieldErrors()).isEmpty();
    }
    
    @Test
    void shouldCreateValidationExceptionWithCustomErrorCodeAndFieldErrors() {
        // Given
        String errorCode = "CUSTOM_VALIDATION_ERROR";
        String message = "Custom validation failed";
        Map<String, String> fieldErrors = Map.of("field1", "Error message");
        
        // When
        ValidationException exception = new ValidationException(errorCode, message, fieldErrors);
        
        // Then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getFieldErrors()).containsExactlyInAnyOrderEntriesOf(fieldErrors);
    }
    
    @Test
    void shouldHandleNullFieldErrors() {
        // Given
        String message = "Validation failed";
        Map<String, String> nullFieldErrors = null;
        
        // When
        ValidationException exception = new ValidationException(message, nullFieldErrors);
        
        // Then
        assertThat(exception.getFieldErrors()).isEmpty();
    }
}