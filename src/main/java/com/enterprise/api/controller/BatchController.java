package com.enterprise.api.controller;

import com.enterprise.api.batch.config.JobParameterHandler;
import com.enterprise.api.batch.monitoring.BatchJobMonitoringService;
import com.enterprise.api.batch.monitoring.JobExecutionStatus;
import com.enterprise.api.batch.monitoring.JobStatistics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * REST Controller for managing batch job execution.
 * Provides endpoints for starting, stopping, and monitoring batch jobs.
 * 
 * Requirements: 6.4
 */
@Tag(name = "Batch Processing", description = "Spring Batch job execution and monitoring operations")
@RestController
@RequestMapping("/api/v1/batch")
@SecurityRequirement(name = "bearerAuth")
public class BatchController {

    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);

    private final JobLauncher jobLauncher;
    private final BatchJobMonitoringService monitoringService;
    private final JobParameterHandler parameterHandler;
    private final Map<String, Job> jobRegistry;

    public BatchController(JobLauncher jobLauncher,
                          BatchJobMonitoringService monitoringService,
                          JobParameterHandler parameterHandler,
                          Map<String, Job> jobRegistry) {
        this.jobLauncher = jobLauncher;
        this.monitoringService = monitoringService;
        this.parameterHandler = parameterHandler;
        this.jobRegistry = jobRegistry;
    }

    /**
     * Starts a batch job with the specified name and parameters.
     */
    @PostMapping("/jobs/{jobName}/start")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BATCH_OPERATOR')")
    @Operation(summary = "Start a batch job", description = "Starts the specified batch job with optional parameters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job started successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid job parameters"),
        @ApiResponse(responseCode = "404", description = "Job not found"),
        @ApiResponse(responseCode = "409", description = "Job already running")
    })
    public ResponseEntity<Map<String, Object>> startJob(
            @Parameter(description = "Name of the job to start") @PathVariable String jobName,
            @Parameter(description = "Job parameters") @RequestBody(required = false) Map<String, Object> parameters,
            Authentication authentication) {

        try {
            logger.info("Starting batch job: {} by user: {}", jobName, authentication.getName());

            Job job = jobRegistry.get(jobName);
            if (job == null) {
                return ResponseEntity.notFound().build();
            }

            // Check if job is already running
            if (monitoringService.isJobRunning(jobName)) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Job is already running");
                response.put("jobName", jobName);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            // Create job parameters
            JobParameters jobParameters = parameterHandler.createParameters(
                    authentication.getName(), 
                    parameters != null ? parameters : new HashMap<>()
            );

            // Launch the job
            JobExecution jobExecution = jobLauncher.run(job, jobParameters);

            Map<String, Object> response = new HashMap<>();
            response.put("jobExecutionId", jobExecution.getId());
            response.put("jobName", jobName);
            response.put("status", jobExecution.getStatus().toString());
            response.put("startTime", jobExecution.getStartTime());

            logger.info("Successfully started batch job: {} with execution ID: {}", jobName, jobExecution.getId());
            return ResponseEntity.ok(response);

        } catch (JobExecutionAlreadyRunningException e) {
            logger.warn("Job {} is already running", jobName);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Job is already running");
            response.put("jobName", jobName);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (JobInstanceAlreadyCompleteException e) {
            logger.warn("Job {} instance is already complete", jobName);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Job instance is already complete");
            response.put("jobName", jobName);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (JobParametersInvalidException e) {
            logger.error("Invalid job parameters for job: {}", jobName, e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Invalid job parameters: " + e.getMessage());
            response.put("jobName", jobName);
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            logger.error("Failed to start batch job: {}", jobName, e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to start job: " + e.getMessage());
            response.put("jobName", jobName);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Stops a running batch job execution.
     */
    @PostMapping("/executions/{executionId}/stop")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BATCH_OPERATOR')")
    @Operation(summary = "Stop a batch job execution", description = "Stops the specified running batch job execution")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job stopped successfully"),
        @ApiResponse(responseCode = "404", description = "Job execution not found"),
        @ApiResponse(responseCode = "400", description = "Job is not running")
    })
    public ResponseEntity<Map<String, Object>> stopJobExecution(
            @Parameter(description = "Job execution ID to stop") @PathVariable Long executionId,
            Authentication authentication) {

        logger.info("Stopping batch job execution: {} by user: {}", executionId, authentication.getName());

        boolean stopped = monitoringService.stopJobExecution(executionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("executionId", executionId);
        response.put("stopped", stopped);

        if (stopped) {
            logger.info("Successfully stopped batch job execution: {}", executionId);
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Job execution not found or not running");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Restarts a failed batch job execution.
     */
    @PostMapping("/executions/{executionId}/restart")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BATCH_OPERATOR')")
    @Operation(summary = "Restart a failed batch job execution", description = "Restarts the specified failed batch job execution")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job restarted successfully"),
        @ApiResponse(responseCode = "404", description = "Job execution not found"),
        @ApiResponse(responseCode = "400", description = "Job cannot be restarted")
    })
    public ResponseEntity<Map<String, Object>> restartJobExecution(
            @Parameter(description = "Job execution ID to restart") @PathVariable Long executionId,
            Authentication authentication) {

        try {
            logger.info("Restarting batch job execution: {} by user: {}", executionId, authentication.getName());

            JobExecution newJobExecution = monitoringService.restartJobExecution(executionId);

            Map<String, Object> response = new HashMap<>();
            response.put("originalExecutionId", executionId);
            response.put("newExecutionId", newJobExecution.getId());
            response.put("jobName", newJobExecution.getJobInstance().getJobName());
            response.put("status", newJobExecution.getStatus().toString());
            response.put("startTime", newJobExecution.getStartTime());

            logger.info("Successfully restarted batch job execution: {} as new execution: {}", 
                       executionId, newJobExecution.getId());
            return ResponseEntity.ok(response);

        } catch (NoSuchJobException e) {
            logger.error("Job not found for execution: {}", executionId);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Job not found");
            response.put("executionId", executionId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("Failed to restart batch job execution: {}", executionId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to restart job: " + e.getMessage());
            response.put("executionId", executionId);
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Gets the status of a specific job execution.
     */
    @GetMapping("/executions/{executionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BATCH_OPERATOR') or hasRole('BATCH_VIEWER')")
    @Operation(summary = "Get job execution status", description = "Retrieves the status of the specified job execution")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job execution status retrieved"),
        @ApiResponse(responseCode = "404", description = "Job execution not found")
    })
    public ResponseEntity<JobExecutionStatus> getJobExecutionStatus(
            @Parameter(description = "Job execution ID") @PathVariable Long executionId) {

        Optional<JobExecutionStatus> status = monitoringService.getJobExecutionStatus(executionId);
        return status.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Gets all executions for a specific job.
     */
    @GetMapping("/jobs/{jobName}/executions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BATCH_OPERATOR') or hasRole('BATCH_VIEWER')")
    @Operation(summary = "Get job executions", description = "Retrieves all executions for the specified job")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job executions retrieved")
    })
    public ResponseEntity<List<JobExecutionStatus>> getJobExecutions(
            @Parameter(description = "Job name") @PathVariable String jobName,
            @Parameter(description = "Maximum number of executions to return") @RequestParam(defaultValue = "10") int limit) {

        List<JobExecutionStatus> executions = monitoringService.getJobExecutions(jobName, limit);
        return ResponseEntity.ok(executions);
    }

    /**
     * Gets all currently running job executions.
     */
    @GetMapping("/executions/running")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BATCH_OPERATOR') or hasRole('BATCH_VIEWER')")
    @Operation(summary = "Get running job executions", description = "Retrieves all currently running job executions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Running job executions retrieved")
    })
    public ResponseEntity<List<JobExecutionStatus>> getRunningJobExecutions() {
        List<JobExecutionStatus> runningExecutions = monitoringService.getRunningJobExecutions();
        return ResponseEntity.ok(runningExecutions);
    }

    /**
     * Gets statistics for a specific job.
     */
    @GetMapping("/jobs/{jobName}/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BATCH_OPERATOR') or hasRole('BATCH_VIEWER')")
    @Operation(summary = "Get job statistics", description = "Retrieves execution statistics for the specified job")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job statistics retrieved")
    })
    public ResponseEntity<JobStatistics> getJobStatistics(
            @Parameter(description = "Job name") @PathVariable String jobName) {

        JobStatistics statistics = monitoringService.getJobStatistics(jobName);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Gets all available job names.
     */
    @GetMapping("/jobs")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BATCH_OPERATOR') or hasRole('BATCH_VIEWER')")
    @Operation(summary = "Get available jobs", description = "Retrieves all available batch job names")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Available jobs retrieved")
    })
    public ResponseEntity<Set<String>> getAvailableJobs() {
        Set<String> jobNames = monitoringService.getAvailableJobNames();
        return ResponseEntity.ok(jobNames);
    }
}