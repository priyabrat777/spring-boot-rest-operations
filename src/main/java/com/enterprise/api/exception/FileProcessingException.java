package com.enterprise.api.exception;

/**
 * Exception thrown when file processing operations fail.
 */
public class FileProcessingException extends BusinessException {
    
    public FileProcessingException(String message) {
        super("FILE_PROCESSING_ERROR", message);
    }
    
    public FileProcessingException(String message, Throwable cause) {
        super("FILE_PROCESSING_ERROR", message, cause);
    }
    
    public FileProcessingException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public FileProcessingException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}