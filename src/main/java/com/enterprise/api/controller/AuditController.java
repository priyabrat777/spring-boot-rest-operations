package com.enterprise.api.controller;

import com.enterprise.api.audit.AuditService;
import com.enterprise.api.dto.response.AuditLogResponse;
import com.enterprise.api.dto.response.AuditStatsResponse;
import com.enterprise.api.entity.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for audit trail operations.
 * Provides endpoints for querying audit logs with various filters and generating reports.
 * 
 * Requirements addressed:
 * - 3.1, 3.2, 3.3, 3.4: Comprehensive audit trail querying
 * - 3.6: Audit data filtering by date range, user, and operation
 * - 1.1-1.8: Complete REST API operations with proper HTTP methods
 */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Audit trail operations")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private static final Logger logger = LoggerFactory.getLogger(AuditController.class);

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @Operation(
        summary = "Get all audit logs",
        description = "Retrieves all audit logs with pagination and sorting"
    )
    @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_READ')")
    public ResponseEntity<Page<AuditLogResponse>> getAllAuditLogs(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "performedAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") String sortDir) {

        logger.debug("Getting all audit logs - page: {}, size: {}, sortBy: {}, sortDir: {}", 
                    page, size, sortBy, sortDir);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<AuditLog> auditLogs = auditService.findAll(pageable);
        Page<AuditLogResponse> response = auditLogs.map(this::convertToResponse);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get audit logs by entity",
        description = "Retrieves audit logs for a specific entity"
    )
    @ApiResponse(responseCode = "200", description = "Entity audit logs retrieved successfully")
    @GetMapping("/entity/{entityName}/{entityId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_READ')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByEntity(
            @Parameter(description = "Entity name") @PathVariable String entityName,
            @Parameter(description = "Entity ID") @PathVariable String entityId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        logger.debug("Getting audit logs for entity: {} with ID: {}", entityName, entityId);

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditService.findByEntityNameAndEntityId(entityName, entityId, pageable);
        Page<AuditLogResponse> response = auditLogs.map(this::convertToResponse);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get audit logs by user",
        description = "Retrieves audit logs for operations performed by a specific user"
    )
    @ApiResponse(responseCode = "200", description = "User audit logs retrieved successfully")
    @GetMapping("/user/{username}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_READ') or #username == authentication.name")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByUser(
            @Parameter(description = "Username") @PathVariable String username,
            @Parameter(description = "Start date (ISO format)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        logger.debug("Getting audit logs for user: {} between {} and {}", username, startDate, endDate);

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs;
        
        if (startDate != null && endDate != null) {
            auditLogs = auditService.findByPerformedByAndDateRange(username, startDate, endDate, pageable);
        } else {
            // If no date range specified, get all logs for the user
            auditLogs = auditService.findByPerformedByAndDateRange(
                username, 
                LocalDateTime.now().minusYears(1), 
                LocalDateTime.now(), 
                pageable
            );
        }
        
        Page<AuditLogResponse> response = auditLogs.map(this::convertToResponse);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get audit logs by operation",
        description = "Retrieves audit logs for a specific operation type"
    )
    @ApiResponse(responseCode = "200", description = "Operation audit logs retrieved successfully")
    @GetMapping("/operation/{operation}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_READ')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByOperation(
            @Parameter(description = "Operation type") @PathVariable AuditLog.AuditOperation operation,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        logger.debug("Getting audit logs for operation: {}", operation);

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditService.findByOperation(operation, pageable);
        Page<AuditLogResponse> response = auditLogs.map(this::convertToResponse);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get audit logs by date range",
        description = "Retrieves audit logs within a specific date range"
    )
    @ApiResponse(responseCode = "200", description = "Date range audit logs retrieved successfully")
    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_READ')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByDateRange(
            @Parameter(description = "Start date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        logger.debug("Getting audit logs between {} and {}", startDate, endDate);

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditService.findByDateRange(startDate, endDate, pageable);
        Page<AuditLogResponse> response = auditLogs.map(this::convertToResponse);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get audit log by ID",
        description = "Retrieves a specific audit log by its ID"
    )
    @ApiResponse(responseCode = "200", description = "Audit log retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Audit log not found")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_READ')")
    public ResponseEntity<AuditLogResponse> getAuditLogById(
            @Parameter(description = "Audit log ID") @PathVariable Long id) {

        logger.debug("Getting audit log by ID: {}", id);

        return auditService.findById(id)
                .map(this::convertToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get audit statistics",
        description = "Retrieves audit statistics including operation counts and user activity"
    )
    @ApiResponse(responseCode = "200", description = "Audit statistics retrieved successfully")
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_READ')")
    public ResponseEntity<AuditStatsResponse> getAuditStatistics(
            @Parameter(description = "Start date for statistics (ISO format)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date for statistics (ISO format)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        logger.debug("Getting audit statistics between {} and {}", startDate, endDate);

        // If no date range specified, use last 30 days
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        AuditStatsResponse stats = generateAuditStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @Operation(
        summary = "Search audit logs",
        description = "Search audit logs with multiple filters"
    )
    @ApiResponse(responseCode = "200", description = "Audit logs search completed successfully")
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_READ')")
    public ResponseEntity<Page<AuditLogResponse>> searchAuditLogs(
            @Parameter(description = "Entity name filter")
            @RequestParam(required = false) String entityName,
            @Parameter(description = "Entity ID filter")
            @RequestParam(required = false) String entityId,
            @Parameter(description = "Operation filter")
            @RequestParam(required = false) AuditLog.AuditOperation operation,
            @Parameter(description = "User filter")
            @RequestParam(required = false) String performedBy,
            @Parameter(description = "Start date filter (ISO format)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date filter (ISO format)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        logger.debug("Searching audit logs with filters - entityName: {}, entityId: {}, operation: {}, performedBy: {}, startDate: {}, endDate: {}", 
                    entityName, entityId, operation, performedBy, startDate, endDate);

        // This would require implementing a search method in the service
        // For now, we'll use the existing methods based on available filters
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs;

        if (entityName != null && entityId != null) {
            auditLogs = auditService.findByEntityNameAndEntityId(entityName, entityId, pageable);
        } else if (performedBy != null && startDate != null && endDate != null) {
            auditLogs = auditService.findByPerformedByAndDateRange(performedBy, startDate, endDate, pageable);
        } else if (operation != null) {
            auditLogs = auditService.findByOperation(operation, pageable);
        } else if (startDate != null && endDate != null) {
            auditLogs = auditService.findByDateRange(startDate, endDate, pageable);
        } else {
            auditLogs = auditService.findAll(pageable);
        }

        Page<AuditLogResponse> response = auditLogs.map(this::convertToResponse);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "OPTIONS for audit logs",
        description = "Returns allowed HTTP methods for audit logs endpoint"
    )
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> optionsAuditLogs() {
        return ResponseEntity.ok()
                .header("Allow", "GET, HEAD, OPTIONS")
                .build();
    }

    @Operation(
        summary = "HEAD for audit logs",
        description = "Returns headers for audit logs endpoint without body"
    )
    @RequestMapping(method = RequestMethod.HEAD)
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_READ')")
    public ResponseEntity<Void> headAuditLogs() {
        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .build();
    }

    /**
     * Converts AuditLog entity to AuditLogResponse DTO.
     */
    private AuditLogResponse convertToResponse(AuditLog auditLog) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(auditLog.getId());
        response.setEntityName(auditLog.getEntityName());
        response.setEntityId(auditLog.getEntityId());
        response.setOperation(auditLog.getOperation());
        response.setOldValues(auditLog.getOldValues());
        response.setNewValues(auditLog.getNewValues());
        response.setPerformedBy(auditLog.getPerformedBy());
        response.setPerformedAt(auditLog.getPerformedAt());
        response.setIpAddress(auditLog.getIpAddress());
        response.setUserAgent(auditLog.getUserAgent());
        response.setSessionId(auditLog.getSessionId());
        response.setAdditionalInfo(auditLog.getAdditionalInfo());
        return response;
    }

    /**
     * Generates audit statistics for the given date range.
     */
    private AuditStatsResponse generateAuditStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        // This is a simplified implementation
        // In a real application, you would implement more sophisticated statistics
        AuditStatsResponse stats = new AuditStatsResponse();
        stats.setStartDate(startDate);
        stats.setEndDate(endDate);
        
        // Get total count for the period
        Page<AuditLog> allLogs = auditService.findByDateRange(startDate, endDate, PageRequest.of(0, 1));
        stats.setTotalOperations(allLogs.getTotalElements());
        
        // Get counts by operation type
        Map<String, Long> operationCounts = Map.of(
            "CREATE", auditService.findByOperation(AuditLog.AuditOperation.CREATE, PageRequest.of(0, 1)).getTotalElements(),
            "UPDATE", auditService.findByOperation(AuditLog.AuditOperation.UPDATE, PageRequest.of(0, 1)).getTotalElements(),
            "DELETE", auditService.findByOperation(AuditLog.AuditOperation.DELETE, PageRequest.of(0, 1)).getTotalElements()
        );
        stats.setOperationCounts(operationCounts);
        
        return stats;
    }
}