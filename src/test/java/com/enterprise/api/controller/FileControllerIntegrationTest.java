package com.enterprise.api.controller;

import com.enterprise.api.entity.Role;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.FileMetadataRepository;
import com.enterprise.api.repository.RoleRepository;
import com.enterprise.api.repository.UserRepository;
import com.enterprise.api.security.CustomUserPrincipal;
import com.enterprise.api.security.JwtTokenProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FileController with full Spring context.
 * Tests complete file upload, download, and management workflows.
 * 
 * Requirements addressed:
 * - 2.1: File upload and management system
 * - 10.2: Integration tests with H2 database
 * - 10.4: Edge cases and security tests
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.file.upload-dir=${java.io.tmpdir}/test-uploads",
    "app.file.max-size=1048576", // 1MB for testing
    "app.file.allowed-types=text/plain,image/jpeg,image/png,application/pdf"
})
@Transactional
class FileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @TempDir
    Path tempDir;

    private User testUser;
    private User adminUser;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        // Clean up repositories
        fileMetadataRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Create roles
        Role userRole = new Role();
        userRole.setName("USER");
        userRole = roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole = roleRepository.save(adminRole);

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setRoles(Set.of(userRole));
        testUser = userRepository.save(testUser);

        // Create admin user
        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("password"));
        adminUser.setRoles(Set.of(userRole, adminRole));
        adminUser = userRepository.save(adminUser);

        // Generate JWT tokens
        CustomUserPrincipal userPrincipal = new CustomUserPrincipal(testUser);
        CustomUserPrincipal adminPrincipal = new CustomUserPrincipal(adminUser);
        
        Authentication userAuth = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
        
        userToken = jwtTokenProvider.generateToken(userAuth);
        adminToken = jwtTokenProvider.generateToken(adminAuth);
    }

    @Test
    void completeFileWorkflow_Success() throws Exception {
        // 1. Upload a file
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World Content".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("description", "Test file upload")
                .param("publicAccess", "false")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.originalFileName").value("test.txt"))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andExpect(jsonPath("$.fileSize").value(19))
                .andExpect(jsonPath("$.publicAccess").value(false))
                .andExpect(jsonPath("$.downloadUrl").exists())
                .andReturn().getResponse().getContentAsString();

        // Extract file ID from response
        Long fileId = objectMapper.readTree(uploadResponse).get("id").asLong();
        String storedFileName = objectMapper.readTree(uploadResponse).get("storedFileName").asText();

        // 2. Get file metadata
        mockMvc.perform(get("/api/files/{fileId}", fileId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fileId))
                .andExpect(jsonPath("$.originalFileName").value("test.txt"))
                .andExpect(jsonPath("$.uploadedByUsername").value("testuser"));

        // 3. Download the file
        mockMvc.perform(get("/api/files/download/{storedFileName}", storedFileName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain"))
                .andExpect(header().string("Content-Disposition", containsString("test.txt")))
                .andExpect(content().string("Hello World Content"));

        // 4. List user files
        mockMvc.perform(get("/api/files/my-files")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(fileId));

        // 5. Get file statistics
        mockMvc.perform(get("/api/files/statistics")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFiles").value(1))
                .andExpect(jsonPath("$.totalStorage").value(19))
                .andExpect(jsonPath("$.totalDownloads").value(1)); // Downloaded once

        // 6. Delete the file
        mockMvc.perform(delete("/api/files/{fileId}", fileId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isNoContent());

        // 7. Verify file is deleted
        mockMvc.perform(get("/api/files/{fileId}", fileId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadFile_PublicAccess_Success() throws Exception {
        // Upload a public file
        MockMultipartFile file = new MockMultipartFile(
                "file", "public.txt", "text/plain", "Public content".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("description", "Public file")
                .param("publicAccess", "true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicAccess").value(true))
                .andReturn().getResponse().getContentAsString();

        String storedFileName = objectMapper.readTree(uploadResponse).get("storedFileName").asText();

        // Anonymous user should be able to download public file
        mockMvc.perform(get("/api/files/download/{storedFileName}", storedFileName))
                .andExpect(status().isOk())
                .andExpect(content().string("Public content"));

        // Public file should appear in public files list
        mockMvc.perform(get("/api/files/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].publicAccess").value(true));
    }

    @Test
    void uploadFile_FileTooLarge_Failure() throws Exception {
        // Create a file larger than the 1MB limit
        byte[] largeContent = new byte[1048577]; // 1MB + 1 byte
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.txt", "text/plain", largeContent);

        mockMvc.perform(multipart("/api/files/upload")
                .file(largeFile)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("File size exceeds maximum allowed size")));
    }

    @Test
    void uploadFile_InvalidFileType_Failure() throws Exception {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "script.exe", "application/x-executable", "malicious content".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                .file(invalidFile)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("File type not allowed")));
    }

    @Test
    void uploadFile_EmptyFile_Failure() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/api/files/upload")
                .file(emptyFile)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("File is empty"));
    }

    @Test
    void downloadFile_AccessDenied_Failure() throws Exception {
        // User 1 uploads a private file
        MockMultipartFile file = new MockMultipartFile(
                "file", "private.txt", "text/plain", "Private content".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("publicAccess", "false")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String storedFileName = objectMapper.readTree(uploadResponse).get("storedFileName").asText();

        // Anonymous user should not be able to download private file
        mockMvc.perform(get("/api/files/download/{storedFileName}", storedFileName))
                .andExpect(status().isForbidden());

        // Different user should not be able to download private file
        // (We would need another user token for this test)
    }

    @Test
    void deleteFile_AccessDenied_Failure() throws Exception {
        // User uploads a file
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Test content".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long fileId = objectMapper.readTree(uploadResponse).get("id").asLong();

        // Create another user
        Role userRole = roleRepository.findByNameActive("USER").orElseThrow();
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword(passwordEncoder.encode("password"));
        otherUser.setRoles(Set.of(userRole));
        otherUser = userRepository.save(otherUser);

        CustomUserPrincipal otherUserPrincipal = new CustomUserPrincipal(otherUser);
        Authentication otherUserAuth = new UsernamePasswordAuthenticationToken(otherUserPrincipal, null, otherUserPrincipal.getAuthorities());
        String otherUserToken = jwtTokenProvider.generateToken(otherUserAuth);

        // Other user should not be able to delete the file
        mockMvc.perform(delete("/api/files/{fileId}", fileId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAllFiles() throws Exception {
        // User uploads a private file
        MockMultipartFile file = new MockMultipartFile(
                "file", "private.txt", "text/plain", "Private content".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("publicAccess", "false")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long fileId = objectMapper.readTree(uploadResponse).get("id").asLong();
        String storedFileName = objectMapper.readTree(uploadResponse).get("storedFileName").asText();

        // Admin should be able to access the file
        mockMvc.perform(get("/api/files/{fileId}", fileId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Admin should be able to download the file
        mockMvc.perform(get("/api/files/download/{storedFileName}", storedFileName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Admin should be able to delete the file
        mockMvc.perform(delete("/api/files/{fileId}", fileId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void searchFiles_Success() throws Exception {
        // Upload multiple files
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "document.txt", "text/plain", "Document content".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "image.jpg", "image/jpeg", "Image content".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                .file(file1)
                .param("publicAccess", "true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isCreated());

        mockMvc.perform(multipart("/api/files/upload")
                .file(file2)
                .param("publicAccess", "true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isCreated());

        // Search for files
        mockMvc.perform(get("/api/files/search")
                .param("q", "document"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].originalFileName").value("document.txt"));
    }

    @Test
    void listFilesByCategory_Success() throws Exception {
        // Upload files of different categories
        MockMultipartFile textFile = new MockMultipartFile(
                "file", "document.txt", "text/plain", "Text content".getBytes());
        MockMultipartFile imageFile = new MockMultipartFile(
                "file", "image.jpg", "image/jpeg", "Image content".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                .file(textFile)
                .param("publicAccess", "true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isCreated());

        mockMvc.perform(multipart("/api/files/upload")
                .file(imageFile)
                .param("publicAccess", "true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isCreated());

        // List document files
        mockMvc.perform(get("/api/files/category/document"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].category").value("document"));

        // List image files
        mockMvc.perform(get("/api/files/category/image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].category").value("image"));
    }

    @Test
    void headRequest_Success() throws Exception {
        // Upload a file
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Test content".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("publicAccess", "true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String storedFileName = objectMapper.readTree(uploadResponse).get("storedFileName").asText();

        // HEAD request should return headers without body
        mockMvc.perform(head("/api/files/download/{storedFileName}", storedFileName))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain"))
                .andExpect(header().string("Content-Disposition", containsString("test.txt")))
                .andExpect(header().string("Content-Length", "12"));
    }

    @Test
    void optionsRequest_Success() throws Exception {
        mockMvc.perform(options("/api/files/upload"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "GET, POST, DELETE, HEAD, OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, POST, DELETE, HEAD, OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type, Authorization"));
    }

    @Test
    void unauthorizedAccess_Failure() throws Exception {
        // Try to upload without authentication
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Test content".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                .file(file))
                .andExpect(status().isUnauthorized());

        // Try to access my-files without authentication
        mockMvc.perform(get("/api/files/my-files"))
                .andExpect(status().isUnauthorized());

        // Try to delete without authentication
        mockMvc.perform(delete("/api/files/1"))
                .andExpect(status().isUnauthorized());
    }
}