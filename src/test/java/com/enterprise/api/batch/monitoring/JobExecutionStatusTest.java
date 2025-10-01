package com.enterprise.api.batch.monitoring;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for JobExecutionStatus record.
 * Tests job execution status functionality and calculations.
 * 
 * Requirements: 6.6
 */
class JobExecutionStatusTest {

    @Test
    void shouldCalculateExecutionDuration() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(5);
        LocalDateTime endTime = LocalDateTime.now();
        JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
        
        JobExecutionStatus status = new JobExecutionStatus(
                1L, "testJob", BatchStatus.COMPLETED, ExitStatus.COMPLETED,
                startTime, endTime, jobParameters
        );

        // When
        Long duration = status.getExecutionDurationMs();

        // Then
        assertThat(duration).isNotNull();
        assertThat(duration).isGreaterThan(0);
        assertThat(duration).isLessThan(6 * 60 * 1000); // Less than 6 minutes
    }

    @Test
    void shouldCalculateExecutionDurationForRunningJob() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(2);
        JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
        
        JobExecutionStatus status = new JobExecutionStatus(
                1L, "testJob", BatchStatus.STARTED, ExitStatus.EXECUTING,
                startTime, null, jobParameters
        );

        // When
        Long duration = status.getExecutionDurationMs();

        // Then
        assertThat(duration).isNotNull();
        assertThat(duration).isGreaterThan(0);
    }

    @Test
    void shouldReturnNullDurationWhenNoStartTime() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
        
        JobExecutionStatus status = new JobExecutionStatus(
                1L, "testJob", BatchStatus.COMPLETED, ExitStatus.COMPLETED,
                null, LocalDateTime.now(), jobParameters
        );

        // When
        Long duration = status.getExecutionDurationMs();

        // Then
        assertNull(duration);
    }

    @Test
    void shouldIdentifyRunningJob() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
        
        JobExecutionStatus startedStatus = new JobExecutionStatus(
                1L, "testJob", BatchStatus.STARTED, ExitStatus.EXECUTING,
                LocalDateTime.now(), null, jobParameters
        );
        
        JobExecutionStatus startingStatus = new JobExecutionStatus(
                2L, "testJob", BatchStatus.STARTING, ExitStatus.EXECUTING,
                LocalDateTime.now(), null, jobParameters
        );

        // When & Then
        assertTrue(startedStatus.isRunning());
        assertTrue(startingStatus.isRunning());
    }

    @Test
    void shouldIdentifyNonRunningJob() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
        
        JobExecutionStatus completedStatus = new JobExecutionStatus(
                1L, "testJob", BatchStatus.COMPLETED, ExitStatus.COMPLETED,
                LocalDateTime.now(), LocalDateTime.now(), jobParameters
        );
        
        JobExecutionStatus failedStatus = new JobExecutionStatus(
                2L, "testJob", BatchStatus.FAILED, ExitStatus.FAILED,
                LocalDateTime.now(), LocalDateTime.now(), jobParameters
        );

        // When & Then
        assertFalse(completedStatus.isRunning());
        assertFalse(failedStatus.isRunning());
    }

    @Test
    void shouldIdentifyCompletedJob() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
        
        JobExecutionStatus status = new JobExecutionStatus(
                1L, "testJob", BatchStatus.COMPLETED, ExitStatus.COMPLETED,
                LocalDateTime.now(), LocalDateTime.now(), jobParameters
        );

        // When & Then
        assertTrue(status.isCompleted());
        assertFalse(status.isFailed());
        assertFalse(status.isRunning());
    }

    @Test
    void shouldIdentifyFailedJob() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
        
        JobExecutionStatus status = new JobExecutionStatus(
                1L, "testJob", BatchStatus.FAILED, ExitStatus.FAILED,
                LocalDateTime.now(), LocalDateTime.now(), jobParameters
        );

        // When & Then
        assertTrue(status.isFailed());
        assertFalse(status.isCompleted());
        assertFalse(status.isRunning());
    }

    @Test
    void shouldProvideStatusDescriptions() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder().toJobParameters();

        // Test all status descriptions
        assertThat(createStatus(BatchStatus.STARTING).getStatusDescription())
                .isEqualTo("Job is starting");
        assertThat(createStatus(BatchStatus.STARTED).getStatusDescription())
                .isEqualTo("Job is running");
        assertThat(createStatus(BatchStatus.COMPLETED).getStatusDescription())
                .isEqualTo("Job completed successfully");
        assertThat(createStatus(BatchStatus.STOPPED).getStatusDescription())
                .isEqualTo("Job was stopped");
        assertThat(createStatus(BatchStatus.STOPPING).getStatusDescription())
                .isEqualTo("Job is stopping");
        assertThat(createStatus(BatchStatus.ABANDONED).getStatusDescription())
                .isEqualTo("Job was abandoned");
        assertThat(createStatus(BatchStatus.UNKNOWN).getStatusDescription())
                .isEqualTo("Job status unknown");
    }

    @Test
    void shouldProvideFailedStatusDescriptionWithExitStatus() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
        ExitStatus exitStatus = new ExitStatus("FAILED", "Database connection failed");
        
        JobExecutionStatus status = new JobExecutionStatus(
                1L, "testJob", BatchStatus.FAILED, exitStatus,
                LocalDateTime.now(), LocalDateTime.now(), jobParameters
        );

        // When
        String description = status.getStatusDescription();

        // Then
        assertThat(description).contains("Job failed");
        assertThat(description).contains("Database connection failed");
    }

    private JobExecutionStatus createStatus(BatchStatus batchStatus) {
        JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
        return new JobExecutionStatus(
                1L, "testJob", batchStatus, ExitStatus.EXECUTING,
                LocalDateTime.now(), null, jobParameters
        );
    }
}