package com.enterprise.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for audit statistics.
 * Contains aggregated audit information for reporting purposes.
 */
@Schema(description = "Audit statistics response containing aggregated audit data")
public class AuditStatsResponse {

    @Schema(description = "Start date of the statistics period")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    @Schema(description = "End date of the statistics period")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    @Schema(description = "Total number of operations in the period", example = "1250")
    private Long totalOperations;

    @Schema(description = "Count of operations by type")
    private Map<String, Long> operationCounts;

    @Schema(description = "Count of operations by entity type")
    private Map<String, Long> entityCounts;

    @Schema(description = "Count of operations by user")
    private Map<String, Long> userCounts;

    @Schema(description = "Daily operation counts")
    private Map<String, Long> dailyCounts;

    @Schema(description = "Most active users in the period")
    private Map<String, Long> topUsers;

    @Schema(description = "Most accessed entities in the period")
    private Map<String, Long> topEntities;

    @Schema(description = "Failed operations count", example = "15")
    private Long failedOperations;

    @Schema(description = "Unique users count", example = "45")
    private Long uniqueUsers;

    @Schema(description = "Unique IP addresses count", example = "32")
    private Long uniqueIpAddresses;

    // Constructors
    public AuditStatsResponse() {}

    public AuditStatsResponse(LocalDateTime startDate, LocalDateTime endDate, Long totalOperations) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalOperations = totalOperations;
    }

    // Getters and Setters
    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Long getTotalOperations() {
        return totalOperations;
    }

    public void setTotalOperations(Long totalOperations) {
        this.totalOperations = totalOperations;
    }

    public Map<String, Long> getOperationCounts() {
        return operationCounts;
    }

    public void setOperationCounts(Map<String, Long> operationCounts) {
        this.operationCounts = operationCounts;
    }

    public Map<String, Long> getEntityCounts() {
        return entityCounts;
    }

    public void setEntityCounts(Map<String, Long> entityCounts) {
        this.entityCounts = entityCounts;
    }

    public Map<String, Long> getUserCounts() {
        return userCounts;
    }

    public void setUserCounts(Map<String, Long> userCounts) {
        this.userCounts = userCounts;
    }

    public Map<String, Long> getDailyCounts() {
        return dailyCounts;
    }

    public void setDailyCounts(Map<String, Long> dailyCounts) {
        this.dailyCounts = dailyCounts;
    }

    public Map<String, Long> getTopUsers() {
        return topUsers;
    }

    public void setTopUsers(Map<String, Long> topUsers) {
        this.topUsers = topUsers;
    }

    public Map<String, Long> getTopEntities() {
        return topEntities;
    }

    public void setTopEntities(Map<String, Long> topEntities) {
        this.topEntities = topEntities;
    }

    public Long getFailedOperations() {
        return failedOperations;
    }

    public void setFailedOperations(Long failedOperations) {
        this.failedOperations = failedOperations;
    }

    public Long getUniqueUsers() {
        return uniqueUsers;
    }

    public void setUniqueUsers(Long uniqueUsers) {
        this.uniqueUsers = uniqueUsers;
    }

    public Long getUniqueIpAddresses() {
        return uniqueIpAddresses;
    }

    public void setUniqueIpAddresses(Long uniqueIpAddresses) {
        this.uniqueIpAddresses = uniqueIpAddresses;
    }

    @Override
    public String toString() {
        return "AuditStatsResponse{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", totalOperations=" + totalOperations +
                ", operationCounts=" + operationCounts +
                ", entityCounts=" + entityCounts +
                ", userCounts=" + userCounts +
                ", failedOperations=" + failedOperations +
                ", uniqueUsers=" + uniqueUsers +
                ", uniqueIpAddresses=" + uniqueIpAddresses +
                '}';
    }
}