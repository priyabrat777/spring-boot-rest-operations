package com.enterprise.api.exception;

/**
 * Exception thrown when batch processing operations fail.
 */
public class BatchProcessingException extends BusinessException {
    
    public BatchProcessingException(String message) {
        super("BATCH_PROCESSING_ERROR", message);
    }
    
    public BatchProcessingException(String message, Throwable cause) {
        super("BATCH_PROCESSING_ERROR", message, cause);
    }
    
    public BatchProcessingException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public BatchProcessingException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}