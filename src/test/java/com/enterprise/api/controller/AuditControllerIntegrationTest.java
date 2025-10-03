package com.enterprise.api.controller;

import com.enterprise.api.audit.AuditService;
import com.enterprise.api.entity.AuditLog;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.AuditLogRepository;
import com.enterprise.api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuditController.
 * Tests the complete audit functionality with real database interactions.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class AuditControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    private AuditLog testAuditLog1;
    private AuditLog testAuditLog2;
    private AuditLog testAuditLog3;

    @BeforeEach
    void setUp() {
        // Clean up any existing audit logs
        auditLogRepository.deleteAll();

        // Create test audit logs
        testAuditLog1 = auditService.createAuditLog(
            "User", "1", AuditLog.AuditOperation.CREATE,
            null, "{\"username\":\"testuser1\",\"email\":\"test1@example.com\"}",
            "admin", "192.168.1.100", "Mozilla/5.0", "Test creation"
        );

        testAuditLog2 = auditService.createAuditLog(
            "User", "1", AuditLog.AuditOperation.UPDATE,
            "{\"email\":\"test1@example.com\"}", "{\"email\":\"updated1@example.com\"}",
            "admin", "192.168.1.100", "Mozilla/5.0", "Test update"
        );

        testAuditLog3 = auditService.createAuditLog(
            "Role", "2", AuditLog.AuditOperation.CREATE,
            null, "{\"name\":\"ADMIN\",\"description\":\"Administrator role\"}",
            "superadmin", "192.168.1.101", "Chrome/91.0", "Role creation"
        );

        // Set different timestamps for testing date range queries
        testAuditLog1.setPerformedAt(LocalDateTime.now().minusDays(2));
        testAuditLog2.setPerformedAt(LocalDateTime.now().minusDays(1));
        testAuditLog3.setPerformedAt(LocalDateTime.now().minusHours(1));

        auditLogRepository.save(testAuditLog1);
        auditLogRepository.save(testAuditLog2);
        auditLogRepository.save(testAuditLog3);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllAuditLogs_ShouldReturnAllAuditLogsWithPagination() throws Exception {
        mockMvc.perform(get("/api/v1/audit")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.content[*].entityName", containsInAnyOrder("User", "User", "Role")))
                .andExpect(jsonPath("$.content[*].operation", containsInAnyOrder("CREATE", "UPDATE", "CREATE")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLogsByEntity_ShouldReturnEntitySpecificLogs() throws Exception {
        mockMvc.perform(get("/api/v1/audit/entity/User/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].entityName", everyItem(is("User"))))
                .andExpect(jsonPath("$.content[*].entityId", everyItem(is("1"))))
                .andExpect(jsonPath("$.content[*].operation", containsInAnyOrder("CREATE", "UPDATE")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAuditLogsByUser_ShouldReturnUserSpecificLogs() throws Exception {
        mockMvc.perform(get("/api/v1/audit/user/admin"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].performedBy", everyItem(is("admin"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLogsByOperation_ShouldReturnOperationSpecificLogs() throws Exception {
        mockMvc.perform(get("/api/v1/audit/operation/CREATE"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].operation", everyItem(is("CREATE"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLogsByDateRange_ShouldReturnDateFilteredLogs() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().minusDays(1).minusHours(1);
        LocalDateTime endDate = LocalDateTime.now();

        mockMvc.perform(get("/api/v1/audit/date-range")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2))) // Should return testAuditLog2 and testAuditLog3
                .andExpect(jsonPath("$.content[*].performedBy", containsInAnyOrder("admin", "superadmin")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLogById_ExistingLog_ShouldReturnSpecificLog() throws Exception {
        mockMvc.perform(get("/api/v1/audit/" + testAuditLog1.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testAuditLog1.getId().intValue())))
                .andExpect(jsonPath("$.entityName", is("User")))
                .andExpect(jsonPath("$.entityId", is("1")))
                .andExpect(jsonPath("$.operation", is("CREATE")))
                .andExpect(jsonPath("$.performedBy", is("admin")))
                .andExpect(jsonPath("$.ipAddress", is("192.168.1.100")))
                .andExpect(jsonPath("$.userAgent", is("Mozilla/5.0")))
                .andExpect(jsonPath("$.additionalInfo", is("Test creation")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLogById_NonExistingLog_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/audit/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditStatistics_ShouldReturnCorrectStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/audit/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalOperations", greaterThan(0)))
                .andExpect(jsonPath("$.operationCounts", notNullValue()))
                .andExpect(jsonPath("$.startDate", notNullValue()))
                .andExpect(jsonPath("$.endDate", notNullValue()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditStatistics_WithDateRange_ShouldReturnFilteredStatistics() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().minusDays(1).minusHours(1);
        LocalDateTime endDate = LocalDateTime.now();

        mockMvc.perform(get("/api/v1/audit/stats")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalOperations", is(2))) // Should count testAuditLog2 and testAuditLog3
                .andExpect(jsonPath("$.startDate", is(startDate.toString())))
                .andExpect(jsonPath("$.endDate", is(endDate.toString())));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchAuditLogs_WithEntityNameAndId_ShouldReturnFilteredResults() throws Exception {
        mockMvc.perform(get("/api/v1/audit/search")
                .param("entityName", "User")
                .param("entityId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].entityName", everyItem(is("User"))))
                .andExpect(jsonPath("$.content[*].entityId", everyItem(is("1"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchAuditLogs_WithUserAndDateRange_ShouldReturnFilteredResults() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().minusDays(3);
        LocalDateTime endDate = LocalDateTime.now().minusHours(2);

        mockMvc.perform(get("/api/v1/audit/search")
                .param("performedBy", "admin")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].performedBy", everyItem(is("admin"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchAuditLogs_WithOperation_ShouldReturnFilteredResults() throws Exception {
        mockMvc.perform(get("/api/v1/audit/search")
                .param("operation", "UPDATE"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].operation", is("UPDATE")))
                .andExpect(jsonPath("$.content[0].oldValues", containsString("test1@example.com")))
                .andExpect(jsonPath("$.content[0].newValues", containsString("updated1@example.com")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchAuditLogs_WithDateRangeOnly_ShouldReturnFilteredResults() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().minusHours(2);
        LocalDateTime endDate = LocalDateTime.now();

        mockMvc.perform(get("/api/v1/audit/search")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].performedBy", is("superadmin")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchAuditLogs_NoFilters_ShouldReturnAllResults() throws Exception {
        mockMvc.perform(get("/api/v1/audit/search"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getAuditLogsByUser_OwnUser_ShouldAllowAccess() throws Exception {
        // Create an audit log for the test user
        auditService.createAuditLog(
            "Profile", "testuser", AuditLog.AuditOperation.UPDATE,
            "{\"email\":\"old@test.com\"}", "{\"email\":\"new@test.com\"}",
            "testuser", "192.168.1.102", "Safari/14.0", "Profile update"
        );

        mockMvc.perform(get("/api/v1/audit/user/testuser"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].performedBy", is("testuser")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getAuditLogsByUser_DifferentUser_ShouldDenyAccess() throws Exception {
        mockMvc.perform(get("/api/v1/audit/user/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllAuditLogs_WithSorting_ShouldReturnSortedResults() throws Exception {
        mockMvc.perform(get("/api/v1/audit")
                .param("sortBy", "performedAt")
                .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(3)))
                // First should be the oldest (testAuditLog1)
                .andExpect(jsonPath("$.content[0].entityName", is("User")))
                .andExpect(jsonPath("$.content[0].operation", is("CREATE")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllAuditLogs_WithPagination_ShouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/api/v1/audit")
                .param("page", "0")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.first", is(true)))
                .andExpect(jsonPath("$.last", is(false)));

        mockMvc.perform(get("/api/v1/audit")
                .param("page", "1")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.first", is(false)))
                .andExpect(jsonPath("$.last", is(true)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void optionsAuditLogs_ShouldReturnAllowedMethods() throws Exception {
        mockMvc.perform(options("/api/v1/audit"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "GET, HEAD, OPTIONS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void headAuditLogs_ShouldReturnHeadersWithoutBody() throws Exception {
        mockMvc.perform(head("/api/v1/audit"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(content().string(""));
    }
}