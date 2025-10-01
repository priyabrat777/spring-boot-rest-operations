package com.enterprise.api.batch.monitoring;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for monitoring and managing Spring Batch jobs.
 * Provides functionality for job status tracking, execution monitoring, and job management.
 * 
 * This service handles:
 * - Job execution status monitoring
 * - Job instance and execution queries
 * - Job restart and stop operations
 * - Batch job statistics and reporting
 * 
 * Requirements: 6.6
 */
@Service
public class BatchJobMonitoringService {

    private final JobExplorer jobExplorer;
    private final JobRepository jobRepository;
    private final JobLauncher jobLauncher;
    private final Map<String, Job> jobRegistry;

    public BatchJobMonitoringService(JobExplorer jobExplorer, 
                                   JobRepository jobRepository,
                                   JobLauncher jobLauncher,
                                   Map<String, Job> jobRegistry) {
        this.jobExplorer = jobExplorer;
        this.jobRepository = jobRepository;
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
    }

    /**
     * Gets the current status of a job execution.
     * 
     * @param executionId the job execution ID
     * @return JobExecutionStatus containing execution details
     */
    public Optional<JobExecutionStatus> getJobExecutionStatus(Long executionId) {
        JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
        if (jobExecution == null) {
            return Optional.empty();
        }
        
        return Optional.of(createJobExecutionStatus(jobExecution));
    }

    /**
     * Gets all job executions for a specific job name.
     * 
     * @param jobName the name of the job
     * @param limit maximum number of executions to return
     * @return list of job execution statuses
     */
    public List<JobExecutionStatus> getJobExecutions(String jobName, int limit) {
        List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, limit);
        
        return jobInstances.stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .map(this::createJobExecutionStatus)
                .collect(Collectors.toList());
    }

    /**
     * Gets all running job executions.
     * 
     * @return list of currently running job executions
     */
    public List<JobExecutionStatus> getRunningJobExecutions() {
        Set<String> jobNames = Set.copyOf(jobExplorer.getJobNames());
        
        return jobNames.stream()
                .flatMap(jobName -> {
                    List<JobInstance> instances = jobExplorer.getJobInstances(jobName, 0, 100);
                    return instances.stream()
                            .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream());
                })
                .filter(execution -> execution.getStatus() == BatchStatus.STARTED || 
                                   execution.getStatus() == BatchStatus.STARTING)
                .map(this::createJobExecutionStatus)
                .collect(Collectors.toList());
    }

    /**
     * Gets job execution statistics for a specific job.
     * 
     * @param jobName the name of the job
     * @return JobStatistics containing execution metrics
     */
    public JobStatistics getJobStatistics(String jobName) {
        List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, 1000);
        
        List<JobExecution> executions = jobInstances.stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .collect(Collectors.toList());

        long totalExecutions = executions.size();
        long completedExecutions = executions.stream()
                .mapToLong(exec -> exec.getStatus() == BatchStatus.COMPLETED ? 1 : 0)
                .sum();
        long failedExecutions = executions.stream()
                .mapToLong(exec -> exec.getStatus() == BatchStatus.FAILED ? 1 : 0)
                .sum();
        long runningExecutions = executions.stream()
                .mapToLong(exec -> exec.getStatus() == BatchStatus.STARTED || 
                                 exec.getStatus() == BatchStatus.STARTING ? 1 : 0)
                .sum();

        double averageExecutionTime = 0.0;
        if (!executions.isEmpty()) {
            long totalTime = 0;
            int count = 0;
            for (JobExecution exec : executions) {
                if (exec.getStartTime() != null && exec.getEndTime() != null) {
                    // Convert to milliseconds for calculation
                    long startMillis = exec.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    long endMillis = exec.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    totalTime += endMillis - startMillis;
                    count++;
                }
            }
            if (count > 0) {
                averageExecutionTime = (double) totalTime / count;
            }
        }

        return new JobStatistics(
                jobName,
                totalExecutions,
                completedExecutions,
                failedExecutions,
                runningExecutions,
                averageExecutionTime
        );
    }

    /**
     * Stops a running job execution.
     * 
     * @param executionId the job execution ID to stop
     * @return true if job was stopped successfully, false otherwise
     */
    public boolean stopJobExecution(Long executionId) {
        JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
        if (jobExecution == null || !jobExecution.isRunning()) {
            return false;
        }

        jobExecution.setStatus(BatchStatus.STOPPING);
        jobRepository.update(jobExecution);
        return true;
    }

    /**
     * Restarts a failed job execution.
     * 
     * @param executionId the job execution ID to restart
     * @return new JobExecution if restart was successful
     * @throws JobRestartException if job cannot be restarted
     * @throws JobInstanceAlreadyCompleteException if job instance is already complete
     * @throws NoSuchJobException if job is not found
     * @throws JobExecutionAlreadyRunningException if job is already running
     */
    public JobExecution restartJobExecution(Long executionId) 
            throws JobRestartException, JobInstanceAlreadyCompleteException, 
                   NoSuchJobException, JobExecutionAlreadyRunningException, JobParametersInvalidException {
        JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
        if (jobExecution == null) {
            throw new NoSuchJobException("Job execution not found: " + executionId);
        }

        String jobName = jobExecution.getJobInstance().getJobName();
        Job job = jobRegistry.get(jobName);
        if (job == null) {
            throw new NoSuchJobException("Job not found: " + jobName);
        }

        JobParameters jobParameters = jobExecution.getJobParameters();
        return jobLauncher.run(job, jobParameters);
    }

    /**
     * Gets all available job names.
     * 
     * @return set of job names
     */
    public Set<String> getAvailableJobNames() {
        return Set.copyOf(jobExplorer.getJobNames());
    }

    /**
     * Checks if a job is currently running.
     * 
     * @param jobName the name of the job
     * @return true if job is running, false otherwise
     */
    public boolean isJobRunning(String jobName) {
        List<JobInstance> instances = jobExplorer.getJobInstances(jobName, 0, 10);
        
        return instances.stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .anyMatch(execution -> execution.getStatus() == BatchStatus.STARTED || 
                                     execution.getStatus() == BatchStatus.STARTING);
    }

    /**
     * Creates a JobExecutionStatus from a JobExecution.
     * 
     * @param jobExecution the job execution
     * @return JobExecutionStatus with execution details
     */
    private JobExecutionStatus createJobExecutionStatus(JobExecution jobExecution) {
        LocalDateTime startTime = jobExecution.getStartTime();
        LocalDateTime endTime = jobExecution.getEndTime();

        return new JobExecutionStatus(
                jobExecution.getId(),
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus(),
                jobExecution.getExitStatus(),
                startTime,
                endTime,
                jobExecution.getJobParameters()
        );
    }
}