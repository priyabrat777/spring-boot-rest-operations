package com.enterprise.api.dto.response;

import com.enterprise.api.entity.AuditLog;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response DTO for audit log information.
 * Contains all audit log details for API responses.
 */
@Schema(description = "Audit log response containing operation details")
public class AuditLogResponse {

    @Schema(description = "Unique identifier of the audit log", example = "1")
    private Long id;

    @Schema(description = "Name of the entity that was modified", example = "User")
    private String entityName;

    @Schema(description = "ID of the entity that was modified", example = "123")
    private String entityId;

    @Schema(description = "Type of operation performed", example = "CREATE")
    private AuditLog.AuditOperation operation;

    @Schema(description = "Previous values before the operation (JSON format)")
    private String oldValues;

    @Schema(description = "New values after the operation (JSON format)")
    private String newValues;

    @Schema(description = "Username of the person who performed the operation", example = "john.doe")
    private String performedBy;

    @Schema(description = "Timestamp when the operation was performed")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime performedAt;

    @Schema(description = "IP address from which the operation was performed", example = "192.168.1.100")
    private String ipAddress;

    @Schema(description = "User agent of the client that performed the operation")
    private String userAgent;

    @Schema(description = "Session ID associated with the operation")
    private String sessionId;

    @Schema(description = "Additional information about the operation")
    private String additionalInfo;

    // Constructors
    public AuditLogResponse() {}

    public AuditLogResponse(Long id, String entityName, String entityId, AuditLog.AuditOperation operation,
                           String performedBy, LocalDateTime performedAt) {
        this.id = id;
        this.entityName = entityName;
        this.entityId = entityId;
        this.operation = operation;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
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

    public AuditLog.AuditOperation getOperation() {
        return operation;
    }

    public void setOperation(AuditLog.AuditOperation operation) {
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
    public String toString() {
        return "AuditLogResponse{" +
                "id=" + id +
                ", entityName='" + entityName + '\'' +
                ", entityId='" + entityId + '\'' +
                ", operation=" + operation +
                ", performedBy='" + performedBy + '\'' +
                ", performedAt=" + performedAt +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}