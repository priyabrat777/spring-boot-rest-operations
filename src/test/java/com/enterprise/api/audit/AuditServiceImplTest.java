package com.enterprise.api.audit;

import com.enterprise.api.entity.AuditLog;
import com.enterprise.api.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditServiceImpl auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditServiceImpl(auditLogRepository);
    }

    @Test
    void saveAuditLog_ShouldSaveAndReturnAuditLog() {
        // Given
        AuditLog auditLog = createTestAuditLog();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(auditLog);

        // When
        AuditLog result = auditService.saveAuditLog(auditLog);

        // Then
        assertThat(result).isEqualTo(auditLog);
        verify(auditLogRepository).save(auditLog);
    }

    @Test
    void findByEntityNameAndEntityId_ShouldReturnAuditLogs() {
        // Given
        String entityName = "User";
        String entityId = "1";
        List<AuditLog> auditLogs = Arrays.asList(createTestAuditLog(), createTestAuditLog());
        when(auditLogRepository.findByEntityNameAndEntityIdOrderByPerformedAtDesc(entityName, entityId))
                .thenReturn(auditLogs);

        // When
        List<AuditLog> result = auditService.findByEntityNameAndEntityId(entityName, entityId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(auditLogs);
        verify(auditLogRepository).findByEntityNameAndEntityIdOrderByPerformedAtDesc(entityName, entityId);
    }

    @Test
    void findByEntityNameAndEntityIdWithPageable_ShouldReturnPagedAuditLogs() {
        // Given
        String entityName = "User";
        String entityId = "1";
        Pageable pageable = PageRequest.of(0, 10);
        List<AuditLog> auditLogs = Arrays.asList(createTestAuditLog());
        Page<AuditLog> page = new PageImpl<>(auditLogs, pageable, 1);
        when(auditLogRepository.findByEntityNameAndEntityIdOrderByPerformedAtDesc(entityName, entityId, pageable))
                .thenReturn(page);

        // When
        Page<AuditLog> result = auditService.findByEntityNameAndEntityId(entityName, entityId, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(auditLogRepository).findByEntityNameAndEntityIdOrderByPerformedAtDesc(entityName, entityId, pageable);
    }

    @Test
    void findByPerformedByAndDateRange_ShouldReturnPagedAuditLogs() {
        // Given
        String performedBy = "testuser";
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);
        List<AuditLog> auditLogs = Arrays.asList(createTestAuditLog());
        Page<AuditLog> page = new PageImpl<>(auditLogs, pageable, 1);
        when(auditLogRepository.findByPerformedByAndPerformedAtBetweenOrderByPerformedAtDesc(
                performedBy, startDate, endDate, pageable)).thenReturn(page);

        // When
        Page<AuditLog> result = auditService.findByPerformedByAndDateRange(performedBy, startDate, endDate, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findByPerformedByAndPerformedAtBetweenOrderByPerformedAtDesc(
                performedBy, startDate, endDate, pageable);
    }

    @Test
    void findByOperation_ShouldReturnPagedAuditLogs() {
        // Given
        AuditLog.AuditOperation operation = AuditLog.AuditOperation.CREATE;
        Pageable pageable = PageRequest.of(0, 10);
        List<AuditLog> auditLogs = Arrays.asList(createTestAuditLog());
        Page<AuditLog> page = new PageImpl<>(auditLogs, pageable, 1);
        when(auditLogRepository.findByOperationOrderByPerformedAtDesc(operation, pageable)).thenReturn(page);

        // When
        Page<AuditLog> result = auditService.findByOperation(operation, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findByOperationOrderByPerformedAtDesc(operation, pageable);
    }

    @Test
    void findByDateRange_ShouldReturnPagedAuditLogs() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);
        List<AuditLog> auditLogs = Arrays.asList(createTestAuditLog());
        Page<AuditLog> page = new PageImpl<>(auditLogs, pageable, 1);
        when(auditLogRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(startDate, endDate, pageable))
                .thenReturn(page);

        // When
        Page<AuditLog> result = auditService.findByDateRange(startDate, endDate, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findByPerformedAtBetweenOrderByPerformedAtDesc(startDate, endDate, pageable);
    }

    @Test
    void findById_ShouldReturnOptionalAuditLog() {
        // Given
        Long id = 1L;
        AuditLog auditLog = createTestAuditLog();
        when(auditLogRepository.findById(id)).thenReturn(Optional.of(auditLog));

        // When
        Optional<AuditLog> result = auditService.findById(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(auditLog);
        verify(auditLogRepository).findById(id);
    }

    @Test
    void findById_WhenNotFound_ShouldReturnEmptyOptional() {
        // Given
        Long id = 1L;
        when(auditLogRepository.findById(id)).thenReturn(Optional.empty());

        // When
        Optional<AuditLog> result = auditService.findById(id);

        // Then
        assertThat(result).isEmpty();
        verify(auditLogRepository).findById(id);
    }

    @Test
    void findAll_ShouldReturnPagedAuditLogs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<AuditLog> auditLogs = Arrays.asList(createTestAuditLog());
        Page<AuditLog> page = new PageImpl<>(auditLogs, pageable, 1);
        when(auditLogRepository.findAllByOrderByPerformedAtDesc(pageable)).thenReturn(page);

        // When
        Page<AuditLog> result = auditService.findAll(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findAllByOrderByPerformedAtDesc(pageable);
    }

    @Test
    void createAuditLog_ShouldCreateAndSaveAuditLog() {
        // Given
        String entityName = "User";
        String entityId = "1";
        AuditLog.AuditOperation operation = AuditLog.AuditOperation.CREATE;
        String oldValues = "{}";
        String newValues = "{\"name\":\"test\"}";
        String performedBy = "testuser";
        
        AuditLog savedAuditLog = createTestAuditLog();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedAuditLog);

        // When
        AuditLog result = auditService.createAuditLog(entityName, entityId, operation, oldValues, newValues, performedBy);

        // Then
        assertThat(result).isEqualTo(savedAuditLog);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void createAuditLogWithContext_ShouldCreateAndSaveAuditLogWithAdditionalInfo() {
        // Given
        String entityName = "User";
        String entityId = "1";
        AuditLog.AuditOperation operation = AuditLog.AuditOperation.CREATE;
        String oldValues = "{}";
        String newValues = "{\"name\":\"test\"}";
        String performedBy = "testuser";
        String ipAddress = "192.168.1.1";
        String userAgent = "Mozilla/5.0";
        String additionalInfo = "Additional context";
        
        AuditLog savedAuditLog = createTestAuditLog();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedAuditLog);

        // When
        AuditLog result = auditService.createAuditLog(entityName, entityId, operation, oldValues, newValues, 
                                                     performedBy, ipAddress, userAgent, additionalInfo);

        // Then
        assertThat(result).isEqualTo(savedAuditLog);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    private AuditLog createTestAuditLog() {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(1L);
        auditLog.setEntityName("User");
        auditLog.setEntityId("1");
        auditLog.setOperation(AuditLog.AuditOperation.CREATE);
        auditLog.setPerformedBy("testuser");
        auditLog.setPerformedAt(LocalDateTime.now());
        return auditLog;
    }
}