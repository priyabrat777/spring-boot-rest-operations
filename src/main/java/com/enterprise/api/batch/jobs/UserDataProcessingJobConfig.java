package com.enterprise.api.batch.jobs;

import com.enterprise.api.batch.config.BaseJobConfig;
import com.enterprise.api.batch.dto.UserDataDto;
import com.enterprise.api.batch.processors.UserDataProcessor;
import com.enterprise.api.batch.readers.UserDataReader;
import com.enterprise.api.batch.writers.UserDataWriter;
import com.enterprise.api.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration for user data processing batch job.
 * Demonstrates chunk-based processing with restart and skip capabilities.
 * 
 * Requirements: 6.2, 6.3, 6.4
 */
@Configuration
public class UserDataProcessingJobConfig extends BaseJobConfig {

    private static final Logger logger = LoggerFactory.getLogger(UserDataProcessingJobConfig.class);
    
    private static final String JOB_NAME = "userDataProcessingJob";
    private static final String STEP_NAME = "userDataProcessingStep";
    private static final int CHUNK_SIZE = 10;
    private static final int SKIP_LIMIT = 5;

    private final UserDataReader userDataReader;
    private final UserDataProcessor userDataProcessor;
    private final UserDataWriter userDataWriter;

    public UserDataProcessingJobConfig(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     UserDataReader userDataReader,
                                     UserDataProcessor userDataProcessor,
                                     UserDataWriter userDataWriter) {
        super(jobRepository, transactionManager);
        this.userDataReader = userDataReader;
        this.userDataProcessor = userDataProcessor;
        this.userDataWriter = userDataWriter;
    }

    /**
     * Creates the main user data processing job.
     * Configured with restart capability and fault tolerance.
     * 
     * @return configured Job
     */
    @Bean(name = JOB_NAME)
    public Job userDataProcessingJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(createJobParametersValidator())
                .start(userDataProcessingStep())
                .build();
    }

    /**
     * Creates the user data processing step with chunk-based processing.
     * Includes skip logic for handling failed items.
     * 
     * @return configured Step
     */
    @Bean(name = STEP_NAME)
    public Step userDataProcessingStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<User, UserDataDto>chunk(CHUNK_SIZE, transactionManager)
                .reader(createUserItemReader())
                .processor(createUserItemProcessor())
                .writer(createUserItemWriter())
                .faultTolerant()
                .skipLimit(SKIP_LIMIT)
                .skip(RuntimeException.class)
                .noSkip(IllegalArgumentException.class) // Don't skip validation errors
                .listener(new UserDataProcessingStepListener())
                .build();
    }

    /**
     * Creates the ItemReader for reading User entities.
     * 
     * @return configured ItemReader
     */
    @Bean
    public ItemReader<User> createUserItemReader() {
        // Default configuration reads all enabled users with page size of CHUNK_SIZE
        return userDataReader.createUserReader(CHUNK_SIZE, true);
    }

    /**
     * Creates the ItemProcessor for processing User entities.
     * 
     * @return configured ItemProcessor
     */
    @Bean
    public ItemProcessor<User, UserDataDto> createUserItemProcessor() {
        return userDataProcessor;
    }

    /**
     * Creates the ItemWriter for writing processed UserDataDto objects.
     * 
     * @return configured ItemWriter
     */
    @Bean
    public ItemWriter<UserDataDto> createUserItemWriter() {
        return userDataWriter;
    }

    /**
     * Alternative job configuration for processing users by role.
     * 
     * @return configured Job for role-based processing
     */
    @Bean(name = "userDataProcessingByRoleJob")
    public Job userDataProcessingByRoleJob() {
        return new JobBuilder("userDataProcessingByRoleJob", jobRepository)
                .validator(createJobParametersValidator())
                .start(userDataProcessingByRoleStep())
                .build();
    }

    /**
     * Creates a step for processing users by specific role.
     * 
     * @return configured Step
     */
    @Bean(name = "userDataProcessingByRoleStep")
    public Step userDataProcessingByRoleStep() {
        return new StepBuilder("userDataProcessingByRoleStep", jobRepository)
                .<User, UserDataDto>chunk(CHUNK_SIZE, transactionManager)
                .reader(createUserByRoleItemReader())
                .processor(createUserItemProcessor())
                .writer(createUserItemWriter())
                .faultTolerant()
                .skipLimit(SKIP_LIMIT)
                .skip(RuntimeException.class)
                .noSkip(IllegalArgumentException.class)
                .listener(new UserDataProcessingStepListener())
                .build();
    }

    /**
     * Creates an ItemReader for reading users by role.
     * This reader will be configured at runtime based on job parameters.
     * 
     * @return configured ItemReader
     */
    @Bean
    public ItemReader<User> createUserByRoleItemReader() {
        // This will be configured with role parameter at runtime
        return userDataReader.createUserReaderByRole(CHUNK_SIZE, "USER");
    }

    /**
     * Alternative job configuration for processing recent users.
     * 
     * @return configured Job for recent user processing
     */
    @Bean(name = "recentUserDataProcessingJob")
    public Job recentUserDataProcessingJob() {
        return new JobBuilder("recentUserDataProcessingJob", jobRepository)
                .validator(createJobParametersValidator())
                .start(recentUserDataProcessingStep())
                .build();
    }

    /**
     * Creates a step for processing recently created users.
     * 
     * @return configured Step
     */
    @Bean(name = "recentUserDataProcessingStep")
    public Step recentUserDataProcessingStep() {
        return new StepBuilder("recentUserDataProcessingStep", jobRepository)
                .<User, UserDataDto>chunk(CHUNK_SIZE, transactionManager)
                .reader(createRecentUserItemReader())
                .processor(createUserItemProcessor())
                .writer(createUserItemWriter())
                .faultTolerant()
                .skipLimit(SKIP_LIMIT)
                .skip(RuntimeException.class)
                .noSkip(IllegalArgumentException.class)
                .listener(new UserDataProcessingStepListener())
                .build();
    }

    /**
     * Creates an ItemReader for reading recently created users.
     * 
     * @return configured ItemReader
     */
    @Bean
    public ItemReader<User> createRecentUserItemReader() {
        // Default to users created in the last 30 days
        return userDataReader.createUserReaderWithDateFilter(CHUNK_SIZE, 30);
    }

    /**
     * Step listener for logging and monitoring step execution.
     */
    public static class UserDataProcessingStepListener implements 
            org.springframework.batch.core.StepExecutionListener {

        private static final Logger stepLogger = LoggerFactory.getLogger(UserDataProcessingStepListener.class);

        @Override
        public void beforeStep(org.springframework.batch.core.StepExecution stepExecution) {
            stepLogger.info("Starting step: {} for job: {}", 
                           stepExecution.getStepName(), 
                           stepExecution.getJobExecution().getJobInstance().getJobName());
        }

        @Override
        public org.springframework.batch.core.ExitStatus afterStep(org.springframework.batch.core.StepExecution stepExecution) {
            stepLogger.info("Completed step: {} with status: {}. Read: {}, Written: {}, Skipped: {}", 
                           stepExecution.getStepName(),
                           stepExecution.getStatus(),
                           stepExecution.getReadCount(),
                           stepExecution.getWriteCount(),
                           stepExecution.getSkipCount());
            
            return stepExecution.getExitStatus();
        }
    }
}