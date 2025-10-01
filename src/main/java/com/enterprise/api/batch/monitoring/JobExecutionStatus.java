package com.enterprise.api.batch.monitoring;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;

import java.time.LocalDateTime;

/**
 * Data class representing the status of a job execution.
 * Contains all relevant information about a batch job execution.
 * 
 * Requirements: 6.6
 */
public record JobExecutionStatus(
        Long executionId,
        String jobName,
        BatchStatus status,
        ExitStatus exitStatus,
        LocalDateTime startTime,
        LocalDateTime endTime,
        JobParameters jobParameters
) {
    
    /**
     * Calculates the execution duration in milliseconds.
     * 
     * @return execution duration or null if not completed
     */
    public Long getExecutionDurationMs() {
        if (startTime == null) {
            return null;
        }
        
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMillis();
    }
    
    /**
     * Checks if the job execution is currently running.
     * 
     * @return true if job is running, false otherwise
     */
    public boolean isRunning() {
        return status == BatchStatus.STARTED || status == BatchStatus.STARTING;
    }
    
    /**
     * Checks if the job execution completed successfully.
     * 
     * @return true if job completed successfully, false otherwise
     */
    public boolean isCompleted() {
        return status == BatchStatus.COMPLETED;
    }
    
    /**
     * Checks if the job execution failed.
     * 
     * @return true if job failed, false otherwise
     */
    public boolean isFailed() {
        return status == BatchStatus.FAILED;
    }
    
    /**
     * Gets a human-readable status description.
     * 
     * @return status description
     */
    public String getStatusDescription() {
        return switch (status) {
            case STARTING -> "Job is starting";
            case STARTED -> "Job is running";
            case COMPLETED -> "Job completed successfully";
            case FAILED -> "Job failed: " + (exitStatus != null ? exitStatus.getExitDescription() : "Unknown error");
            case STOPPED -> "Job was stopped";
            case STOPPING -> "Job is stopping";
            case ABANDONED -> "Job was abandoned";
            case UNKNOWN -> "Job status unknown";
        };
    }
}