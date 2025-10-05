package com.enterprise.api.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for collecting and managing business metrics.
 * 
 * <p>This service provides methods to track various business operations
 * and their performance metrics using Micrometer.</p>
 */
@Service
public class BusinessMetricsService {

    private final MeterRegistry meterRegistry;
    
    // Counters for business operations
    private final Counter userRegistrationCounter;
    private final Counter userLoginCounter;
    private final Counter userLoginFailureCounter;
    private final Counter fileUploadCounter;
    private final Counter fileDownloadCounter;
    private final Counter otpGenerationCounter;
    private final Counter otpValidationCounter;
    private final Counter captchaGenerationCounter;
    private final Counter captchaValidationCounter;
    private final Counter batchJobExecutionCounter;
    private final Counter auditEventCounter;
    
    // Timers for operation duration
    private final Timer userRegistrationTimer;
    private final Timer userLoginTimer;
    private final Timer fileUploadTimer;
    private final Timer fileDownloadTimer;
    private final Timer otpGenerationTimer;
    private final Timer otpValidationTimer;
    private final Timer captchaGenerationTimer;
    private final Timer captchaValidationTimer;
    private final Timer batchJobExecutionTimer;
    
    // Gauges for current state
    private final AtomicLong activeUserSessions = new AtomicLong(0);
    private final AtomicLong totalUsers = new AtomicLong(0);
    private final AtomicLong totalFiles = new AtomicLong(0);
    private final AtomicLong activeBatchJobs = new AtomicLong(0);

    public BusinessMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.userRegistrationCounter = Counter.builder("business.user.registration")
                .description("Number of user registrations")
                .register(meterRegistry);
                
        this.userLoginCounter = Counter.builder("business.user.login.success")
                .description("Number of successful user logins")
                .register(meterRegistry);
                
        this.userLoginFailureCounter = Counter.builder("business.user.login.failure")
                .description("Number of failed user login attempts")
                .register(meterRegistry);
                
        this.fileUploadCounter = Counter.builder("business.file.upload")
                .description("Number of file uploads")
                .register(meterRegistry);
                
        this.fileDownloadCounter = Counter.builder("business.file.download")
                .description("Number of file downloads")
                .register(meterRegistry);
                
        this.otpGenerationCounter = Counter.builder("business.otp.generation")
                .description("Number of OTP generations")
                .register(meterRegistry);
                
        this.otpValidationCounter = Counter.builder("business.otp.validation")
                .description("Number of OTP validations")
                .register(meterRegistry);
                
        this.captchaGenerationCounter = Counter.builder("business.captcha.generation")
                .description("Number of CAPTCHA generations")
                .register(meterRegistry);
                
        this.captchaValidationCounter = Counter.builder("business.captcha.validation")
                .description("Number of CAPTCHA validations")
                .register(meterRegistry);
                
        this.batchJobExecutionCounter = Counter.builder("business.batch.job.execution")
                .description("Number of batch job executions")
                .register(meterRegistry);
                
        this.auditEventCounter = Counter.builder("business.audit.event")
                .description("Number of audit events")
                .register(meterRegistry);
        
        // Initialize timers
        this.userRegistrationTimer = Timer.builder("business.user.registration.duration")
                .description("Duration of user registration operations")
                .register(meterRegistry);
                
        this.userLoginTimer = Timer.builder("business.user.login.duration")
                .description("Duration of user login operations")
                .register(meterRegistry);
                
        this.fileUploadTimer = Timer.builder("business.file.upload.duration")
                .description("Duration of file upload operations")
                .register(meterRegistry);
                
        this.fileDownloadTimer = Timer.builder("business.file.download.duration")
                .description("Duration of file download operations")
                .register(meterRegistry);
                
        this.otpGenerationTimer = Timer.builder("business.otp.generation.duration")
                .description("Duration of OTP generation operations")
                .register(meterRegistry);
                
        this.otpValidationTimer = Timer.builder("business.otp.validation.duration")
                .description("Duration of OTP validation operations")
                .register(meterRegistry);
                
        this.captchaGenerationTimer = Timer.builder("business.captcha.generation.duration")
                .description("Duration of CAPTCHA generation operations")
                .register(meterRegistry);
                
