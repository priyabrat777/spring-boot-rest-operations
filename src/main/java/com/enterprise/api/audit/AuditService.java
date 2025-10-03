package com.enterprise.api.audit;

import com.enterprise.api.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    /**
     * Finds audit logs with multiple filters using the repository's custom query.
     * 
     * @param entityName the entity name filter
     * @param entityId the entity ID filter
     * @param operation the operation filter
     * @param performedBy the user filter
     * @param startDate the start date filter
     * @param endDate the end date filter
     * @param pageable pagination information
     * @return page of filtered audit logs
     */
    Page<AuditLog> findWithFilters(String entityName, String entityId, AuditLog.AuditOperation operation,
                                  String performedBy, LocalDateTime startDate, LocalDateTime endDate, 
                                  Pageable pageable);

    /**
     * Gets audit statistics by operation type.
     * 
     * @return map of operation types to counts
     */
    Map<String, Long> getAuditStatsByOperation();

    /**
     * Gets audit statistics by entity name.
     * 
     * @return map of entity names to counts
     */
    Map<String, Long> getAuditStatsByEntityName();

    /**
     * Gets audit statistics by user.
     * 
     * @return map of users to counts
     */
    Map<String, Long> getAuditStatsByUser();

    /**
     * Gets daily audit statistics for a date range.
     * 
     * @param startDate start date
     * @param endDate end date
     * @return map of dates to counts
     */
    Map<String, Long> getDailyAuditStats(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Finds most active users within a date range.
     * 
     * @param startDate start date
     * @param endDate end date
     * @param limit maximum number of users to return
     * @return map of users to their activity counts
     */
    Map<String, Long> findMostActiveUsers(LocalDateTime startDate, LocalDateTime endDate, int limit);

    /**
     * Finds most accessed entities within a date range.
     * 
     * @param startDate start date
     * @param endDate end date
     * @param limit maximum number of entities to return
     * @return map of entities to their access counts
     */
    Map<String, Long> findMostAccessedEntities(LocalDateTime startDate, LocalDateTime endDate, int limit);

    /**
     * Counts failed operations within a date range.
     * 
     * @param startDate start date
     * @param endDate end date
     * @return count of failed operations
     */
    long countFailedOperations(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Counts unique users within a date range.
     * 
     * @param startDate start date
     * @param endDate end date
     * @return count of unique users
     */
    long countUniqueUsers(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Counts unique IP addresses within a date range.
     * 
     * @param startDate start date
     * @param endDate end date
     * @return count of unique IP addresses
     */
    long countUniqueIpAddresses(LocalDateTime startDate, LocalDateTime endDate);
}