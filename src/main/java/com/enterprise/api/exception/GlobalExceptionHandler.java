package com.enterprise.api.exception;

import com.enterprise.api.dto.response.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST API exceptions.
 * Provides consistent error response format and proper HTTP status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex, HttpServletRequest request) {
        logger.warn("Validation error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            HttpStatus.BAD_REQUEST.value()
        );
        
        if (!ex.getFieldErrors().isEmpty()) {
            errorResponse.setFieldErrors(ex.getFieldErrors());
        }
        
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        logger.warn("Method argument validation error: {}", ex.getMessage());
        
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed for request",
            request.getRequestURI(),
            HttpStatus.BAD_REQUEST.value()
        );
        errorResponse.setFieldErrors(fieldErrors);
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        logger.warn("Constraint violation error: {}", ex.getMessage());
        
        Map<String, String> fieldErrors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            fieldErrors.put(fieldName, violation.getMessage());
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            "CONSTRAINT_VIOLATION",
            "Constraint validation failed",
            request.getRequestURI(),
            HttpStatus.BAD_REQUEST.value()
        );
        errorResponse.setFieldErrors(fieldErrors);
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        logger.warn("Entity not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            HttpStatus.NOT_FOUND.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest request) {
        logger.warn("Duplicate resource error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            HttpStatus.CONFLICT.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        logger.warn("Authentication error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            HttpStatus.UNAUTHORIZED.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    @ExceptionHandler({BadCredentialsException.class, org.springframework.security.core.AuthenticationException.class})
    public ResponseEntity<ErrorResponse> handleSpringAuthentication(org.springframework.security.core.AuthenticationException ex, HttpServletRequest request) {
        logger.warn("Spring Security authentication error: {}", ex.getMessage());
        
        String errorCode = "AUTHENTICATION_FAILED";
        String message = "Invalid credentials";
        
        if (ex instanceof BadCredentialsException) {
            message = "Invalid username or password";
        } else if (ex instanceof DisabledException) {
            errorCode = "ACCOUNT_DISABLED";
            message = "Account is disabled";
        } else if (ex instanceof LockedException) {
            errorCode = "ACCOUNT_LOCKED";
            message = "Account is locked";
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            errorCode,
            message,
            request.getRequestURI(),
            HttpStatus.UNAUTHORIZED.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    @ExceptionHandler({AuthorizationException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAuthorization(Exception ex, HttpServletRequest request) {
        logger.warn("Authorization error: {}", ex.getMessage());
        
        String errorCode = "ACCESS_DENIED";
        String message = "Access denied";
        
        if (ex instanceof AuthorizationException authEx) {
            errorCode = authEx.getErrorCode();
            message = authEx.getMessage();
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            errorCode,
            message,
            request.getRequestURI(),
            HttpStatus.FORBIDDEN.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException ex, HttpServletRequest request) {
        logger.warn("JWT error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "JWT_ERROR",
            "Invalid or expired token",
            request.getRequestURI(),
            HttpStatus.UNAUTHORIZED.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<ErrorResponse> handleFileProcessing(FileProcessingException ex, HttpServletRequest request) {
        logger.error("File processing error: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            HttpStatus.BAD_REQUEST.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        logger.warn("File size exceeded: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "FILE_SIZE_EXCEEDED",
            "File size exceeds maximum allowed limit",
            request.getRequestURI(),
            HttpStatus.PAYLOAD_TOO_LARGE.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }
    
    @ExceptionHandler(BatchProcessingException.class)
    public ResponseEntity<ErrorResponse> handleBatchProcessing(BatchProcessingException ex, HttpServletRequest request) {
        logger.error("Batch processing error: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        logger.error("Data integrity violation: {}", ex.getMessage(), ex);
        
        String message = "Data integrity constraint violation";
        String errorCode = "DATA_INTEGRITY_VIOLATION";
        
        // Extract more specific error information if possible
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            String causeMessage = ex.getCause().getMessage().toLowerCase();
            if (causeMessage.contains("unique") || causeMessage.contains("duplicate")) {
                message = "Duplicate entry detected";
                errorCode = "DUPLICATE_ENTRY";
            } else if (causeMessage.contains("foreign key")) {
                message = "Referenced entity does not exist";
                errorCode = "FOREIGN_KEY_VIOLATION";
            }
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            errorCode,
            message,
            request.getRequestURI(),
            HttpStatus.CONFLICT.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        logger.warn("Method not supported: {}", ex.getMessage());
        
        List<String> supportedMethods = List.of(ex.getSupportedMethods());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "METHOD_NOT_SUPPORTED",
            String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()),
            request.getRequestURI(),
            HttpStatus.METHOD_NOT_ALLOWED.value()
        );
        errorResponse.setDetails(List.of("Supported methods: " + String.join(", ", supportedMethods)));
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }
    
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        logger.warn("Media type not supported: {}", ex.getMessage());
        
        List<String> supportedTypes = ex.getSupportedMediaTypes().stream()
            .map(Object::toString)
            .collect(Collectors.toList());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "MEDIA_TYPE_NOT_SUPPORTED",
            String.format("Media type '%s' is not supported", ex.getContentType()),
            request.getRequestURI(),
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()
        );
        errorResponse.setDetails(List.of("Supported media types: " + String.join(", ", supportedTypes)));
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        logger.warn("Message not readable: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "MALFORMED_REQUEST",
            "Request body is malformed or missing",
            request.getRequestURI(),
            HttpStatus.BAD_REQUEST.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        logger.warn("Missing request parameter: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "MISSING_PARAMETER",
            String.format("Required parameter '%s' is missing", ex.getParameterName()),
            request.getRequestURI(),
            HttpStatus.BAD_REQUEST.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        logger.warn("Type mismatch: {}", ex.getMessage());
        
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        
        ErrorResponse errorResponse = new ErrorResponse(
            "TYPE_MISMATCH",
            String.format("Parameter '%s' should be of type %s", ex.getName(), expectedType),
            request.getRequestURI(),
            HttpStatus.BAD_REQUEST.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        logger.warn("No handler found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "ENDPOINT_NOT_FOUND",
            String.format("No endpoint found for %s %s", ex.getHttpMethod(), ex.getRequestURL()),
            request.getRequestURI(),
            HttpStatus.NOT_FOUND.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        logger.warn("Business exception: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI(),
            HttpStatus.BAD_REQUEST.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected error occurred", ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please try again later.",
            request.getRequestURI(),
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        errorResponse.setTraceId(MDC.get("traceId"));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}