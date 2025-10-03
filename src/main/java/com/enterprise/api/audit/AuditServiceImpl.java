package com.enterprise.api.audit;

import com.enterprise.api.entity.AuditLog;
import com.enterprise.api.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of AuditService for managing audit logs.
 * 
 * This service handles:
 * - Saving audit log entries
 * - Querying audit logs with various filters
 * - Programmatic audit log creation
 */
@Service
@Transactional(readOnly = true)
public class AuditServiceImpl implements AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog saveAuditLog(AuditLog auditLog) {
        try {
            logger.debug("Saving audit log for entity: {} with ID: {}", 
                        auditLog.getEntityName(), auditLog.getEntityId());
            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to save audit log for entity: {} with ID: {}", 
                        auditLog.getEntityName(), auditLog.getEntityId(), e);
            throw e;
        }
    }

    @Override
    public List<AuditLog> findByEntityNameAndEntityId(String entityName, String entityId) {
        logger.debug("Finding audit logs for entity: {} with ID: {}", entityName, entityId);
        return auditLogRepository.findByEntityNameAndEntityIdOrderByPerformedAtDesc(entityName, entityId);
    }

    @Override
    public Page<AuditLog> findByEntityNameAndEntityId(String entityName, String entityId, Pageable pageable) {
        logger.debug("Finding audit logs for entity: {} with ID: {} with pagination", entityName, entityId);
        return auditLogRepository.findByEntityNameAndEntityIdOrderByPerformedAtDesc(entityName, entityId, pageable);
    }

    @Override
    public Page<AuditLog> findByPerformedByAndDateRange(String performedBy, LocalDateTime startDate, 
                                                       LocalDateTime endDate, Pageable pageable) {
        logger.debug("Finding audit logs for user: {} between {} and {}", performedBy, startDate, endDate);
        return auditLogRepository.findByPerformedByAndPerformedAtBetweenOrderByPerformedAtDesc(
                performedBy, startDate, endDate, pageable);
    }

    @Override
    public Page<AuditLog> findByOperation(AuditLog.AuditOperation operation, Pageable pageable) {
        logger.debug("Finding audit logs for operation: {}", operation);
        return auditLogRepository.findByOperationOrderByPerformedAtDesc(operation, pageable);
    }

    @Override
    public Page<AuditLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        logger.debug("Finding audit logs between {} and {}", startDate, endDate);
        return auditLogRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(startDate, endDate, pageable);
    }

    @Override
    public Optional<AuditLog> findById(Long id) {
        logger.debug("Finding audit log by ID: {}", id);
        return auditLogRepository.findById(id);
    }

    @Override
    public Page<AuditLog> findAll(Pageable pageable) {
        logger.debug("Finding all audit logs with pagination");
        return auditLogRepository.findAllByOrderByPerformedAtDesc(pageable);
    }

    @Override
    @Transactional
    public AuditLog createAuditLog(String entityName, String entityId, AuditLog.AuditOperation operation,
                                  String oldValues, String newValues, String performedBy) {
        
        AuditLog auditLog = new AuditLog(entityName, entityId, operation, performedBy);
        auditLog.setOldValues(oldValues);
        auditLog.setNewValues(newValues);
        
        return saveAuditLog(auditLog);
    }

    @Override
    @Transactional
    public AuditLog createAuditLog(String entityName, String entityId, AuditLog.AuditOperation operation,
                                  String oldValues, String newValues, String performedBy,
                                  String ipAddress, String userAgent, String additionalInfo) {
        
        AuditLog auditLog = new AuditLog(entityName, entityId, operation, performedBy);
        auditLog.setOldValues(oldValues);
        auditLog.setNewValues(newValues);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setAdditionalInfo(additionalInfo);
        
        return saveAuditLog(auditLog);
    }

    @Override
    public Page<AuditLog> findWithFilters(String entityName, String entityId, AuditLog.AuditOperation operation,
                                         String performedBy, LocalDateTime startDate, LocalDateTime endDate, 
                                         Pageable pageable) {
        logger.debug("Finding audit logs with filters - entityName: {}, entityId: {}, operation: {}, performedBy: {}, startDate: {}, endDate: {}", 
                    entityName, entityId, operation, performedBy, startDate, endDate);
        return auditLogRepository.findWithFilters(entityName, entityId, operation, performedBy, startDate, endDate, pageable);
    }

    @Override
    public Map<String, Long> getAuditStatsByOperation() {
        logger.debug("Getting audit statistics by operation");
        List<Object[]> results = auditLogRepository.getAuditStatsByOperation();
        return results.stream()
                .collect(java.util.stream.Collectors.toMap(
                    row -> row[0].toString(),
                    row -> (Long) row[1]
                ));
    }

    @Override
    public Map<String, Long> getAuditStatsByEntityName() {
        logger.debug("Getting audit statistics by entity name");
        List<Object[]> results = auditLogRepository.getAuditStatsByEntityName();
        return results.stream()
                .collect(java.util.stream.Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
    }

    @Override
    public Map<String, Long> getAuditStatsByUser() {
        logger.debug("Getting audit statistics by user");
        List<Object[]> results = auditLogRepository.getAuditStatsByUser();
        return results.stream()
                .collect(java.util.stream.Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1]
                ));
    }

    @Override
    public Map<String, Long> getDailyAuditStats(LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Getting daily audit statistics between {} and {}", startDate, endDate);
        List<Object[]> results = auditLogRepository.getDailyAuditStats(startDate, endDate);
        return results.stream()
                .collect(java.util.stream.Collectors.toMap(
                    row -> row[0].toString(),
                    row -> (Long) row[1]
                ));
    }

    @Override
    public Map<String, Long> findMostActiveUsers(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        logger.debug("Finding most active users between {} and {} (limit: {})", startDate, endDate, limit);
        List<Object[]> results = auditLogRepository.findMostActiveUsers(startDate, endDate, limit);
        return results.stream()
                .collect(java.util.stream.Collectors.toMap(
                    row -> (String) row[0],
                    row -> (Long) row[1],
                    (existing, replacement) -> existing,
                    java.util.LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, Long> findMostAccessedEntities(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        logger.debug("Finding most accessed entities between {} and {} (limit: {})", startDate, endDate, limit);
        List<Object[]> results = auditLogRepository.findMostAccessedEntities(startDate, endDate, limit);
        return results.stream()
                .collect(java.util.stream.Collectors.toMap(
                    row -> row[0] + ":" + row[1], // entityName:entityId
                    row -> (Long) row[2],
                    (existing, replacement) -> existing,
                    java.util.LinkedHashMap::new
                ));
    }

    @Override
    public long countFailedOperations(LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Counting failed operations between {} and {}", startDate, endDate);
        List<AuditLog.AuditOperation> failedOperations = List.of(AuditLog.AuditOperation.ACCESS_DENIED);
        Page<AuditLog> results = auditLogRepository.findFailedOperations(failedOperations, startDate, endDate, 
                                                                         org.springframework.data.domain.PageRequest.of(0, 1));
        return results.getTotalElements();
    }

    @Override
    public long countUniqueUsers(LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Counting unique users between {} and {}", startDate, endDate);
        // This would require a custom query in the repository
        // For now, we'll use a simplified approach
        Page<AuditLog> logs = auditLogRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(
            startDate, endDate, org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE));
        return logs.getContent().stream()
                .map(AuditLog::getPerformedBy)
                .distinct()
                .count();
    }

    @Override
    public long countUniqueIpAddresses(LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Counting unique IP addresses between {} and {}", startDate, endDate);
        // This would require a custom query in the repository
        // For now, we'll use a simplified approach
        Page<AuditLog> logs = auditLogRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(
            startDate, endDate, org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE));
        return logs.getContent().stream()
                .map(AuditLog::getIpAddress)
                .filter(ip -> ip != null && !ip.isEmpty())
                .distinct()
                .count();
    }
}