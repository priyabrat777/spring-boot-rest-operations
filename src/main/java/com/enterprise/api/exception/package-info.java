/**
 * Exception handling package containing custom exceptions and global exception handler.
 * 
 * <p>This package provides:
 * <ul>
 *   <li>Custom exception hierarchy for business logic errors</li>
 *   <li>Global exception handler for consistent error responses</li>
 *   <li>Standardized error response format</li>
 *   <li>Field-level validation error handling</li>
 * </ul>
 * 
 * <p>All exceptions extend from {@link com.enterprise.api.exception.BusinessException}
 * which provides error codes and message arguments for internationalization support.
 * 
 * <p>The {@link com.enterprise.api.exception.GlobalExceptionHandler} ensures all
 * exceptions are handled consistently with proper HTTP status codes and error responses.
 */
package com.enterprise.api.exception;