        this.captchaValidationTimer = Timer.builder("business.captcha.validation.duration")
                .description("Duration of CAPTCHA validation operations")
                .register(meterRegistry);
                
        this.batchJobExecutionTimer = Timer.builder("business.batch.job.execution.duration")
                .description("Duration of batch job executions")
                .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder("business.user.sessions.active", activeUserSessions, AtomicLong::doubleValue)
                .description("Number of active user sessions")
                .register(meterRegistry);
                
        Gauge.builder("business.user.total", totalUsers, AtomicLong::doubleValue)
                .description("Total number of users")
                .register(meterRegistry);
                
        Gauge.builder("business.file.total", totalFiles, AtomicLong::doubleValue)
                .description("Total number of files")
                .register(meterRegistry);
                
        Gauge.builder("business.batch.jobs.active", activeBatchJobs, AtomicLong::doubleValue)
                .description("Number of active batch jobs")
                .register(meterRegistry);
    }

    // Counter increment methods
    public void incrementUserRegistration() {
        userRegistrationCounter.increment();
    }

    public void incrementUserLogin() {
        userLoginCounter.increment();
    }

    public void incrementUserLoginFailure() {
        userLoginFailureCounter.increment();
    }

    public void incrementFileUpload() {
        fileUploadCounter.increment();
    }

    public void incrementFileDownload() {
        fileDownloadCounter.increment();
    }

    public void incrementOtpGeneration() {
        otpGenerationCounter.increment();
    }

    public void incrementOtpValidation() {
        otpValidationCounter.increment();
    }

    public void incrementCaptchaGeneration() {
        captchaGenerationCounter.increment();
    }

    public void incrementCaptchaValidation() {
        captchaValidationCounter.increment();
    }

    public void incrementBatchJobExecution() {
        batchJobExecutionCounter.increment();
    }

    public void incrementAuditEvent() {
        auditEventCounter.increment();
    }

    // Timer methods
    public Timer.Sample startUserRegistrationTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopUserRegistrationTimer(Timer.Sample sample) {
        sample.stop(userRegistrationTimer);
    }

    public Timer.Sample startUserLoginTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopUserLoginTimer(Timer.Sample sample) {
        sample.stop(userLoginTimer);
    }

    public Timer.Sample startFileUploadTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopFileUploadTimer(Timer.Sample sample) {
        sample.stop(fileUploadTimer);
    }

    public Timer.Sample startFileDownloadTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopFileDownloadTimer(Timer.Sample sample) {
        sample.stop(fileDownloadTimer);
    }

    public Timer.Sample startOtpGenerationTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopOtpGenerationTimer(Timer.Sample sample) {
        sample.stop(otpGenerationTimer);
    }

    public Timer.Sample startOtpValidationTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopOtpValidationTimer(Timer.Sample sample) {
        sample.stop(otpValidationTimer);
    }

    public Timer.Sample startCaptchaGenerationTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopCaptchaGenerationTimer(Timer.Sample sample) {
        sample.stop(captchaGenerationTimer);
    }

    public Timer.Sample startCaptchaValidationTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopCaptchaValidationTimer(Timer.Sample sample) {
        sample.stop(captchaValidationTimer);
    }

    public Timer.Sample startBatchJobExecutionTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopBatchJobExecutionTimer(Timer.Sample sample) {
        sample.stop(batchJobExecutionTimer);
    }

    // Gauge update methods
    public void setActiveUserSessions(long count) {
        activeUserSessions.set(count);
    }

    public void incrementActiveUserSessions() {
        activeUserSessions.incrementAndGet();
    }

    public void decrementActiveUserSessions() {
        activeUserSessions.decrementAndGet();
    }

    public void setTotalUsers(long count) {
        totalUsers.set(count);
    }

    public void setTotalFiles(long count) {
        totalFiles.set(count);
    }

    public void setActiveBatchJobs(long count) {
        activeBatchJobs.set(count);
    }

    public void incrementActiveBatchJobs() {
        activeBatchJobs.incrementAndGet();
    }

    public void decrementActiveBatchJobs() {
        activeBatchJobs.decrementAndGet();
    }
}