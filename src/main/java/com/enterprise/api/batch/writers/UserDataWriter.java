package com.enterprise.api.batch.writers;

import com.enterprise.api.batch.dto.UserDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ItemWriter for writing processed user data to various outputs.
 * Supports writing to files, databases, or external systems.
 * 
 * Requirements: 6.2, 6.3
 */
@Component
public class UserDataWriter implements ItemWriter<UserDataDto> {

    private static final Logger logger = LoggerFactory.getLogger(UserDataWriter.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String outputDirectory;
    private String jobExecutionUser;
    private Long jobExecutionId;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.jobExecutionUser = stepExecution.getJobParameters().getString("user", "system");
        this.jobExecutionId = stepExecution.getJobExecution().getId();
        this.outputDirectory = stepExecution.getJobParameters().getString("outputDirectory", "batch-output");
        
        // Create output directory if it doesn't exist
        createOutputDirectory();
        
        logger.info("Starting user data writing step. Job execution ID: {}, User: {}, Output directory: {}", 
                   jobExecutionId, jobExecutionUser, outputDirectory);
    }

    @Override
    public void write(Chunk<? extends UserDataDto> chunk) throws Exception {
        List<? extends UserDataDto> items = chunk.getItems();
        logger.debug("Writing {} processed user records", items.size());

        // Write to CSV file
        writeToCSVFile(items);

        // Write to JSON file
        writeToJSONFile(items);

        // Write summary statistics
        writeSummaryStatistics(items);

        logger.debug("Successfully wrote {} user records to output files", items.size());
    }

    /**
     * Writes user data to a CSV file.
     * 
     * @param items the list of UserDataDto to write
     * @throws IOException if file writing fails
     */
    private void writeToCSVFile(List<? extends UserDataDto> items) throws IOException {
        String fileName = String.format("user_data_%d_%s.csv", 
                                       jobExecutionId, 
                                       LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        Path filePath = Paths.get(outputDirectory, fileName);

        boolean fileExists = Files.exists(filePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile(), true))) {
            // Write header if file doesn't exist
            if (!fileExists) {
                writer.write("ID,Username,Email,FirstName,LastName,Enabled,Status,LoginCount,LastLoginDate,ProcessedBy,ProcessedAt");
                writer.newLine();
            }

            // Write data rows
            for (UserDataDto dto : items) {
                writer.write(formatCSVRow(dto));
                writer.newLine();
            }
        }

