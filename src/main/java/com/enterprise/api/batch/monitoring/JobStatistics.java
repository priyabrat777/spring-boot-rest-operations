package com.enterprise.api.batch.monitoring;

/**
 * Data class representing statistics for a batch job.
 * Contains aggregated metrics about job executions.
 * 
 * Requirements: 6.6
 */
public record JobStatistics(
        String jobName,
        long totalExecutions,
        long completedExecutions,
        long failedExecutions,
        long runningExecutions,
        double averageExecutionTimeMs
) {
    
    /**
     * Calculates the success rate as a percentage.
     * 
     * @return success rate (0-100)
     */
    public double getSuccessRate() {
        if (totalExecutions == 0) {
            return 0.0;
        }
        return (double) completedExecutions / totalExecutions * 100.0;
    }
    
    /**
     * Calculates the failure rate as a percentage.
     * 
     * @return failure rate (0-100)
     */
    public double getFailureRate() {
        if (totalExecutions == 0) {
            return 0.0;
        }
        return (double) failedExecutions / totalExecutions * 100.0;
    }
    
    /**
     * Gets the number of non-running executions.
     * 
     * @return count of completed or failed executions
     */
    public long getFinishedExecutions() {
        return completedExecutions + failedExecutions;
    }
    
    /**
     * Formats the average execution time as a human-readable string.
     * 
     * @return formatted execution time
     */
    public String getFormattedAverageExecutionTime() {
        if (averageExecutionTimeMs < 1000) {
            return String.format("%.0f ms", averageExecutionTimeMs);
        } else if (averageExecutionTimeMs < 60000) {
            return String.format("%.1f seconds", averageExecutionTimeMs / 1000.0);
        } else {
            return String.format("%.1f minutes", averageExecutionTimeMs / 60000.0);
        }
    }
    
    /**
     * Creates a summary string of the job statistics.
     * 
     * @return formatted summary
     */
    public String getSummary() {
        return String.format(
            "Job: %s | Total: %d | Completed: %d (%.1f%%) | Failed: %d (%.1f%%) | Running: %d | Avg Time: %s",
            jobName,
            totalExecutions,
            completedExecutions,
            getSuccessRate(),
            failedExecutions,
            getFailureRate(),
            runningExecutions,
            getFormattedAverageExecutionTime()
        );
    }
}