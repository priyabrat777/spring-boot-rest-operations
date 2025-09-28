package com.enterprise.api.audit;

import com.enterprise.api.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for audit operations.
 * Provides methods for saving and querying audit logs.
 */
public interface AuditService {

    /**
     * Saves an audit log entry.
     * 
     * @param auditLog the audit log to save
     * @return the saved audit log
     */
    AuditLog saveAuditLog(AuditLog auditLog);

    /**
     * Finds audit logs by entity name and ID.
     * 
     * @param entityName the entity name
     * @param entityId the entity ID
     * @return list of audit logs for the entity
     */
    List<AuditLog> findByEntityNameAndEntityId(String entityName, String entityId);

    /**
     * Finds audit logs by entity name and ID with pagination.
     * 
     * @param entityName the entity name
     * @param entityId the entity ID
     * @param pageable pagination information
     * @return page of audit logs for the entity
     */
    Page<AuditLog> findByEntityNameAndEntityId(String entityName, String entityId, Pageable pageable);

    /**
     * Finds audit logs by user within a date range.
     * 
     * @param performedBy the user who performed the operations
     * @param startDate the start date
     * @param endDate the end date
     * @param pageable pagination information
     * @return page of audit logs for the user within the date range
     */
    Page<AuditLog> findByPerformedByAndDateRange(String performedBy, LocalDateTime startDate, 
                                                LocalDateTime endDate, Pageable pageable);

    /**
     * Finds audit logs by operation type.
     * 
     * @param operation the operation type
     * @param pageable pagination information
     * @return page of audit logs for the operation type
     */
    Page<AuditLog> findByOperation(AuditLog.AuditOperation operation, Pageable pageable);

    /**
     * Finds audit logs within a date range.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @param pageable pagination information
     * @return page of audit logs within the date range
     */
    Page<AuditLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Finds an audit log by ID.
     * 
     * @param id the audit log ID
     * @return optional containing the audit log if found
     */
    Optional<AuditLog> findById(Long id);

    /**
     * Finds all audit logs with pagination.
     * 
     * @param pageable pagination information
     * @return page of all audit logs
     */
    Page<AuditLog> findAll(Pageable pageable);

    /**
     * Creates an audit log entry programmatically.
     * 
     * @param entityName the entity name
     * @param entityId the entity ID
     * @param operation the operation type
     * @param oldValues the old values (JSON string)
     * @param newValues the new values (JSON string)
     * @param performedBy the user who performed the operation
     * @return the created audit log
     */
    AuditLog createAuditLog(String entityName, String entityId, AuditLog.AuditOperation operation,
                           String oldValues, String newValues, String performedBy);

    /**
     * Creates an audit log entry with additional context.
     * 
     * @param entityName the entity name
     * @param entityId the entity ID
     * @param operation the operation type
     * @param oldValues the old values (JSON string)
     * @param newValues the new values (JSON string)
     * @param performedBy the user who performed the operation
     * @param ipAddress the IP address
     * @param userAgent the user agent
     * @param additionalInfo additional information
     * @return the created audit log
     */
    AuditLog createAuditLog(String entityName, String entityId, AuditLog.AuditOperation operation,
                           String oldValues, String newValues, String performedBy,
                           String ipAddress, String userAgent, String additionalInfo);
}