package com.enterprise.api.audit;

import com.enterprise.api.entity.AuditLog;
import com.enterprise.api.entity.AuditableEntity;
import com.enterprise.api.repository.AuditLogRepository;
import jakarta.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for audit functionality.
 * Tests the complete audit flow including JPA auditing and audit log creation.
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.show-sql=true"
})
@ActiveProfiles("test")
class AuditIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        // Set up security context with a test user
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", "password"));
        
        // Initialize audit service manually for testing
        auditService = new AuditServiceImpl(auditLogRepository);
    }

    @Test
    void auditableEntity_WhenSaved_ShouldPopulateAuditFields() {
        // Given
        TestEntity entity = new TestEntity();
        entity.setName("Test Entity");

        // When
        TestEntity savedEntity = entityManager.persistAndFlush(entity);

        // Then
        assertThat(savedEntity.getCreatedDate()).isNotNull();
        assertThat(savedEntity.getCreatedBy()).isEqualTo("testuser");
        assertThat(savedEntity.getLastModifiedDate()).isNotNull();
        assertThat(savedEntity.getLastModifiedBy()).isEqualTo("testuser");
        assertThat(savedEntity.getVersion()).isEqualTo(0L);
        assertThat(savedEntity.isDeleted()).isFalse();
    }

    @Test
    void auditableEntity_WhenUpdated_ShouldUpdateAuditFields() {
        // Given
        TestEntity entity = new TestEntity();
        entity.setName("Original Name");
        TestEntity savedEntity = entityManager.persistAndFlush(entity);
        
        LocalDateTime originalCreatedDate = savedEntity.getCreatedDate();
        String originalCreatedBy = savedEntity.getCreatedBy();
        Long originalVersion = savedEntity.getVersion();

        entityManager.clear();

        // When
        TestEntity foundEntity = entityManager.find(TestEntity.class, savedEntity.getId());
        foundEntity.setName("Updated Name");
        TestEntity updatedEntity = entityManager.persistAndFlush(foundEntity);

        // Then
        assertThat(updatedEntity.getCreatedDate()).isEqualTo(originalCreatedDate);
        assertThat(updatedEntity.getCreatedBy()).isEqualTo(originalCreatedBy);
        assertThat(updatedEntity.getLastModifiedDate()).isAfter(originalCreatedDate);
        assertThat(updatedEntity.getLastModifiedBy()).isEqualTo("testuser");
        assertThat(updatedEntity.getVersion()).isGreaterThan(originalVersion);
    }

    @Test
    void auditService_ShouldSaveAndRetrieveAuditLogs() {
        // Given
        AuditLog auditLog = new AuditLog("TestEntity", "1", AuditLog.AuditOperation.CREATE, "testuser");
        auditLog.setNewValues("{\"name\":\"Test Entity\"}");

        // When
        AuditLog savedAuditLog = auditService.saveAuditLog(auditLog);

        // Then
        assertThat(savedAuditLog.getId()).isNotNull();
        
        List<AuditLog> auditLogs = auditService.findByEntityNameAndEntityId("TestEntity", "1");
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.get(0).getOperation()).isEqualTo(AuditLog.AuditOperation.CREATE);
        assertThat(auditLogs.get(0).getPerformedBy()).isEqualTo("testuser");
    }

    @Test
    void auditService_ShouldCreateAuditLogProgrammatically() {
        // When
        AuditLog auditLog = auditService.createAuditLog(
                "User", "123", AuditLog.AuditOperation.UPDATE,
                "{\"name\":\"old\"}", "{\"name\":\"new\"}", "testuser");

        // Then
        assertThat(auditLog.getId()).isNotNull();
        assertThat(auditLog.getEntityName()).isEqualTo("User");
        assertThat(auditLog.getEntityId()).isEqualTo("123");
        assertThat(auditLog.getOperation()).isEqualTo(AuditLog.AuditOperation.UPDATE);
        assertThat(auditLog.getOldValues()).isEqualTo("{\"name\":\"old\"}");
        assertThat(auditLog.getNewValues()).isEqualTo("{\"name\":\"new\"}");
        assertThat(auditLog.getPerformedBy()).isEqualTo("testuser");
    }

    @Test
    void auditService_ShouldCreateAuditLogWithContext() {
        // When
        AuditLog auditLog = auditService.createAuditLog(
                "User", "123", AuditLog.AuditOperation.DELETE,
                "{\"name\":\"deleted\"}", null, "testuser",
                "192.168.1.1", "Mozilla/5.0", "Deletion context");

        // Then
        assertThat(auditLog.getId()).isNotNull();
        assertThat(auditLog.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(auditLog.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(auditLog.getAdditionalInfo()).isEqualTo("Deletion context");
    }

    @Test
    void auditableEntity_SoftDelete_ShouldMarkAsDeleted() {
        // Given
        TestEntity entity = new TestEntity();
        entity.setName("To be deleted");
        TestEntity savedEntity = entityManager.persistAndFlush(entity);

        // When
        savedEntity.markAsDeleted();
        TestEntity updatedEntity = entityManager.persistAndFlush(savedEntity);

        // Then
        assertThat(updatedEntity.isDeleted()).isTrue();
    }

    @Test
    void auditableEntity_Restore_ShouldUnmarkDeleted() {
        // Given
        TestEntity entity = new TestEntity();
        entity.setName("Deleted entity");
        entity.markAsDeleted();
        TestEntity savedEntity = entityManager.persistAndFlush(entity);
        assertThat(savedEntity.isDeleted()).isTrue();

        // When
        savedEntity.restore();
        TestEntity restoredEntity = entityManager.persistAndFlush(savedEntity);

        // Then
        assertThat(restoredEntity.isDeleted()).isFalse();
    }

    /**
     * Test entity that extends AuditableEntity for integration testing.
     */
    @Entity
    @Table(name = "test_entities")
    public static class TestEntity extends AuditableEntity {
        
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        @Column(name = "name")
        private String name;
        
        // Constructors
        public TestEntity() {}
        
        public TestEntity(String name) {
            this.name = name;
        }
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * Test configuration for JPA auditing.
     */
    @Configuration
    @EnableJpaAuditing(auditorAwareRef = "testAuditorAware")
    static class TestAuditConfig {
        
        @Bean
        public AuditorAware<String> testAuditorAware() {
            return new AuditAware();
        }
    }
}