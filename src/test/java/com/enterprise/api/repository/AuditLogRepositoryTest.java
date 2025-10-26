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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuditLogRepository.
 */
@DataJpaTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class AuditLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private AuditLog testAuditLog1;
    private AuditLog testAuditLog2;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        auditLogRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
        
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

    @Test
    void findByEntityNameAndEntityIdIn_ShouldReturnAuditLogsForMultipleEntities() {
        // Given
        AuditLog auditLog3 = new AuditLog("User", "2", AuditLog.AuditOperation.CREATE, "testuser3");
        entityManager.persistAndFlush(auditLog3);
        List<String> entityIds = List.of("1", "2");

        // When
        List<AuditLog> result = auditLogRepository.findByEntityNameAndEntityIdIn("User", entityIds);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(AuditLog::getEntityId)
                .containsExactlyInAnyOrder("1", "1", "2");
    }

    @Test
    void findByEntityNameAndEntityIdInWithPagination_ShouldReturnPagedResults() {
        // Given
        AuditLog auditLog3 = new AuditLog("User", "2", AuditLog.AuditOperation.CREATE, "testuser3");
        entityManager.persistAndFlush(auditLog3);
        List<String> entityIds = List.of("1", "2");
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<AuditLog> result = auditLogRepository.findByEntityNameAndEntityIdIn("User", entityIds, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findBySessionIdOrderByPerformedAtDesc_ShouldReturnAuditLogsBySession() {
        // Given
        testAuditLog1.setSessionId("session123");
        testAuditLog2.setSessionId("session123");
        entityManager.merge(testAuditLog1);
        entityManager.merge(testAuditLog2);
        entityManager.flush();

        // When
        List<AuditLog> result = auditLogRepository.findBySessionIdOrderByPerformedAtDesc("session123");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(log -> "session123".equals(log.getSessionId()));
    }

    @Test
    void findRecentByUser_ShouldReturnRecentUserActivity() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusHours(1);

        // When
        List<AuditLog> result = auditLogRepository.findRecentByUser("testuser1", since);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPerformedBy()).isEqualTo("testuser1");
    }

    @Test
    void findByOperationAndEntityNameOrderByPerformedAtDesc_ShouldReturnFilteredLogs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<AuditLog> result = auditLogRepository.findByOperationAndEntityNameOrderByPerformedAtDesc(
                AuditLog.AuditOperation.CREATE, "User", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOperation()).isEqualTo(AuditLog.AuditOperation.CREATE);
        assertThat(result.getContent().get(0).getEntityName()).isEqualTo("User");
    }

    @Test
    void findLogsWithChanges_ShouldReturnLogsWithOldOrNewValues() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<AuditLog> result = auditLogRepository.findLogsWithChanges(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(log -> 
                log.getOldValues() != null || log.getNewValues() != null);
    }

    @Test
    void findLogsWithoutChanges_ShouldReturnLogsWithoutValues() {
        // Given
        AuditLog logWithoutChanges = new AuditLog("User", "3", AuditLog.AuditOperation.READ, "testuser");
        entityManager.persistAndFlush(logWithoutChanges);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<AuditLog> result = auditLogRepository.findLogsWithoutChanges(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOldValues()).isNull();
        assertThat(result.getContent().get(0).getNewValues()).isNull();
    }

    @Test
    void countByOperation_ShouldReturnCorrectCount() {
        // When
        long createCount = auditLogRepository.countByOperation(AuditLog.AuditOperation.CREATE);
        long updateCount = auditLogRepository.countByOperation(AuditLog.AuditOperation.UPDATE);

        // Then
        assertThat(createCount).isEqualTo(1);
        assertThat(updateCount).isEqualTo(1);
    }

    @Test
    void countByEntityName_ShouldReturnCorrectCount() {
        // When
        long userCount = auditLogRepository.countByEntityName("User");

        // Then
        assertThat(userCount).isEqualTo(2);
    }

    @Test
    void countByPerformedBy_ShouldReturnCorrectCount() {
        // When
        long user1Count = auditLogRepository.countByPerformedBy("testuser1");
        long user2Count = auditLogRepository.countByPerformedBy("testuser2");

        // Then
        assertThat(user1Count).isEqualTo(1);
        assertThat(user2Count).isEqualTo(1);
    }

    @Test
    void getAuditStatsByOperation_ShouldReturnOperationStatistics() {
        // When
        List<Object[]> stats = auditLogRepository.getAuditStatsByOperation();

        // Then
        assertThat(stats).hasSize(2);
        assertThat(stats).anySatisfy(stat -> {
            assertThat(stat[0]).isEqualTo(AuditLog.AuditOperation.CREATE);
            assertThat(stat[1]).isEqualTo(1L);
        });
        assertThat(stats).anySatisfy(stat -> {
            assertThat(stat[0]).isEqualTo(AuditLog.AuditOperation.UPDATE);
            assertThat(stat[1]).isEqualTo(1L);
        });
    }

    @Test
    void getAuditStatsByEntityName_ShouldReturnEntityStatistics() {
        // When
        List<Object[]> stats = auditLogRepository.getAuditStatsByEntityName();

        // Then
        assertThat(stats).hasSize(1);
        assertThat(stats.get(0)[0]).isEqualTo("User");
        assertThat(stats.get(0)[1]).isEqualTo(2L);
    }

    @Test
    void getAuditStatsByUser_ShouldReturnUserStatistics() {
        // When
        List<Object[]> stats = auditLogRepository.getAuditStatsByUser();

        // Then
        assertThat(stats).hasSize(2);
        // Should be ordered by count descending, but both have count 1, so order may vary
        assertThat(stats).anySatisfy(stat -> {
            assertThat(stat[0]).isIn("testuser1", "testuser2");
            assertThat(stat[1]).isEqualTo(1L);
        });
    }

    @Test
    void getDailyAuditStats_ShouldReturnDailyStatistics() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        // When
        List<Object[]> stats = auditLogRepository.getDailyAuditStats(startDate, endDate);

        // Then
        assertThat(stats).hasSize(1);
        assertThat(stats.get(0)[1]).isEqualTo(2L); // Count of logs for today
    }

    @Test
    void findMostActiveUsers_ShouldReturnActiveUsers() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);

        // When
        List<Object[]> activeUsers = auditLogRepository.findMostActiveUsers(startDate, endDate, 5);

        // Then
        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers).anySatisfy(user -> {
            assertThat(user[0]).isIn("testuser1", "testuser2");
            assertThat(user[1]).isEqualTo(1L);
        });
    }

    @Test
    void findMostAccessedEntities_ShouldReturnAccessedEntities() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);

        // When
        List<Object[]> accessedEntities = auditLogRepository.findMostAccessedEntities(startDate, endDate, 5);

        // Then
        assertThat(accessedEntities).hasSize(1);
        assertThat(accessedEntities.get(0)[0]).isEqualTo("User");
        assertThat(accessedEntities.get(0)[1]).isEqualTo("1");
        assertThat(accessedEntities.get(0)[2]).isEqualTo(2L);
    }

    @Test
    void findFailedOperations_ShouldReturnFailedOperations() {
        // Given
        AuditLog failedLog = new AuditLog("User", "1", AuditLog.AuditOperation.ACCESS_DENIED, "testuser");
        entityManager.persistAndFlush(failedLog);
        
        List<AuditLog.AuditOperation> failureOperations = List.of(AuditLog.AuditOperation.ACCESS_DENIED);
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<AuditLog> result = auditLogRepository.findFailedOperations(failureOperations, startDate, endDate, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOperation()).isEqualTo(AuditLog.AuditOperation.ACCESS_DENIED);
    }

    @Test
    void findByIpAddressPattern_ShouldReturnMatchingLogs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<AuditLog> result = auditLogRepository.findByIpAddressPattern("192.168.1.%", pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(log -> log.getIpAddress().startsWith("192.168.1."));
    }

    @Test
    void findByUserAgentPattern_ShouldReturnMatchingLogs() {
        // Given
        testAuditLog1.setUserAgent("Mozilla/5.0 Chrome");
        testAuditLog2.setUserAgent("Mozilla/5.0 Firefox");
        entityManager.merge(testAuditLog1);
        entityManager.merge(testAuditLog2);
        entityManager.flush();
        
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<AuditLog> result = auditLogRepository.findByUserAgentPattern("Mozilla%", pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(log -> log.getUserAgent().startsWith("Mozilla"));
    }

    @Test
    void deleteOldAuditLogs_ShouldDeleteOldLogs() {
        // Given - Set one log to be old
        testAuditLog1.setPerformedAt(LocalDateTime.now().minusDays(30));
        entityManager.merge(testAuditLog1);
        entityManager.flush();
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);

        // When
        int deletedCount = auditLogRepository.deleteOldAuditLogs(cutoffDate);

        // Then
        assertThat(deletedCount).isEqualTo(1);
        
        // Verify the old log is deleted
        List<AuditLog> remainingLogs = auditLogRepository.findAll();
        assertThat(remainingLogs).hasSize(1);
        assertThat(remainingLogs.get(0).getId()).isEqualTo(testAuditLog2.getId());
    }

    @Test
    void archiveOldAuditLogs_ShouldArchiveOldLogs() {
        // Given - Set one log to be old
        testAuditLog1.setPerformedAt(LocalDateTime.now().minusDays(30));
        entityManager.merge(testAuditLog1);
        entityManager.flush();
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        String archiveInfo = " [ARCHIVED]";

        // When
        int archivedCount = auditLogRepository.archiveOldAuditLogs(cutoffDate, archiveInfo);

        // Then
        assertThat(archivedCount).isEqualTo(1);
        
        entityManager.clear();
        AuditLog archivedLog = entityManager.find(AuditLog.class, testAuditLog1.getId());
        assertThat(archivedLog.getAdditionalInfo()).contains("[ARCHIVED]");
    }

    @Test
    void findForComplianceReport_ShouldReturnComplianceLogs() {
        // Given
        List<String> entityNames = List.of("User");
        List<AuditLog.AuditOperation> operations = List.of(AuditLog.AuditOperation.CREATE, AuditLog.AuditOperation.UPDATE);
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<AuditLog> result = auditLogRepository.findForComplianceReport(entityNames, operations, startDate, endDate, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(log -> 
                entityNames.contains(log.getEntityName()) && operations.contains(log.getOperation()));
    }

    @Test
    void findFirstByEntityNameAndEntityId_ShouldReturnFirstLog() {
        // When
        AuditLog firstLog = auditLogRepository.findFirstByEntityNameAndEntityId("User", "1");

        // Then
        assertThat(firstLog).isNotNull();
        assertThat(firstLog.getOperation()).isEqualTo(AuditLog.AuditOperation.CREATE);
    }

    @Test
    void hasUserAccessedEntity_ShouldReturnTrueIfUserAccessedEntity() {
        // When
        boolean hasAccessed = auditLogRepository.hasUserAccessedEntity("User", "1", "testuser1");
        boolean hasNotAccessed = auditLogRepository.hasUserAccessedEntity("User", "1", "nonexistentuser");

        // Then
        assertThat(hasAccessed).isTrue();
        assertThat(hasNotAccessed).isFalse();
    }
}