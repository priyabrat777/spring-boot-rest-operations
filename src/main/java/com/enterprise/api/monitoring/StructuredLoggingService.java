package com.enterprise.api.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Service for structured logging with correlation IDs and contextual information.
 * 
 * <p>This service provides methods for logging business events, security events,
 * performance metrics, and audit trails with structured data.</p>
 */
@Service
public class StructuredLoggingService {

    private static final Logger auditLogger = LoggerFactory.getLogger("com.enterprise.api.audit");
    private static final Logger securityLogger = LoggerFactory.getLogger("com.enterprise.api.security");
    private static final Logger performanceLogger = LoggerFactory.getLogger("com.enterprise.api.monitoring");
    private static final Logger businessLogger = LoggerFactory.getLogger("com.enterprise.api.business");

    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String USER_ID_KEY = "userId";
    private static final String SESSION_ID_KEY = "sessionId";
    private static final String IP_ADDRESS_KEY = "ipAddress";
    private static final String USER_AGENT_KEY = "userAgent";
    private static final String OPERATION_KEY = "operation";
    private static final String ENTITY_TYPE_KEY = "entityType";
    private static final String ENTITY_ID_KEY = "entityId";
    private static final String DURATION_KEY = "duration";
    private static final String STATUS_KEY = "status";

