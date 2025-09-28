package com.enterprise.api.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuditableEntity.
 */
class AuditableEntityTest {

    private TestAuditableEntity entity;

    @BeforeEach
    void setUp() {
        entity = new TestAuditableEntity();
    }

    @Test
    void newEntity_ShouldHaveDefaultValues() {
        // Then
        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.getCreatedDate()).isNull();
        assertThat(entity.getLastModifiedDate()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getLastModifiedBy()).isNull();
        assertThat(entity.getVersion()).isNull();
    }

    @Test
    void setCreatedDate_ShouldSetValue() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        entity.setCreatedDate(now);

        // Then
        assertThat(entity.getCreatedDate()).isEqualTo(now);
    }

    @Test
    void setLastModifiedDate_ShouldSetValue() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        entity.setLastModifiedDate(now);

        // Then
        assertThat(entity.getLastModifiedDate()).isEqualTo(now);
    }

    @Test
    void setCreatedBy_ShouldSetValue() {
        // Given
        String createdBy = "testuser";

        // When
        entity.setCreatedBy(createdBy);

        // Then
        assertThat(entity.getCreatedBy()).isEqualTo(createdBy);
    }

    @Test
    void setLastModifiedBy_ShouldSetValue() {
        // Given
        String lastModifiedBy = "testuser";

        // When
        entity.setLastModifiedBy(lastModifiedBy);

        // Then
        assertThat(entity.getLastModifiedBy()).isEqualTo(lastModifiedBy);
    }

    @Test
    void setVersion_ShouldSetValue() {
        // Given
        Long version = 1L;

        // When
        entity.setVersion(version);

        // Then
        assertThat(entity.getVersion()).isEqualTo(version);
    }

    @Test
    void markAsDeleted_ShouldSetDeletedToTrue() {
        // Given
        assertThat(entity.isDeleted()).isFalse();

        // When
        entity.markAsDeleted();

        // Then
        assertThat(entity.isDeleted()).isTrue();
    }

    @Test
    void restore_ShouldSetDeletedToFalse() {
        // Given
        entity.setDeleted(true);
        assertThat(entity.isDeleted()).isTrue();

        // When
        entity.restore();

        // Then
        assertThat(entity.isDeleted()).isFalse();
    }

    @Test
    void equals_WhenBothEntitiesHaveNullVersion_ShouldReturnFalse() {
        // Given
        TestAuditableEntity other = new TestAuditableEntity();

        // When & Then
        assertThat(entity.equals(other)).isFalse();
    }

    @Test
    void equals_WhenBothEntitiesHaveSameVersion_ShouldReturnTrue() {
        // Given
        Long version = 1L;
        entity.setVersion(version);
        TestAuditableEntity other = new TestAuditableEntity();
        other.setVersion(version);

        // When & Then
        assertThat(entity.equals(other)).isTrue();
    }

    @Test
    void equals_WhenEntitiesHaveDifferentVersions_ShouldReturnFalse() {
        // Given
        entity.setVersion(1L);
        TestAuditableEntity other = new TestAuditableEntity();
        other.setVersion(2L);

        // When & Then
        assertThat(entity.equals(other)).isFalse();
    }

    @Test
    void equals_WhenComparedWithNull_ShouldReturnFalse() {
        // When & Then
        assertThat(entity.equals(null)).isFalse();
    }

    @Test
    void equals_WhenComparedWithSameInstance_ShouldReturnTrue() {
        // When & Then
        assertThat(entity.equals(entity)).isTrue();
    }

    @Test
    void equals_WhenComparedWithDifferentClass_ShouldReturnFalse() {
        // When & Then
        assertThat(entity.equals("string")).isFalse();
    }

    @Test
    void hashCode_WhenVersionIsNull_ShouldReturnZero() {
        // When & Then
        assertThat(entity.hashCode()).isEqualTo(0);
    }

    @Test
    void hashCode_WhenVersionIsSet_ShouldReturnVersionHashCode() {
        // Given
        Long version = 1L;
        entity.setVersion(version);

        // When & Then
        assertThat(entity.hashCode()).isEqualTo(version.hashCode());
    }

    @Test
    void toString_ShouldContainAllAuditFields() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedDate(now);
        entity.setLastModifiedDate(now);
        entity.setCreatedBy("creator");
        entity.setLastModifiedBy("modifier");
        entity.setVersion(1L);
        entity.setDeleted(true);

        // When
        String result = entity.toString();

        // Then
        assertThat(result).contains("TestAuditableEntity");
        assertThat(result).contains("createdDate=" + now);
        assertThat(result).contains("lastModifiedDate=" + now);
        assertThat(result).contains("createdBy='creator'");
        assertThat(result).contains("lastModifiedBy='modifier'");
        assertThat(result).contains("version=1");
        assertThat(result).contains("deleted=true");
    }

    /**
     * Test implementation of AuditableEntity for testing purposes.
     */
    private static class TestAuditableEntity extends AuditableEntity {
        // No additional fields needed for testing the base class
    }
}