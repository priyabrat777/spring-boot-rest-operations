package com.enterprise.api.batch.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BatchJobMonitoringService.
 * Tests job monitoring and status tracking functionality.
 * 
 * Requirements: 6.6
 */
@ExtendWith(MockitoExtension.class)
class BatchJobMonitoringServiceTest {

    @Mock
    private JobExplorer jobExplorer;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private JobExecution jobExecution;

    @Mock
    private JobInstance jobInstance;

    @Mock
    private Job job;

    private BatchJobMonitoringService monitoringService;
    private Map<String, Job> jobRegistry;

    @BeforeEach
    void setUp() {
        jobRegistry = new HashMap<>();
        jobRegistry.put("testJob", job);
        monitoringService = new BatchJobMonitoringService(jobExplorer, jobRepository, jobLauncher, jobRegistry);
    }

    @Test
    void shouldGetJobExecutionStatus() {
        // Given
        Long executionId = 1L;
        when(jobExplorer.getJobExecution(executionId)).thenReturn(jobExecution);
        when(jobExecution.getId()).thenReturn(executionId);
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("testJob");
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
        when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now());
        when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());
        when(jobExecution.getJobParameters()).thenReturn(new JobParametersBuilder().toJobParameters());

        // When
        Optional<JobExecutionStatus> result = monitoringService.getJobExecutionStatus(executionId);

        // Then
        assertTrue(result.isPresent());
        assertThat(result.get().executionId()).isEqualTo(executionId);
        assertThat(result.get().jobName()).isEqualTo("testJob");
        assertThat(result.get().status()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void shouldReturnEmptyForNonExistentJobExecution() {
        // Given
        Long executionId = 999L;
        when(jobExplorer.getJobExecution(executionId)).thenReturn(null);

        // When
        Optional<JobExecutionStatus> result = monitoringService.getJobExecutionStatus(executionId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void shouldGetJobExecutions() {
        // Given
        String jobName = "testJob";
        int limit = 10;
        List<JobInstance> jobInstances = Arrays.asList(jobInstance);
        List<JobExecution> jobExecutions = Arrays.asList(jobExecution);

        when(jobExplorer.getJobInstances(jobName, 0, limit)).thenReturn(jobInstances);
        when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(jobExecutions);
        when(jobExecution.getId()).thenReturn(1L);
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn(jobName);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);
        when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now());
        when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());
        when(jobExecution.getJobParameters()).thenReturn(new JobParametersBuilder().toJobParameters());

        // When
        List<JobExecutionStatus> result = monitoringService.getJobExecutions(jobName, limit);

        // Then
        assertNotNull(result);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).jobName()).isEqualTo(jobName);
    }

    @Test
    void shouldGetRunningJobExecutions() {
        // Given
        List<String> jobNames = List.of("testJob");
        List<JobInstance> jobInstances = Arrays.asList(jobInstance);
        List<JobExecution> jobExecutions = Arrays.asList(jobExecution);

        when(jobExplorer.getJobNames()).thenReturn(jobNames);
        when(jobExplorer.getJobInstances("testJob", 0, 100)).thenReturn(jobInstances);
        when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(jobExecutions);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);
        when(jobExecution.getId()).thenReturn(1L);
        when(jobExecution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn("testJob");
        when(jobExecution.getExitStatus()).thenReturn(ExitStatus.EXECUTING);
        when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now());
        when(jobExecution.getJobParameters()).thenReturn(new JobParametersBuilder().toJobParameters());

        // When
        List<JobExecutionStatus> result = monitoringService.getRunningJobExecutions();

        // Then
        assertNotNull(result);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(BatchStatus.STARTED);
    }

    @Test
    void shouldGetJobStatistics() {
        // Given
        String jobName = "testJob";
        List<JobInstance> jobInstances = Arrays.asList(jobInstance);
        List<JobExecution> jobExecutions = Arrays.asList(jobExecution);

        when(jobExplorer.getJobInstances(jobName, 0, 1000)).thenReturn(jobInstances);
        when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(jobExecutions);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStartTime()).thenReturn(LocalDateTime.now().minusSeconds(5));
        when(jobExecution.getEndTime()).thenReturn(LocalDateTime.now());

        // When
        JobStatistics result = monitoringService.getJobStatistics(jobName);

        // Then
        assertNotNull(result);
        assertThat(result.jobName()).isEqualTo(jobName);
        assertThat(result.totalExecutions()).isEqualTo(1);
        assertThat(result.completedExecutions()).isEqualTo(1);
        assertThat(result.failedExecutions()).isEqualTo(0);
        assertThat(result.runningExecutions()).isEqualTo(0);
    }

    @Test
    void shouldStopJobExecution() {
        // Given
        Long executionId = 1L;
        when(jobExplorer.getJobExecution(executionId)).thenReturn(jobExecution);
        when(jobExecution.isRunning()).thenReturn(true);

        // When
        boolean result = monitoringService.stopJobExecution(executionId);

        // Then
        assertTrue(result);
        verify(jobExecution).setStatus(BatchStatus.STOPPING);
        verify(jobRepository).update(jobExecution);
    }

    @Test
    void shouldNotStopNonRunningJobExecution() {
        // Given
        Long executionId = 1L;
        when(jobExplorer.getJobExecution(executionId)).thenReturn(jobExecution);
        when(jobExecution.isRunning()).thenReturn(false);

        // When
        boolean result = monitoringService.stopJobExecution(executionId);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldNotStopNonExistentJobExecution() {
        // Given
        Long executionId = 999L;
        when(jobExplorer.getJobExecution(executionId)).thenReturn(null);

        // When
        boolean result = monitoringService.stopJobExecution(executionId);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldGetAvailableJobNames() {
        // Given
        List<String> jobNames = List.of("job1", "job2", "job3");
        when(jobExplorer.getJobNames()).thenReturn(jobNames);

        // When
        Set<String> result = monitoringService.getAvailableJobNames();

        // Then
        assertThat(result).containsExactlyInAnyOrder("job1", "job2", "job3");
    }

    @Test
    void shouldCheckIfJobIsRunning() {
        // Given
        String jobName = "testJob";
        List<JobInstance> jobInstances = Arrays.asList(jobInstance);
        List<JobExecution> jobExecutions = Arrays.asList(jobExecution);

        when(jobExplorer.getJobInstances(jobName, 0, 10)).thenReturn(jobInstances);
        when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(jobExecutions);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);

        // When
        boolean result = monitoringService.isJobRunning(jobName);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldCheckIfJobIsNotRunning() {
        // Given
        String jobName = "testJob";
        List<JobInstance> jobInstances = Arrays.asList(jobInstance);
        List<JobExecution> jobExecutions = Arrays.asList(jobExecution);

        when(jobExplorer.getJobInstances(jobName, 0, 10)).thenReturn(jobInstances);
        when(jobExplorer.getJobExecutions(jobInstance)).thenReturn(jobExecutions);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

        // When
        boolean result = monitoringService.isJobRunning(jobName);

        // Then
        assertFalse(result);
    }
}