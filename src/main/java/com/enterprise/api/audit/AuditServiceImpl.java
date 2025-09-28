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

    @Autowired
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
}