package com.enterprise.api.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuditLog entity.
 */
class AuditLogTest {

    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
    }

    @Test
    void defaultConstructor_ShouldSetPerformedAtToCurrentTime() {
        // Given & When
        AuditLog newAuditLog = new AuditLog();

        // Then
        assertThat(newAuditLog.getPerformedAt()).isNotNull();
        assertThat(newAuditLog.getPerformedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
        assertThat(newAuditLog.getPerformedAt()).isAfter(LocalDateTime.now().minusSeconds(1));
    }

    @Test
    void parameterizedConstructor_ShouldSetAllRequiredFields() {
        // Given
        String entityName = "User";
        String entityId = "123";
        AuditLog.AuditOperation operation = AuditLog.AuditOperation.CREATE;
        String performedBy = "testuser";

        // When
        AuditLog newAuditLog = new AuditLog(entityName, entityId, operation, performedBy);

        // Then
        assertThat(newAuditLog.getEntityName()).isEqualTo(entityName);
        assertThat(newAuditLog.getEntityId()).isEqualTo(entityId);
        assertThat(newAuditLog.getOperation()).isEqualTo(operation);
        assertThat(newAuditLog.getPerformedBy()).isEqualTo(performedBy);
        assertThat(newAuditLog.getPerformedAt()).isNotNull();
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        // Given
        Long id = 1L;
        String entityName = "User";
        String entityId = "123";
        AuditLog.AuditOperation operation = AuditLog.AuditOperation.UPDATE;
        String oldValues = "{\"name\":\"old\"}";
        String newValues = "{\"name\":\"new\"}";
        String performedBy = "testuser";
        LocalDateTime performedAt = LocalDateTime.now();
        String ipAddress = "192.168.1.1";
        String userAgent = "Mozilla/5.0";
        String sessionId = "session123";
        String additionalInfo = "Additional information";

        // When
        auditLog.setId(id);
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId);
        auditLog.setOperation(operation);
        auditLog.setOldValues(oldValues);
        auditLog.setNewValues(newValues);
        auditLog.setPerformedBy(performedBy);
        auditLog.setPerformedAt(performedAt);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setSessionId(sessionId);
        auditLog.setAdditionalInfo(additionalInfo);

        // Then
        assertThat(auditLog.getId()).isEqualTo(id);
        assertThat(auditLog.getEntityName()).isEqualTo(entityName);
        assertThat(auditLog.getEntityId()).isEqualTo(entityId);
        assertThat(auditLog.getOperation()).isEqualTo(operation);
        assertThat(auditLog.getOldValues()).isEqualTo(oldValues);
        assertThat(auditLog.getNewValues()).isEqualTo(newValues);
        assertThat(auditLog.getPerformedBy()).isEqualTo(performedBy);
        assertThat(auditLog.getPerformedAt()).isEqualTo(performedAt);
        assertThat(auditLog.getIpAddress()).isEqualTo(ipAddress);
        assertThat(auditLog.getUserAgent()).isEqualTo(userAgent);
        assertThat(auditLog.getSessionId()).isEqualTo(sessionId);
        assertThat(auditLog.getAdditionalInfo()).isEqualTo(additionalInfo);
    }

    @Test
    void equals_WhenBothHaveSameId_ShouldReturnTrue() {
        // Given
        Long id = 1L;
        auditLog.setId(id);
        AuditLog other = new AuditLog();
        other.setId(id);

        // When & Then
        assertThat(auditLog.equals(other)).isTrue();
    }

    @Test
    void equals_WhenBothHaveNullId_ShouldReturnTrue() {
        // Given
        AuditLog other = new AuditLog();

        // When & Then
        assertThat(auditLog.equals(other)).isTrue();
    }

    @Test
    void equals_WhenDifferentIds_ShouldReturnFalse() {
        // Given
        auditLog.setId(1L);
        AuditLog other = new AuditLog();
        other.setId(2L);

        // When & Then
        assertThat(auditLog.equals(other)).isFalse();
    }

    @Test
    void equals_WhenComparedWithNull_ShouldReturnFalse() {
        // When & Then
        assertThat(auditLog.equals(null)).isFalse();
    }

    @Test
    void equals_WhenComparedWithSameInstance_ShouldReturnTrue() {
        // When & Then
        assertThat(auditLog.equals(auditLog)).isTrue();
    }

    @Test
    void equals_WhenComparedWithDifferentClass_ShouldReturnFalse() {
        // When & Then
        assertThat(auditLog.equals("string")).isFalse();
    }

    @Test
    void hashCode_WhenIdIsNull_ShouldReturnZero() {
        // When & Then
        assertThat(auditLog.hashCode()).isEqualTo(0);
    }

    @Test
    void hashCode_WhenIdIsSet_ShouldReturnIdHashCode() {
        // Given
        Long id = 1L;
        auditLog.setId(id);

        // When & Then
        assertThat(auditLog.hashCode()).isEqualTo(id.hashCode());
    }

    @Test
    void toString_ShouldContainKeyFields() {
        // Given
        auditLog.setId(1L);
        auditLog.setEntityName("User");
        auditLog.setEntityId("123");
        auditLog.setOperation(AuditLog.AuditOperation.CREATE);
        auditLog.setPerformedBy("testuser");
        auditLog.setIpAddress("192.168.1.1");

        // When
        String result = auditLog.toString();

        // Then
        assertThat(result).contains("AuditLog");
        assertThat(result).contains("id=1");
        assertThat(result).contains("entityName='User'");
        assertThat(result).contains("entityId='123'");
        assertThat(result).contains("operation=CREATE");
        assertThat(result).contains("performedBy='testuser'");
        assertThat(result).contains("ipAddress='192.168.1.1'");
    }

    @Test
    void auditOperation_ShouldHaveCorrectValues() {
        // Then
        assertThat(AuditLog.AuditOperation.CREATE.getValue()).isEqualTo("CREATE");
        assertThat(AuditLog.AuditOperation.UPDATE.getValue()).isEqualTo("UPDATE");
        assertThat(AuditLog.AuditOperation.DELETE.getValue()).isEqualTo("DELETE");
        assertThat(AuditLog.AuditOperation.READ.getValue()).isEqualTo("READ");
        assertThat(AuditLog.AuditOperation.LOGIN.getValue()).isEqualTo("LOGIN");
        assertThat(AuditLog.AuditOperation.LOGOUT.getValue()).isEqualTo("LOGOUT");
        assertThat(AuditLog.AuditOperation.ACCESS_DENIED.getValue()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void auditOperation_toString_ShouldReturnValue() {
        // Then
        assertThat(AuditLog.AuditOperation.CREATE.toString()).isEqualTo("CREATE");
        assertThat(AuditLog.AuditOperation.UPDATE.toString()).isEqualTo("UPDATE");
        assertThat(AuditLog.AuditOperation.DELETE.toString()).isEqualTo("DELETE");
    }
}