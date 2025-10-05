package com.enterprise.api.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BusinessMetricsService.
 */
class BusinessMetricsServiceTest {

    private MeterRegistry meterRegistry;
    private BusinessMetricsService businessMetricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        businessMetricsService = new BusinessMetricsService(meterRegistry);
    }

    @Test
    void incrementUserRegistration_ShouldIncrementCounter() {
        // When
        businessMetricsService.incrementUserRegistration();
        businessMetricsService.incrementUserRegistration();

        // Then
        Counter counter = meterRegistry.find("business.user.registration").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    void incrementUserLogin_ShouldIncrementCounter() {
        // When
        businessMetricsService.incrementUserLogin();

        // Then
        Counter counter = meterRegistry.find("business.user.login.success").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void incrementUserLoginFailure_ShouldIncrementCounter() {
        // When
        businessMetricsService.incrementUserLoginFailure();

        // Then
        Counter counter = meterRegistry.find("business.user.login.failure").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void incrementFileUpload_ShouldIncrementCounter() {
        // When
        businessMetricsService.incrementFileUpload();

        // Then
        Counter counter = meterRegistry.find("business.file.upload").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void incrementOtpGeneration_ShouldIncrementCounter() {
        // When
        businessMetricsService.incrementOtpGeneration();

        // Then
        Counter counter = meterRegistry.find("business.otp.generation").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void userRegistrationTimer_ShouldRecordDuration() throws InterruptedException {
        // When
        Timer.Sample sample = businessMetricsService.startUserRegistrationTimer();
        Thread.sleep(10); // Small delay to ensure measurable duration
        businessMetricsService.stopUserRegistrationTimer(sample);

        // Then
        Timer timer = meterRegistry.find("business.user.registration.duration").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    void fileUploadTimer_ShouldRecordDuration() throws InterruptedException {
        // When
        Timer.Sample sample = businessMetricsService.startFileUploadTimer();
        Thread.sleep(10); // Small delay to ensure measurable duration
        businessMetricsService.stopFileUploadTimer(sample);

        // Then
        Timer timer = meterRegistry.find("business.file.upload.duration").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    void setActiveUserSessions_ShouldUpdateGauge() {
        // When
        businessMetricsService.setActiveUserSessions(5);

        // Then
        var gauge = meterRegistry.find("business.user.sessions.active").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(5.0);
    }

    @Test
    void incrementActiveUserSessions_ShouldIncrementGauge() {
        // Given
        businessMetricsService.setActiveUserSessions(3);

        // When
        businessMetricsService.incrementActiveUserSessions();
        businessMetricsService.incrementActiveUserSessions();

        // Then
        var gauge = meterRegistry.find("business.user.sessions.active").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(5.0);
    }

    @Test
    void decrementActiveUserSessions_ShouldDecrementGauge() {
        // Given
        businessMetricsService.setActiveUserSessions(5);

        // When
        businessMetricsService.decrementActiveUserSessions();

        // Then
        var gauge = meterRegistry.find("business.user.sessions.active").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(4.0);
    }

    @Test
    void setTotalUsers_ShouldUpdateGauge() {
        // When
        businessMetricsService.setTotalUsers(100);

        // Then
        var gauge = meterRegistry.find("business.user.total").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(100.0);
    }

    @Test
    void setTotalFiles_ShouldUpdateGauge() {
        // When
        businessMetricsService.setTotalFiles(50);

        // Then
        var gauge = meterRegistry.find("business.file.total").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(50.0);
    }

    @Test
    void batchJobMetrics_ShouldWorkCorrectly() {
        // When
        businessMetricsService.incrementBatchJobExecution();
        businessMetricsService.setActiveBatchJobs(2);
        businessMetricsService.incrementActiveBatchJobs();

        // Then
        Counter counter = meterRegistry.find("business.batch.job.execution").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);

        var gauge = meterRegistry.find("business.batch.jobs.active").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(3.0);
    }

    @Test
    void allCountersExist_ShouldBeRegistered() {
        // Trigger all counters to ensure they're registered
        businessMetricsService.incrementUserRegistration();
        businessMetricsService.incrementUserLogin();
        businessMetricsService.incrementUserLoginFailure();
        businessMetricsService.incrementFileUpload();
        businessMetricsService.incrementFileDownload();
        businessMetricsService.incrementOtpGeneration();
        businessMetricsService.incrementOtpValidation();
        businessMetricsService.incrementCaptchaGeneration();
        businessMetricsService.incrementCaptchaValidation();
        businessMetricsService.incrementBatchJobExecution();
        businessMetricsService.incrementAuditEvent();

        // Then verify all counters exist
        assertThat(meterRegistry.find("business.user.registration").counter()).isNotNull();
        assertThat(meterRegistry.find("business.user.login.success").counter()).isNotNull();
        assertThat(meterRegistry.find("business.user.login.failure").counter()).isNotNull();
        assertThat(meterRegistry.find("business.file.upload").counter()).isNotNull();
        assertThat(meterRegistry.find("business.file.download").counter()).isNotNull();
        assertThat(meterRegistry.find("business.otp.generation").counter()).isNotNull();
        assertThat(meterRegistry.find("business.otp.validation").counter()).isNotNull();
        assertThat(meterRegistry.find("business.captcha.generation").counter()).isNotNull();
        assertThat(meterRegistry.find("business.captcha.validation").counter()).isNotNull();
        assertThat(meterRegistry.find("business.batch.job.execution").counter()).isNotNull();
        assertThat(meterRegistry.find("business.audit.event").counter()).isNotNull();
    }
}