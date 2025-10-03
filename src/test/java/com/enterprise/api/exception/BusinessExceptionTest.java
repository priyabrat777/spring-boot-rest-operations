package com.enterprise.api.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {
    
    @Test
    void shouldCreateExceptionWithErrorCodeAndMessage() {
        // Given
        String errorCode = "TEST_ERROR";
        String message = "Test error message";
        
        // When
        BusinessException exception = new BusinessException(errorCode, message);
        
        // Then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getArgs()).isEmpty();
        assertThat(exception.getCause()).isNull();
    }
    
    @Test
    void shouldCreateExceptionWithErrorCodeMessageAndArgs() {
        // Given
        String errorCode = "TEST_ERROR";
        String message = "Test error with args";
        Object[] args = {"arg1", "arg2"};
        
        // When
        BusinessException exception = new BusinessException(errorCode, message, args);
        
        // Then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getArgs()).containsExactly("arg1", "arg2");
    }
    
    @Test
    void shouldCreateExceptionWithCause() {
        // Given
        String errorCode = "TEST_ERROR";
        String message = "Test error with cause";
        Throwable cause = new RuntimeException("Root cause");
        
        // When
        BusinessException exception = new BusinessException(errorCode, message, cause);
        
        // Then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getArgs()).isEmpty();
    }
    
    @Test
    void shouldCreateExceptionWithCauseAndArgs() {
        // Given
        String errorCode = "TEST_ERROR";
        String message = "Test error with cause and args";
        Throwable cause = new RuntimeException("Root cause");
        Object[] args = {"arg1", 123};
        
        // When
        BusinessException exception = new BusinessException(errorCode, message, cause, args);
        
        // Then
        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getArgs()).containsExactly("arg1", 123);
    }
    
    @Test
    void shouldHandleNullArgs() {
        // Given
        String errorCode = "TEST_ERROR";
        String message = "Test error with null args";
        
        // When
        BusinessException exception = new BusinessException(errorCode, message, (Object[]) null);
        
        // Then
        assertThat(exception.getArgs()).isEmpty();
    }
}