        logger.debug("Wrote {} records to CSV file: {}", items.size(), filePath);
    }

    /**
     * Writes user data to a JSON file.
     * 
     * @param items the list of UserDataDto to write
     * @throws IOException if file writing fails
     */
    private void writeToJSONFile(List<? extends UserDataDto> items) throws IOException {
        String fileName = String.format("user_data_%d_%s.json", 
                                       jobExecutionId, 
                                       LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        Path filePath = Paths.get(outputDirectory, fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile(), true))) {
            // Write JSON array opening if file doesn't exist
            if (!Files.exists(filePath) || Files.size(filePath) == 0) {
                writer.write("[");
                writer.newLine();
            } else {
                writer.write(",");
                writer.newLine();
            }

            // Write JSON objects
            for (int i = 0; i < items.size(); i++) {
                writer.write(formatJSONObject(items.get(i)));
                if (i < items.size() - 1) {
                    writer.write(",");
                }
                writer.newLine();
            }
        }

        logger.debug("Wrote {} records to JSON file: {}", items.size(), filePath);
    }

    /**
     * Writes summary statistics for the processed batch.
     * 
     * @param items the list of UserDataDto to analyze
     * @throws IOException if file writing fails
     */
    private void writeSummaryStatistics(List<? extends UserDataDto> items) throws IOException {
        String fileName = String.format("batch_summary_%d.txt", jobExecutionId);
        Path filePath = Paths.get(outputDirectory, fileName);

        // Calculate statistics
        long activeUsers = items.stream().mapToLong(dto -> "ACTIVE".equals(dto.getStatus()) ? 1 : 0).sum();
        long inactiveUsers = items.stream().mapToLong(dto -> "INACTIVE".equals(dto.getStatus()) ? 1 : 0).sum();
        long dormantUsers = items.stream().mapToLong(dto -> "DORMANT".equals(dto.getStatus()) ? 1 : 0).sum();
        long disabledUsers = items.stream().mapToLong(dto -> "DISABLED".equals(dto.getStatus()) ? 1 : 0).sum();

        double averageLoginCount = items.stream()
                .mapToInt(UserDataDto::getLoginCount)
                .average()
                .orElse(0.0);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile(), true))) {
            writer.write(String.format("Batch Processing Summary - Job Execution ID: %d%n", jobExecutionId));
            writer.write(String.format("Processed by: %s%n", jobExecutionUser));
            writer.write(String.format("Processed at: %s%n", LocalDateTime.now().format(TIMESTAMP_FORMATTER)));
            writer.write(String.format("Total records processed: %d%n", items.size()));
            writer.write(String.format("Active users: %d%n", activeUsers));
            writer.write(String.format("Inactive users: %d%n", inactiveUsers));
            writer.write(String.format("Dormant users: %d%n", dormantUsers));
            writer.write(String.format("Disabled users: %d%n", disabledUsers));
            writer.write(String.format("Average login count: %.2f%n", averageLoginCount));
            writer.write("-----------------------------------");
            writer.newLine();
        }

        logger.debug("Wrote summary statistics to file: {}", filePath);
    }

    /**
     * Formats a UserDataDto as a CSV row.
     * 
     * @param dto the UserDataDto to format
     * @return formatted CSV row
     */
    private String formatCSVRow(UserDataDto dto) {
        return String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",%b,\"%s\",%d,\"%s\",\"%s\",\"%s\"",
                dto.getId(),
                escapeCSV(dto.getUsername()),
                escapeCSV(dto.getEmail()),
                escapeCSV(dto.getFirstName()),
                escapeCSV(dto.getLastName()),
                dto.isEnabled(),
                escapeCSV(dto.getStatus()),
                dto.getLoginCount(),
                dto.getLastLoginDate() != null ? dto.getLastLoginDate().format(TIMESTAMP_FORMATTER) : "",
                escapeCSV(dto.getProcessedBy()),
                dto.getProcessedAt() != null ? dto.getProcessedAt().format(TIMESTAMP_FORMATTER) : ""
        );
    }

    /**
     * Formats a UserDataDto as a JSON object.
     * 
     * @param dto the UserDataDto to format
     * @return formatted JSON object
     */
    private String formatJSONObject(UserDataDto dto) {
        return String.format(
                "  {" +
                "\"id\": %d, " +
                "\"username\": \"%s\", " +
                "\"email\": \"%s\", " +
                "\"firstName\": \"%s\", " +
                "\"lastName\": \"%s\", " +
                "\"enabled\": %b, " +
                "\"status\": \"%s\", " +
                "\"loginCount\": %d, " +
                "\"lastLoginDate\": \"%s\", " +
                "\"processedBy\": \"%s\", " +
                "\"processedAt\": \"%s\"" +
                "}",
                dto.getId(),
                escapeJSON(dto.getUsername()),
                escapeJSON(dto.getEmail()),
                escapeJSON(dto.getFirstName()),
                escapeJSON(dto.getLastName()),
                dto.isEnabled(),
                escapeJSON(dto.getStatus()),
                dto.getLoginCount(),
                dto.getLastLoginDate() != null ? dto.getLastLoginDate().format(TIMESTAMP_FORMATTER) : "",
                escapeJSON(dto.getProcessedBy()),
                dto.getProcessedAt() != null ? dto.getProcessedAt().format(TIMESTAMP_FORMATTER) : ""
        );
    }

    /**
     * Escapes special characters for CSV format.
     * 
     * @param value the string to escape
     * @return escaped string
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }

    /**
     * Escapes special characters for JSON format.
     * 
     * @param value the string to escape
     * @return escaped string
     */
    private String escapeJSON(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    /**
     * Creates the output directory if it doesn't exist.
     */
    private void createOutputDirectory() {
        try {
            Path path = Paths.get(outputDirectory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.info("Created output directory: {}", outputDirectory);
            }
        } catch (IOException e) {
            logger.error("Failed to create output directory: {}", outputDirectory, e);
            throw new RuntimeException("Failed to create output directory", e);
        }
    }
}