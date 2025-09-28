package com.enterprise.api.repository;

import com.enterprise.api.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Repository interface for AuditLog entities.
 * Provides comprehensive methods for querying audit logs with various filters.
 * 
 * Requirements addressed:
 * - 5.1: CRUD operations, custom queries, and specifications
 * - 5.2: @Query annotations and method name derivation
 * - 5.3: Pageable and Sort parameters
 * - 5.4: Batch operations
 * - 3.6: Audit trail queries and filtering
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    /**
     * Finds audit logs by entity name and ID, ordered by performed date descending.
     */
    List<AuditLog> findByEntityNameAndEntityIdOrderByPerformedAtDesc(String entityName, String entityId);

    /**
     * Finds audit logs by entity name and ID with pagination, ordered by performed date descending.
     */
    Page<AuditLog> findByEntityNameAndEntityIdOrderByPerformedAtDesc(String entityName, String entityId, Pageable pageable);

    /**
     * Finds audit logs by user and date range, ordered by performed date descending.
     */
    Page<AuditLog> findByPerformedByAndPerformedAtBetweenOrderByPerformedAtDesc(
            String performedBy, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Finds audit logs by operation type, ordered by performed date descending.
     */
    Page<AuditLog> findByOperationOrderByPerformedAtDesc(AuditLog.AuditOperation operation, Pageable pageable);

    /**
     * Finds audit logs within a date range, ordered by performed date descending.
     */
    Page<AuditLog> findByPerformedAtBetweenOrderByPerformedAtDesc(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Finds all audit logs ordered by performed date descending.
     */
    Page<AuditLog> findAllByOrderByPerformedAtDesc(Pageable pageable);

    /**
     * Finds audit logs by entity name, ordered by performed date descending.
     */
    Page<AuditLog> findByEntityNameOrderByPerformedAtDesc(String entityName, Pageable pageable);

    /**
     * Finds audit logs by user, ordered by performed date descending.
     */
    Page<AuditLog> findByPerformedByOrderByPerformedAtDesc(String performedBy, Pageable pageable);

    /**
     * Custom query to find audit logs with multiple filters.
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:entityName IS NULL OR a.entityName = :entityName) AND " +
           "(:entityId IS NULL OR a.entityId = :entityId) AND " +
           "(:operation IS NULL OR a.operation = :operation) AND " +
           "(:performedBy IS NULL OR a.performedBy = :performedBy) AND " +
           "(:startDate IS NULL OR a.performedAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.performedAt <= :endDate) " +
           "ORDER BY a.performedAt DESC")
    Page<AuditLog> findWithFilters(@Param("entityName") String entityName,
                                  @Param("entityId") String entityId,
                                  @Param("operation") AuditLog.AuditOperation operation,
                                  @Param("performedBy") String performedBy,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate,
                                  Pageable pageable);

    /**
     * Counts audit logs by entity name and ID.
     */
    long countByEntityNameAndEntityId(String entityName, String entityId);

    /**
     * Counts audit logs by user within a date range.
     */
    long countByPerformedByAndPerformedAtBetween(String performedBy, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Finds the latest audit log for a specific entity.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityName = :entityName AND a.entityId = :entityId " +
           "ORDER BY a.performedAt DESC LIMIT 1")
    AuditLog findLatestByEntityNameAndEntityId(@Param("entityName") String entityName, @Param("entityId") String entityId);

    /**
     * Finds audit logs by IP address for security monitoring.
     */
    Page<AuditLog> findByIpAddressOrderByPerformedAtDesc(String ipAddress, Pageable pageable);

    /**
     * Custom query to find suspicious activities (multiple failed operations from same IP).
     */
    @Query("SELECT a FROM AuditLog a WHERE a.ipAddress = :ipAddress AND " +
           "a.operation = :operation AND a.performedAt >= :since " +
           "ORDER BY a.performedAt DESC")
    List<AuditLog> findSuspiciousActivities(@Param("ipAddress") String ipAddress,
                                           @Param("operation") AuditLog.AuditOperation operation,
                                           @Param("since") LocalDateTime since);

    /**
     * Find audit logs by multiple entity IDs for batch operations.
     * 
     * @param entityName the entity name
     * @param entityIds list of entity IDs
     * @return list of audit logs for the entities
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityName = :entityName AND a.entityId IN :entityIds ORDER BY a.performedAt DESC")
    List<AuditLog> findByEntityNameAndEntityIdIn(@Param("entityName") String entityName, @Param("entityIds") List<String> entityIds);

    /**
     * Find audit logs by multiple entity IDs with pagination.
     * 
     * @param entityName the entity name
     * @param entityIds list of entity IDs
     * @param pageable pagination information
     * @return page of audit logs for the entities
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityName = :entityName AND a.entityId IN :entityIds ORDER BY a.performedAt DESC")
    Page<AuditLog> findByEntityNameAndEntityIdIn(@Param("entityName") String entityName, @Param("entityIds") List<String> entityIds, Pageable pageable);

    /**
     * Find audit logs by session ID for tracking user sessions.
     * 
     * @param sessionId the session ID
     * @return list of audit logs for the session
     */
    @Query("SELECT a FROM AuditLog a WHERE a.sessionId = :sessionId ORDER BY a.performedAt DESC")
    List<AuditLog> findBySessionIdOrderByPerformedAtDesc(@Param("sessionId") String sessionId);

    /**
     * Find audit logs by session ID with pagination.
     * 
     * @param sessionId the session ID
     * @param pageable pagination information
     * @return page of audit logs for the session
     */
    @Query("SELECT a FROM AuditLog a WHERE a.sessionId = :sessionId ORDER BY a.performedAt DESC")
    Page<AuditLog> findBySessionIdOrderByPerformedAtDesc(@Param("sessionId") String sessionId, Pageable pageable);

    /**
     * Find recent audit logs for a user within specified hours.
     * 
     * @param performedBy the user who performed the operations
     * @param hours number of hours to look back
     * @return list of recent audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.performedBy = :performedBy AND a.performedAt >= :since ORDER BY a.performedAt DESC")
    List<AuditLog> findRecentByUser(@Param("performedBy") String performedBy, @Param("since") LocalDateTime since);

    /**
     * Find audit logs by operation and entity name.
     * 
     * @param operation the audit operation
     * @param entityName the entity name
     * @param pageable pagination information
     * @return page of audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.operation = :operation AND a.entityName = :entityName ORDER BY a.performedAt DESC")
    Page<AuditLog> findByOperationAndEntityNameOrderByPerformedAtDesc(@Param("operation") AuditLog.AuditOperation operation, 
                                                                      @Param("entityName") String entityName, 
                                                                      Pageable pageable);

    /**
     * Find audit logs with changes (where old values or new values are not null).
     * 
     * @param pageable pagination information
     * @return page of audit logs with changes
     */
    @Query("SELECT a FROM AuditLog a WHERE (a.oldValues IS NOT NULL OR a.newValues IS NOT NULL) ORDER BY a.performedAt DESC")
    Page<AuditLog> findLogsWithChanges(Pageable pageable);

    /**
     * Find audit logs without changes (where both old and new values are null).
     * 
     * @param pageable pagination information
     * @return page of audit logs without changes
     */
    @Query("SELECT a FROM AuditLog a WHERE a.oldValues IS NULL AND a.newValues IS NULL ORDER BY a.performedAt DESC")
    Page<AuditLog> findLogsWithoutChanges(Pageable pageable);

    /**
     * Count audit logs by operation type.
     * 
     * @param operation the audit operation
     * @return count of audit logs
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.operation = :operation")
    long countByOperation(@Param("operation") AuditLog.AuditOperation operation);

    /**
     * Count audit logs by entity name.
     * 
     * @param entityName the entity name
     * @return count of audit logs
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.entityName = :entityName")
    long countByEntityName(@Param("entityName") String entityName);

    /**
     * Count audit logs by user.
     * 
     * @param performedBy the user who performed the operations
     * @return count of audit logs
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.performedBy = :performedBy")
    long countByPerformedBy(@Param("performedBy") String performedBy);

    /**
     * Get audit statistics by operation type.
     * 
     * @return map of operation types to counts
     */
    @Query("SELECT a.operation, COUNT(a) FROM AuditLog a GROUP BY a.operation")
    List<Object[]> getAuditStatsByOperation();

    /**
     * Get audit statistics by entity name.
     * 
     * @return map of entity names to counts
     */
    @Query("SELECT a.entityName, COUNT(a) FROM AuditLog a GROUP BY a.entityName")
    List<Object[]> getAuditStatsByEntityName();

    /**
     * Get audit statistics by user.
     * 
     * @return map of users to counts
     */
    @Query("SELECT a.performedBy, COUNT(a) FROM AuditLog a GROUP BY a.performedBy ORDER BY COUNT(a) DESC")
    List<Object[]> getAuditStatsByUser();

    /**
     * Get daily audit statistics for a date range.
     * 
     * @param startDate start date
     * @param endDate end date
     * @return list of daily counts
     */
    @Query("SELECT DATE(a.performedAt), COUNT(a) FROM AuditLog a WHERE a.performedAt BETWEEN :startDate AND :endDate GROUP BY DATE(a.performedAt) ORDER BY DATE(a.performedAt)")
    List<Object[]> getDailyAuditStats(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find most active users within a date range.
     * 
     * @param startDate start date
     * @param endDate end date
     * @param limit maximum number of users to return
     * @return list of users with their activity counts
     */
    @Query("SELECT a.performedBy, COUNT(a) FROM AuditLog a WHERE a.performedAt BETWEEN :startDate AND :endDate GROUP BY a.performedBy ORDER BY COUNT(a) DESC LIMIT :limit")
    List<Object[]> findMostActiveUsers(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate, 
                                      @Param("limit") int limit);

    /**
     * Find most accessed entities within a date range.
     * 
     * @param startDate start date
     * @param endDate end date
     * @param limit maximum number of entities to return
     * @return list of entities with their access counts
     */
    @Query("SELECT a.entityName, a.entityId, COUNT(a) FROM AuditLog a WHERE a.performedAt BETWEEN :startDate AND :endDate GROUP BY a.entityName, a.entityId ORDER BY COUNT(a) DESC LIMIT :limit")
    List<Object[]> findMostAccessedEntities(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate, 
                                           @Param("limit") int limit);

    /**
     * Find failed operations (like ACCESS_DENIED) within a date range.
     * 
     * @param operations list of operations to consider as failures
     * @param startDate start date
     * @param endDate end date
     * @param pageable pagination information
     * @return page of failed operations
     */
    @Query("SELECT a FROM AuditLog a WHERE a.operation IN :operations AND a.performedAt BETWEEN :startDate AND :endDate ORDER BY a.performedAt DESC")
    Page<AuditLog> findFailedOperations(@Param("operations") List<AuditLog.AuditOperation> operations,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       Pageable pageable);

    /**
     * Find audit logs by IP address pattern for security analysis.
     * 
     * @param ipPattern IP address pattern (supports wildcards)
     * @param pageable pagination information
     * @return page of audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.ipAddress LIKE :ipPattern ORDER BY a.performedAt DESC")
    Page<AuditLog> findByIpAddressPattern(@Param("ipPattern") String ipPattern, Pageable pageable);

    /**
     * Find audit logs by user agent pattern for device tracking.
     * 
     * @param userAgentPattern user agent pattern (supports wildcards)
     * @param pageable pagination information
     * @return page of audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userAgent LIKE :userAgentPattern ORDER BY a.performedAt DESC")
    Page<AuditLog> findByUserAgentPattern(@Param("userAgentPattern") String userAgentPattern, Pageable pageable);

    /**
     * Delete old audit logs before a specific date for cleanup.
     * 
     * @param cutoffDate the cutoff date for deletion
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.performedAt < :cutoffDate")
    int deleteOldAuditLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Archive old audit logs by updating additional info field.
     * 
     * @param cutoffDate the cutoff date for archiving
     * @param archiveInfo information to add to archived logs
     * @return number of archived records
     */
    @Modifying
    @Query("UPDATE AuditLog a SET a.additionalInfo = CONCAT(COALESCE(a.additionalInfo, ''), :archiveInfo) WHERE a.performedAt < :cutoffDate AND (a.additionalInfo IS NULL OR a.additionalInfo NOT LIKE '%ARCHIVED%')")
    int archiveOldAuditLogs(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("archiveInfo") String archiveInfo);

    /**
     * Find audit logs for compliance reporting within date range and entity types.
     * 
     * @param entityNames list of entity names to include
     * @param operations list of operations to include
     * @param startDate start date
     * @param endDate end date
     * @param pageable pagination information
     * @return page of audit logs for compliance
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityName IN :entityNames AND a.operation IN :operations AND a.performedAt BETWEEN :startDate AND :endDate ORDER BY a.performedAt DESC")
    Page<AuditLog> findForComplianceReport(@Param("entityNames") List<String> entityNames,
                                          @Param("operations") List<AuditLog.AuditOperation> operations,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          Pageable pageable);

    /**
     * Find the first audit log (creation) for a specific entity.
     * 
     * @param entityName the entity name
     * @param entityId the entity ID
     * @return the first audit log for the entity
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityName = :entityName AND a.entityId = :entityId AND a.operation = 'CREATE' ORDER BY a.performedAt ASC LIMIT 1")
    AuditLog findFirstByEntityNameAndEntityId(@Param("entityName") String entityName, @Param("entityId") String entityId);

    /**
     * Check if an entity has been accessed by a specific user.
     * 
     * @param entityName the entity name
     * @param entityId the entity ID
     * @param performedBy the user to check
     * @return true if the user has accessed the entity
     */
    @Query("SELECT COUNT(a) > 0 FROM AuditLog a WHERE a.entityName = :entityName AND a.entityId = :entityId AND a.performedBy = :performedBy")
    boolean hasUserAccessedEntity(@Param("entityName") String entityName, 
                                 @Param("entityId") String entityId, 
                                 @Param("performedBy") String performedBy);
}