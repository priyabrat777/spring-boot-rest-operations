package com.enterprise.api.batch.writers;

import com.enterprise.api.batch.dto.UserDataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserDataWriter.
 * Tests file writing functionality and output formats.
 * 
 * Requirements: 6.2, 6.3
 */
@ExtendWith(MockitoExtension.class)
class UserDataWriterTest {

    @InjectMocks
    private UserDataWriter writer;

    @Mock
    private StepExecution stepExecution;

    @Mock
    private JobExecution jobExecution;

    @TempDir
    Path tempDir;

    private JobParameters jobParameters;

    @BeforeEach
    void setUp() {
        jobParameters = new JobParametersBuilder()
                .addString("user", "test-user")
                .addString("outputDirectory", tempDir.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        when(stepExecution.getJobParameters()).thenReturn(jobParameters);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getId()).thenReturn(123L);

        // Initialize the writer
        writer.beforeStep(stepExecution);
    }

    @Test
    void testWriteUserData() throws Exception {
        // Given
        List<UserDataDto> testData = createTestUserData();
        Chunk<UserDataDto> chunk = new Chunk<>(testData);

        // When
        writer.write(chunk);

        // Then
        verifyOutputFiles();
    }

    @Test
    void testWriteEmptyChunk() throws Exception {
        // Given
        Chunk<UserDataDto> emptyChunk = new Chunk<>();

        // When
        writer.write(emptyChunk);

        // Then
        // Should not create files for empty chunks, but directory should exist
        assertThat(Files.exists(tempDir)).isTrue();
    }

    @Test
    void testWriteMultipleChunks() throws Exception {
        // Given
        List<UserDataDto> firstChunk = createTestUserData();
        List<UserDataDto> secondChunk = createAdditionalTestUserData();

        // When
        writer.write(new Chunk<>(firstChunk));
        writer.write(new Chunk<>(secondChunk));

        // Then
        verifyOutputFiles();
        verifyMultipleChunkContent();
    }

    @Test
    void testCSVFormatting() throws Exception {
        // Given
        UserDataDto dto = createUserDataWithSpecialCharacters();
        Chunk<UserDataDto> chunk = new Chunk<>(List.of(dto));

        // When
        writer.write(chunk);

        // Then
        verifyCSVEscaping();
    }

    @Test
    void testJSONFormatting() throws Exception {
        // Given
        UserDataDto dto = createUserDataWithSpecialCharacters();
        Chunk<UserDataDto> chunk = new Chunk<>(List.of(dto));

        // When
        writer.write(chunk);

        // Then
        verifyJSONEscaping();
    }

    @Test
    void testSummaryStatistics() throws Exception {
        // Given
        List<UserDataDto> testData = createTestUserDataWithVariousStatuses();
        Chunk<UserDataDto> chunk = new Chunk<>(testData);

        // When
        writer.write(chunk);

        // Then
        verifySummaryStatistics();
    }

