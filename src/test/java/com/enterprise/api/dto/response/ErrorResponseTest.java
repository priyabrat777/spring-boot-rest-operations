package com.enterprise.api.dto.response;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {
    
    @Test
    void shouldCreateErrorResponseWithDefaultConstructor() {
        // When
        ErrorResponse errorResponse = new ErrorResponse();
        
        // Then
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getTimestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
    }
    
    @Test
    void shouldCreateErrorResponseWithErrorCodeMessageAndStatus() {
        // Given
        String errorCode = "TEST_ERROR";
        String message = "Test error message";
        int status = 400;
        
        // When
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message, status);
        
        // Then
        assertThat(errorResponse.getErrorCode()).isEqualTo(errorCode);
        assertThat(errorResponse.getMessage()).isEqualTo(message);
        assertThat(errorResponse.getStatus()).isEqualTo(status);
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }
    
    @Test
    void shouldCreateErrorResponseWithPath() {
        // Given
        String errorCode = "TEST_ERROR";
        String message = "Test error message";
        String path = "/api/test";
        int status = 404;
        
        // When
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message, path, status);
        
        // Then
        assertThat(errorResponse.getErrorCode()).isEqualTo(errorCode);
        assertThat(errorResponse.getMessage()).isEqualTo(message);
        assertThat(errorResponse.getPath()).isEqualTo(path);
        assertThat(errorResponse.getStatus()).isEqualTo(status);
    }
    
    @Test
    void shouldSetAndGetFieldErrors() {
        // Given
        ErrorResponse errorResponse = new ErrorResponse();
        Map<String, String> fieldErrors = Map.of(
            "username", "Username is required",
            "email", "Email format is invalid"
        );
        
        // When
        errorResponse.setFieldErrors(fieldErrors);
        
        // Then
        assertThat(errorResponse.getFieldErrors()).containsExactlyInAnyOrderEntriesOf(fieldErrors);
    }
    
    @Test
    void shouldSetAndGetDetails() {
        // Given
        ErrorResponse errorResponse = new ErrorResponse();
        List<String> details = List.of("Detail 1", "Detail 2");
        
        // When
        errorResponse.setDetails(details);
        
        // Then
        assertThat(errorResponse.getDetails()).containsExactlyElementsOf(details);
    }
    
    @Test
    void shouldSetAndGetTraceId() {
        // Given
        ErrorResponse errorResponse = new ErrorResponse();
        String traceId = "trace-123";
        
        // When
        errorResponse.setTraceId(traceId);
        
        // Then
        assertThat(errorResponse.getTraceId()).isEqualTo(traceId);
    }
    
    @Test
    void shouldSetAndGetTimestamp() {
        // Given
        ErrorResponse errorResponse = new ErrorResponse();
        LocalDateTime timestamp = LocalDateTime.now().minusHours(1);
        
        // When
        errorResponse.setTimestamp(timestamp);
        
        // Then
        assertThat(errorResponse.getTimestamp()).isEqualTo(timestamp);
    }
}