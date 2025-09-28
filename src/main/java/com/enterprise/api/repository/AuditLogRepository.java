package com.enterprise.api.repository;

import com.enterprise.api.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for AuditLog entities.
 * Provides methods for querying audit logs with various filters.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

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
}