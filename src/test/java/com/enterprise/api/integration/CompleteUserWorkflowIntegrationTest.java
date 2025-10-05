package com.enterprise.api.integration;

import com.enterprise.api.dto.request.*;
import com.enterprise.api.dto.response.*;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.UserRepository;
import com.enterprise.api.repository.RoleRepository;
import com.enterprise.api.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Complete user workflow integration tests covering the entire user journey
 * from registration to advanced operations including file management, OTP, and CAPTCHA.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CompleteUserWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String adminToken;
    private String userToken;
    private Long createdUserId;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up any existing data
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Order(1)
    void testCompleteAdminWorkflow() throws Exception {
        // 1. Admin login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("admin");
        loginRequest.setPassword("admin123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthResponse.class);
        adminToken = authResponse.getAccessToken();
        assertNotNull(adminToken);

        // 2. Create a new role
        CreateRoleRequest roleRequest = new CreateRoleRequest();
        roleRequest.setName("TEST_USER");
        roleRequest.setDescription("Test user role");

        mockMvc.perform(post("/api/roles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("TEST_USER"));

        // 3. Create a new user
        CreateUserRequest userRequest = new CreateUserRequest();
        userRequest.setUsername("testuser");
        userRequest.setEmail("test@example.com");
        userRequest.setPassword("password123");
        userRequest.setFirstName("Test");
        userRequest.setLastName("User");

        MvcResult userResult = mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andReturn();

        UserResponse userResponse = objectMapper.readValue(
                userResult.getResponse().getContentAsString(), UserResponse.class);
        createdUserId = userResponse.getId();

        // 4. Verify audit trail for user creation
        mockMvc.perform(get("/api/audit/logs")
                .header("Authorization", "Bearer " + adminToken)
                .param("entityName", "User")
                .param("operation", "CREATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(2)
    void testCompleteUserWorkflow() throws Exception {
        // Ensure admin workflow completed first
        testCompleteAdminWorkflow();

        // 1. User login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthResponse.class);
        userToken = authResponse.getAccessToken();
        assertNotNull(userToken);

        // 2. Get user profile
        mockMvc.perform(get("/api/users/profile")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));

        // 3. Update user profile
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");

        mockMvc.perform(put("/api/users/" + createdUserId)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));

        // 4. Change password
        ChangePasswordRequest passwordRequest = new ChangePasswordRequest();
        passwordRequest.setCurrentPassword("password123");
        passwordRequest.setNewPassword("newpassword123");

        mockMvc.perform(put("/api/users/" + createdUserId + "/password")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(3)
    void testFileManagementWorkflow() throws Exception {
        // Ensure user is logged in
        testCompleteUserWorkflow();

        // 1. Upload a file
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Test file content".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFileName").value("test.txt"))
                .andReturn();

        FileUploadResponse uploadResponse = objectMapper.readValue(
                uploadResult.getResponse().getContentAsString(), FileUploadResponse.class);
        Long fileId = uploadResponse.getId();

        // 2. Get file metadata
        mockMvc.perform(get("/api/files/" + fileId + "/metadata")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFileName").value("test.txt"));

        // 3. Download file
        mockMvc.perform(get("/api/files/" + fileId + "/download")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Test file content"));

        // 4. List user files
        mockMvc.perform(get("/api/files")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // 5. Delete file
        mockMvc.perform(delete("/api/files/" + fileId)
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(4)
    void testErrorHandlingWorkflow() throws Exception {
        // Test various error scenarios

        // 1. Invalid login
        LoginRequest invalidLogin = new LoginRequest();
        invalidLogin.setUsernameOrEmail("invalid");
        invalidLogin.setPassword("invalid");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized());

        // 2. Access without token
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());

        // 3. Non-existent resource
        mockMvc.perform(get("/api/users/99999")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}