    /**
     * Generate and set a correlation ID for the current thread.
     */
    public String generateCorrelationId() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put(CORRELATION_ID_KEY, correlationId);
        return correlationId;
    }

    /**
     * Set correlation ID for the current thread.
     */
    public void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }

    /**
     * Set user context information.
     */
    public void setUserContext(String userId, String sessionId, String ipAddress, String userAgent) {
        if (userId != null) MDC.put(USER_ID_KEY, userId);
        if (sessionId != null) MDC.put(SESSION_ID_KEY, sessionId);
        if (ipAddress != null) MDC.put(IP_ADDRESS_KEY, ipAddress);
        if (userAgent != null) MDC.put(USER_AGENT_KEY, userAgent);
    }

    /**
     * Clear all MDC context.
     */
    public void clearContext() {
        MDC.clear();
    }

    /**
     * Log an audit event.
     */
    public void logAuditEvent(String operation, String entityType, String entityId, 
                             String oldValues, String newValues, String status) {
        try {
            MDC.put(OPERATION_KEY, operation);
            MDC.put(ENTITY_TYPE_KEY, entityType);
            MDC.put(ENTITY_ID_KEY, entityId);
            MDC.put(STATUS_KEY, status);
            
            auditLogger.info("Audit event: {} on {} [{}] - Status: {} | Old: {} | New: {}", 
                    operation, entityType, entityId, status, oldValues, newValues);
        } finally {
            MDC.remove(OPERATION_KEY);
            MDC.remove(ENTITY_TYPE_KEY);
            MDC.remove(ENTITY_ID_KEY);
            MDC.remove(STATUS_KEY);
        }
    }

    /**
     * Log a security event.
     */
    public void logSecurityEvent(String event, String details, String status) {
        try {
            MDC.put(OPERATION_KEY, event);
            MDC.put(STATUS_KEY, status);
            
            securityLogger.info("Security event: {} - Status: {} | Details: {}", 
                    event, status, details);
        } finally {
            MDC.remove(OPERATION_KEY);
            MDC.remove(STATUS_KEY);
        }
    }

    /**
     * Log a performance event.
     */
    public void logPerformanceEvent(String operation, long durationMs, String status, 
                                   Map<String, Object> additionalData) {
        try {
            MDC.put(OPERATION_KEY, operation);
            MDC.put(DURATION_KEY, String.valueOf(durationMs));
            MDC.put(STATUS_KEY, status);
            
            // Add additional data to MDC
            if (additionalData != null) {
                additionalData.forEach((key, value) -> 
                    MDC.put(key, value != null ? value.toString() : "null"));
            }
            
            if (durationMs > 1000) {
                performanceLogger.warn("Slow operation detected: {} took {}ms - Status: {}", 
                        operation, durationMs, status);
            } else {
                performanceLogger.info("Performance: {} completed in {}ms - Status: {}", 
                        operation, durationMs, status);
            }
        } finally {
            MDC.remove(OPERATION_KEY);
            MDC.remove(DURATION_KEY);
            MDC.remove(STATUS_KEY);
            
            // Remove additional data from MDC
            if (additionalData != null) {
                additionalData.keySet().forEach(MDC::remove);
            }
        }
    }

    /**
     * Log a business event.
     */
    public void logBusinessEvent(String event, String details, String status) {
        try {
            MDC.put(OPERATION_KEY, event);
            MDC.put(STATUS_KEY, status);
            
            businessLogger.info("Business event: {} - Status: {} | Details: {}", 
                    event, status, details);
        } finally {
            MDC.remove(OPERATION_KEY);
            MDC.remove(STATUS_KEY);
        }
    }

    /**
     * Log an authentication event.
     */
    public void logAuthenticationEvent(String username, String event, boolean success, String reason) {
        try {
            MDC.put(OPERATION_KEY, "AUTHENTICATION");
            MDC.put("username", username);
            MDC.put("event", event);
            MDC.put(STATUS_KEY, success ? "SUCCESS" : "FAILURE");
            
            if (success) {
                securityLogger.info("Authentication success: {} - Event: {}", username, event);
            } else {
                securityLogger.warn("Authentication failure: {} - Event: {} | Reason: {}", 
                        username, event, reason);
            }
        } finally {
            MDC.remove(OPERATION_KEY);
            MDC.remove("username");
            MDC.remove("event");
            MDC.remove(STATUS_KEY);
        }
    }

    /**
     * Log an authorization event.
     */
    public void logAuthorizationEvent(String username, String resource, String action, 
                                    boolean granted, String reason) {
        try {
            MDC.put(OPERATION_KEY, "AUTHORIZATION");
            MDC.put("username", username);
            MDC.put("resource", resource);
            MDC.put("action", action);
            MDC.put(STATUS_KEY, granted ? "GRANTED" : "DENIED");
            
            if (granted) {
                securityLogger.info("Authorization granted: {} access to {} for {}", 
                        username, resource, action);
            } else {
                securityLogger.warn("Authorization denied: {} access to {} for {} | Reason: {}", 
                        username, resource, action, reason);
            }
        } finally {
            MDC.remove(OPERATION_KEY);
            MDC.remove("username");
            MDC.remove("resource");
            MDC.remove("action");
            MDC.remove(STATUS_KEY);
        }
    }

    /**
     * Log a file operation event.
     */
    public void logFileOperationEvent(String operation, String filename, long fileSize, 
                                    String contentType, String status) {
        try {
            MDC.put(OPERATION_KEY, operation);
            MDC.put("filename", filename);
            MDC.put("fileSize", String.valueOf(fileSize));
            MDC.put("contentType", contentType);
            MDC.put(STATUS_KEY, status);
            
            businessLogger.info("File operation: {} - File: {} ({}bytes, {}) - Status: {}", 
                    operation, filename, fileSize, contentType, status);
        } finally {
            MDC.remove(OPERATION_KEY);
            MDC.remove("filename");
            MDC.remove("fileSize");
            MDC.remove("contentType");
            MDC.remove(STATUS_KEY);
        }
    }

    /**
     * Log a batch job event.
     */
    public void logBatchJobEvent(String jobName, String operation, long duration, 
                               String status, Map<String, Object> jobMetrics) {
        try {
            MDC.put(OPERATION_KEY, "BATCH_JOB");
            MDC.put("jobName", jobName);
            MDC.put("jobOperation", operation);
            MDC.put(DURATION_KEY, String.valueOf(duration));
            MDC.put(STATUS_KEY, status);
            
            // Add job metrics to MDC
            if (jobMetrics != null) {
                jobMetrics.forEach((key, value) -> 
                    MDC.put("job_" + key, value != null ? value.toString() : "null"));
            }
            
            businessLogger.info("Batch job: {} - Operation: {} completed in {}ms - Status: {}", 
                    jobName, operation, duration, status);
        } finally {
            MDC.remove(OPERATION_KEY);
            MDC.remove("jobName");
            MDC.remove("jobOperation");
            MDC.remove(DURATION_KEY);
            MDC.remove(STATUS_KEY);
            
            // Remove job metrics from MDC
            if (jobMetrics != null) {
                jobMetrics.keySet().forEach(key -> MDC.remove("job_" + key));
            }
        }
    }
}