package com.enterprise.api.exception;

import com.enterprise.api.dto.response.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {
    
    @Mock
    private HttpServletRequest request;
    
    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;
    
    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/test");
    }
    
    @Test
    void shouldHandleValidationException() {
        // Given
        Map<String, String> fieldErrors = Map.of("username", "Username is required");
        ValidationException exception = new ValidationException("Validation failed", fieldErrors);
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getFieldErrors()).containsExactlyInAnyOrderEntriesOf(fieldErrors);
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }
    
    @Test
    void shouldHandleMethodArgumentNotValidException() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("user", "email", "Email is required");
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentNotValid(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getFieldErrors()).containsEntry("email", "Email is required");
    }
    
    @Test
    void shouldHandleConstraintViolationException() {
        // Given
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation.getPropertyPath().toString()).thenReturn("username");
        when(violation.getMessage()).thenReturn("Username cannot be null");
        
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolation(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("CONSTRAINT_VIOLATION");
        assertThat(response.getBody().getFieldErrors()).containsEntry("username", "Username cannot be null");
    }
    
    @Test
    void shouldHandleEntityNotFoundException() {
        // Given
        EntityNotFoundException exception = new EntityNotFoundException("User", 123L);
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleEntityNotFound(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("ENTITY_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("User with id 123 not found");
    }
    
    @Test
    void shouldHandleDuplicateResourceException() {
        // Given
        DuplicateResourceException exception = new DuplicateResourceException("User", "john@example.com");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateResource(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("DUPLICATE_RESOURCE");
    }
    
    @Test
    void shouldHandleAuthenticationException() {
        // Given
        AuthenticationException exception = new AuthenticationException("Invalid credentials");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthentication(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("AUTHENTICATION_FAILED");
    }
    
    @Test
    void shouldHandleAuthorizationException() {
        // Given
        AuthorizationException exception = new AuthorizationException("users", "delete");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthorization(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("ACCESS_DENIED");
    }
    
    @Test
    void shouldHandleAccessDeniedException() {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthorization(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("ACCESS_DENIED");
    }
    
    @Test
    void shouldHandleJwtException() {
        // Given
        JwtException exception = new JwtException("Token expired");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleJwtException(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("JWT_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid or expired token");
    }
    
    @Test
    void shouldHandleFileProcessingException() {
        // Given
        FileProcessingException exception = new FileProcessingException("File upload failed");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleFileProcessing(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("FILE_PROCESSING_ERROR");
    }
    
    @Test
    void shouldHandleMaxUploadSizeExceededException() {
        // Given
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(1024L);
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMaxUploadSizeExceeded(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("FILE_SIZE_EXCEEDED");
    }
    
    @Test
    void shouldHandleBatchProcessingException() {
        // Given
        BatchProcessingException exception = new BatchProcessingException("Batch job failed");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBatchProcessing(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("BATCH_PROCESSING_ERROR");
    }
    
    @Test
    void shouldHandleDataIntegrityViolationException() {
        // Given
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Constraint violation");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("DATA_INTEGRITY_VIOLATION");
    }
    
    @Test
    void shouldHandleDataIntegrityViolationWithUniqueConstraint() {
        // Given
        RuntimeException cause = new RuntimeException("unique constraint violation");
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Constraint violation", cause);
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("DUPLICATE_ENTRY");
        assertThat(response.getBody().getMessage()).isEqualTo("Duplicate entry detected");
    }
    
    @Test
    void shouldHandleHttpRequestMethodNotSupportedException() {
        // Given
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("DELETE", java.util.List.of("GET", "POST"));
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodNotSupported(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("METHOD_NOT_SUPPORTED");
        assertThat(response.getBody().getDetails()).contains("Supported methods: GET, POST");
    }
    
    @Test
    void shouldHandleHttpMessageNotReadableException() {
        // Given
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn("JSON parse error");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMessageNotReadable(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("MALFORMED_REQUEST");
    }
    
    @Test
    void shouldHandleMissingServletRequestParameterException() {
        // Given
        MissingServletRequestParameterException exception = new MissingServletRequestParameterException("userId", "Long");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMissingParameter(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("MISSING_PARAMETER");
        assertThat(response.getBody().getMessage()).contains("userId");
    }
    
    @Test
    void shouldHandleMethodArgumentTypeMismatchException() {
        // Given
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("userId");
        when(exception.getRequiredType()).thenReturn((Class) Long.class);
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTypeMismatch(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("TYPE_MISMATCH");
        assertThat(response.getBody().getMessage()).contains("userId").contains("Long");
    }
    
    @Test
    void shouldHandleBusinessException() {
        // Given
        BusinessException exception = new BusinessException("CUSTOM_ERROR", "Custom business error");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("CUSTOM_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("Custom business error");
    }
    
    @Test
    void shouldHandleGenericException() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected error");
        
        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception, request);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred. Please try again later.");
    }
}