package com.enterprise.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Entity for tracking all operations performed on auditable entities.
 * This provides a comprehensive audit trail for compliance and monitoring.
 * 
 * Tracks:
 * - What entity was modified (entityName, entityId)
 * - What operation was performed (CREATE, UPDATE, DELETE)
 * - What changed (oldValues, newValues)
 * - Who performed the operation (performedBy)
 * - When it was performed (performedAt)
 * - Additional context (ipAddress, userAgent)
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_entity", columnList = "entity_name, entity_id"),
    @Index(name = "idx_audit_performed_by", columnList = "performed_by"),
    @Index(name = "idx_audit_performed_at", columnList = "performed_at"),
    @Index(name = "idx_audit_operation", columnList = "operation")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "entity_id", nullable = false, length = 50)
    private String entityId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 20)
    private AuditOperation operation;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    @NotBlank
    @Size(max = 100)
    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @NotNull
    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Size(max = 500)
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Size(max = 255)
    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Size(max = 1000)
    @Column(name = "additional_info", length = 1000)
    private String additionalInfo;

    // Constructors
    public AuditLog() {
        this.performedAt = LocalDateTime.now();
    }

    public AuditLog(String entityName, String entityId, AuditOperation operation, String performedBy) {
        this();
        this.entityName = entityName;
        this.entityId = entityId;
        this.operation = operation;
        this.performedBy = performedBy;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public AuditOperation getOperation() {
        return operation;
    }

    public void setOperation(AuditOperation operation) {
        this.operation = operation;
    }

    public String getOldValues() {
        return oldValues;
    }

    public void setOldValues(String oldValues) {
        this.oldValues = oldValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public LocalDateTime getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(LocalDateTime performedAt) {
        this.performedAt = performedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AuditLog auditLog = (AuditLog) obj;
        return id != null ? id.equals(auditLog.id) : auditLog.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", entityName='" + entityName + '\'' +
                ", entityId='" + entityId + '\'' +
                ", operation=" + operation +
                ", performedBy='" + performedBy + '\'' +
                ", performedAt=" + performedAt +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }

    /**
     * Enumeration of audit operations that can be tracked.
     */
    public enum AuditOperation {
        CREATE("CREATE"),
        UPDATE("UPDATE"),
        DELETE("DELETE"),
        READ("READ"),
        LOGIN("LOGIN"),
        LOGOUT("LOGOUT"),
        ACCESS_DENIED("ACCESS_DENIED");

        private final String value;

        AuditOperation(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}