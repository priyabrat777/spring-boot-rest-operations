package com.enterprise.api.repository;

import com.enterprise.api.entity.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuditLogRepository.
 */
@DataJpaTest
@ActiveProfiles("test")
class AuditLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private AuditLog testAuditLog1;
    private AuditLog testAuditLog2;

    @BeforeEach
    void setUp() {
        testAuditLog1 = new AuditLog("User", "1", AuditLog.AuditOperation.CREATE, "testuser1");
        testAuditLog1.setNewValues("{\"name\":\"John Doe\"}");
        testAuditLog1.setIpAddress("192.168.1.1");

        testAuditLog2 = new AuditLog("User", "1", AuditLog.AuditOperation.UPDATE, "testuser2");
        testAuditLog2.setOldValues("{\"name\":\"John Doe\"}");
        testAuditLog2.setNewValues("{\"name\":\"Jane Doe\"}");
        testAuditLog2.setIpAddress("192.168.1.2");

        entityManager.persistAndFlush(testAuditLog1);
        entityManager.persistAndFlush(testAuditLog2);
    }

    @Test
    void findByEntityNameAndEntityIdOrderByPerformedAtDesc_ShouldReturnAuditLogs() {
        // When
        List<AuditLog> result = auditLogRepository.findByEntityNameAndEntityIdOrderByPerformedAtDesc("User", "1");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPerformedAt()).isAfterOrEqualTo(result.get(1).getPerformedAt());
    }

    @Test
    void findByEntityNameAndEntityIdOrderByPerformedAtDescWithPageable_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<AuditLog> result = auditLogRepository.findByEntityNameAndEntityIdOrderByPerformedAtDesc("User", "1", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findByOperationOrderByPerformedAtDesc_ShouldReturnAuditLogsByOperation() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<AuditLog> result = auditLogRepository.findByOperationOrderByPerformedAtDesc(AuditLog.AuditOperation.CREATE, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOperation()).isEqualTo(AuditLog.AuditOperation.CREATE);
    }

    @Test
    void findByPerformedByAndPerformedAtBetweenOrderByPerformedAtDesc_ShouldReturnAuditLogsByUserAndDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<AuditLog> result = auditLogRepository.findByPerformedByAndPerformedAtBetweenOrderByPerformedAtDesc(
                "testuser1", startDate, endDate, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPerformedBy()).isEqualTo("testuser1");
    }

    @Test
    void findByPerformedAtBetweenOrderByPerformedAtDesc_ShouldReturnAuditLogsByDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<AuditLog> result = auditLogRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(startDate, endDate, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findAllByOrderByPerformedAtDesc_ShouldReturnAllAuditLogs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<AuditLog> result = auditLogRepository.findAllByOrderByPerformedAtDesc(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getPerformedAt())
                .isAfterOrEqualTo(result.getContent().get(1).getPerformedAt());
    }

    @Test
    void countByEntityNameAndEntityId_ShouldReturnCount() {
        // When
        long count = auditLogRepository.countByEntityNameAndEntityId("User", "1");

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void countByPerformedByAndPerformedAtBetween_ShouldReturnCountByUserAndDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);

        // When
        long count = auditLogRepository.countByPerformedByAndPerformedAtBetween("testuser1", startDate, endDate);

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void findByIpAddressOrderByPerformedAtDesc_ShouldReturnAuditLogsByIpAddress() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<AuditLog> result = auditLogRepository.findByIpAddressOrderByPerformedAtDesc("192.168.1.1", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    void findWithFilters_ShouldReturnFilteredResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<AuditLog> result = auditLogRepository.findWithFilters(
                "User", "1", AuditLog.AuditOperation.CREATE, "testuser1", null, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOperation()).isEqualTo(AuditLog.AuditOperation.CREATE);
        assertThat(result.getContent().get(0).getPerformedBy()).isEqualTo("testuser1");
    }

    @Test
    void findSuspiciousActivities_ShouldReturnSuspiciousLogs() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusHours(1);

        // When
        List<AuditLog> result = auditLogRepository.findSuspiciousActivities(
                "192.168.1.1", AuditLog.AuditOperation.CREATE, since);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(result.get(0).getOperation()).isEqualTo(AuditLog.AuditOperation.CREATE);
    }
}