    @Test
    void testCustomOutputDirectory() throws Exception {
        // Given
        Path customDir = tempDir.resolve("custom-output");
        JobParameters customJobParameters = new JobParametersBuilder()
                .addString("user", "test-user")
                .addString("outputDirectory", customDir.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        when(stepExecution.getJobParameters()).thenReturn(customJobParameters);
        writer.beforeStep(stepExecution);

        List<UserDataDto> testData = createTestUserData();
        Chunk<UserDataDto> chunk = new Chunk<>(testData);

        // When
        writer.write(chunk);

        // Then
        assertThat(Files.exists(customDir)).isTrue();
        assertThat(Files.list(customDir).count()).isGreaterThan(0);
    }

    private List<UserDataDto> createTestUserData() {
        UserDataDto dto1 = new UserDataDto(1L, "user1", "user1@example.com", "John", "Doe", true);
        dto1.setStatus("ACTIVE");
        dto1.setLoginCount(10);
        dto1.setLastLoginDate(LocalDateTime.now().minusDays(1));
        dto1.setProcessedBy("test-user");
        dto1.setProcessedAt(LocalDateTime.now());

        UserDataDto dto2 = new UserDataDto(2L, "user2", "user2@example.com", "Jane", "Smith", false);
        dto2.setStatus("DISABLED");
        dto2.setLoginCount(0);
        dto2.setProcessedBy("test-user");
        dto2.setProcessedAt(LocalDateTime.now());

        return List.of(dto1, dto2);
    }

    private List<UserDataDto> createAdditionalTestUserData() {
        UserDataDto dto3 = new UserDataDto(3L, "user3", "user3@example.com", "Bob", "Johnson", true);
        dto3.setStatus("INACTIVE");
        dto3.setLoginCount(0);
        dto3.setProcessedBy("test-user");
        dto3.setProcessedAt(LocalDateTime.now());

        return List.of(dto3);
    }

    private UserDataDto createUserDataWithSpecialCharacters() {
        UserDataDto dto = new UserDataDto(4L, "user\"with\"quotes", "user@example.com", "First,Name", "Last\nName", true);
        dto.setStatus("ACTIVE");
        dto.setLoginCount(5);
        dto.setProcessedBy("test-user");
        dto.setProcessedAt(LocalDateTime.now());
        return dto;
    }

    private List<UserDataDto> createTestUserDataWithVariousStatuses() {
        UserDataDto active = new UserDataDto(1L, "active", "active@example.com", "Active", "User", true);
        active.setStatus("ACTIVE");
        active.setLoginCount(10);

        UserDataDto inactive = new UserDataDto(2L, "inactive", "inactive@example.com", "Inactive", "User", true);
        inactive.setStatus("INACTIVE");
        inactive.setLoginCount(0);

        UserDataDto dormant = new UserDataDto(3L, "dormant", "dormant@example.com", "Dormant", "User", true);
        dormant.setStatus("DORMANT");
        dormant.setLoginCount(5);

        UserDataDto disabled = new UserDataDto(4L, "disabled", "disabled@example.com", "Disabled", "User", false);
        disabled.setStatus("DISABLED");
        disabled.setLoginCount(0);

        return List.of(active, inactive, dormant, disabled);
    }

    private void verifyOutputFiles() throws IOException {
        // Check CSV file exists
        boolean csvExists = Files.list(tempDir)
                .anyMatch(path -> path.getFileName().toString().endsWith(".csv"));
        assertThat(csvExists).isTrue();

        // Check JSON file exists
        boolean jsonExists = Files.list(tempDir)
                .anyMatch(path -> path.getFileName().toString().endsWith(".json"));
        assertThat(jsonExists).isTrue();

        // Check summary file exists
        boolean summaryExists = Files.list(tempDir)
                .anyMatch(path -> path.getFileName().toString().startsWith("batch_summary_"));
        assertThat(summaryExists).isTrue();
    }

    private void verifyMultipleChunkContent() throws IOException {
        // Verify that files contain data from multiple chunks
        Path csvFile = Files.list(tempDir)
                .filter(path -> path.getFileName().toString().endsWith(".csv"))
                .findFirst()
                .orElseThrow();

        String csvContent = Files.readString(csvFile);
        assertThat(csvContent).contains("user1@example.com");
        assertThat(csvContent).contains("user2@example.com");
        assertThat(csvContent).contains("user3@example.com");
    }

    private void verifyCSVEscaping() throws IOException {
        Path csvFile = Files.list(tempDir)
                .filter(path -> path.getFileName().toString().endsWith(".csv"))
                .findFirst()
                .orElseThrow();

        String csvContent = Files.readString(csvFile);
        // Verify that quotes are properly escaped
        assertThat(csvContent).contains("\"user\"\"with\"\"quotes\"");
        assertThat(csvContent).contains("\"First,Name\"");
    }

    private void verifyJSONEscaping() throws IOException {
        Path jsonFile = Files.list(tempDir)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .findFirst()
                .orElseThrow();

        String jsonContent = Files.readString(jsonFile);
        // Verify that JSON special characters are properly escaped
        assertThat(jsonContent).contains("user\\\"with\\\"quotes");
    }

    private void verifySummaryStatistics() throws IOException {
        Path summaryFile = Files.list(tempDir)
                .filter(path -> path.getFileName().toString().startsWith("batch_summary_"))
                .findFirst()
                .orElseThrow();

        String summaryContent = Files.readString(summaryFile);
        assertThat(summaryContent).contains("Total records processed: 4");
        assertThat(summaryContent).contains("Active users: 1");
        assertThat(summaryContent).contains("Inactive users: 1");
        assertThat(summaryContent).contains("Dormant users: 1");
        assertThat(summaryContent).contains("Disabled users: 1");
        assertThat(summaryContent).contains("Average login count:");
    }
}