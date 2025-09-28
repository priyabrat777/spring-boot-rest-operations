package com.enterprise.api.controller;

import com.enterprise.api.dto.response.FileMetadataResponse;
import com.enterprise.api.dto.response.FileUploadResponse;
import com.enterprise.api.entity.FileMetadata;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.UserRepository;
import com.enterprise.api.security.CustomUserPrincipal;
import com.enterprise.api.service.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for FileController.
 * Tests REST endpoints for file upload, download, deletion, and metadata operations.
 * 
 * Requirements addressed:
 * - 2.1: File upload and management system
 * - 1.1-1.8: Complete REST API operations
 * - 10.1: Unit tests with 100% coverage
 */
@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private CustomUserPrincipal userPrincipal;
    private FileUploadResponse uploadResponse;
    private FileMetadataResponse metadataResponse;
    private Resource testResource;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        userPrincipal = new CustomUserPrincipal(testUser);

        uploadResponse = new FileUploadResponse();
        uploadResponse.setId(1L);
        uploadResponse.setOriginalFileName("test.txt");
        uploadResponse.setStoredFileName("uuid-test.txt");
        uploadResponse.setContentType("text/plain");
        uploadResponse.setFileSize(1024L);
        uploadResponse.setFileSizeFormatted("1 KB");
        uploadResponse.setCategory("document");
        uploadResponse.setPublicAccess(false);
        uploadResponse.setDownloadUrl("/api/files/download/uuid-test.txt");
        uploadResponse.setUploadedAt(LocalDateTime.now());

        metadataResponse = new FileMetadataResponse();
        metadataResponse.setId(1L);
        metadataResponse.setOriginalFileName("test.txt");
        metadataResponse.setStoredFileName("uuid-test.txt");
        metadataResponse.setContentType("text/plain");
        metadataResponse.setFileSize(1024L);
        metadataResponse.setFileSizeFormatted("1 KB");
        metadataResponse.setCategory("document");
        metadataResponse.setPublicAccess(false);
        metadataResponse.setUploadedByUsername("testuser");
        metadataResponse.setUploadedAt(LocalDateTime.now());

        testResource = new ByteArrayResource("Hello World".getBytes());
    }

    @Test
    @WithMockUser(roles = "USER")
    void uploadFile_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes());

        when(userRepository.findByUsernameActive("user")).thenReturn(Optional.of(testUser));
        when(fileService.uploadFile(any(), any(), eq(testUser))).thenReturn(uploadResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/files/upload")
                .file(file)
                .param("description", "Test file")
                .param("publicAccess", "false"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/files/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.originalFileName").value("test.txt"))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andExpect(jsonPath("$.fileSize").value(1024))
                .andExpect(jsonPath("$.publicAccess").value(false));

        verify(fileService).uploadFile(any(), any(), eq(testUser));
    }

    @Test
    void uploadFile_Unauthorized() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes());

        // Act & Assert
        mockMvc.perform(multipart("/api/files/upload")
                .file(file))
                .andExpect(status().isUnauthorized());

        verify(fileService, never()).uploadFile(any(), any(), any());
    }

    @Test
    void downloadFile_Success() throws Exception {
        // Arrange
        String storedFileName = "uuid-test.txt";
        
        when(userRepository.findByUsernameActive("user")).thenReturn(Optional.of(testUser));
        when(fileService.downloadFile(storedFileName, testUser)).thenReturn(testResource);
        when(fileService.getFileMetadataByStoredName(storedFileName, testUser)).thenReturn(metadataResponse);

        // Act & Assert
        mockMvc.perform(get("/api/files/download/{storedFileName}", storedFileName)
                .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""))
                .andExpect(header().string("Content-Length", "1024"));

        verify(fileService).downloadFile(storedFileName, testUser);
        verify(fileService).getFileMetadataByStoredName(storedFileName, testUser);
    }

    @Test
    void downloadFile_Anonymous_PublicFile() throws Exception {
        // Arrange
        String storedFileName = "uuid-test.txt";
        metadataResponse.setPublicAccess(true);
        
        when(fileService.downloadFile(storedFileName, null)).thenReturn(testResource);
        when(fileService.getFileMetadataByStoredName(storedFileName, null)).thenReturn(metadataResponse);

        // Act & Assert
        mockMvc.perform(get("/api/files/download/{storedFileName}", storedFileName))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain"));

        verify(fileService).downloadFile(storedFileName, null);
    }

    @Test
    void downloadFileById_Success() throws Exception {
        // Arrange
        Long fileId = 1L;
        
        when(userRepository.findByUsernameActive("user")).thenReturn(Optional.of(testUser));
        when(fileService.downloadFileById(fileId, testUser)).thenReturn(testResource);
        when(fileService.getFileMetadata(fileId, testUser)).thenReturn(metadataResponse);

        // Act & Assert
        mockMvc.perform(get("/api/files/{fileId}/download", fileId)
                .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""));

        verify(fileService).downloadFileById(fileId, testUser);
        verify(fileService).getFileMetadata(fileId, testUser);
    }

    @Test
    void getFileMetadata_Success() throws Exception {
        // Arrange
        Long fileId = 1L;
        
        when(userRepository.findByUsernameActive("user")).thenReturn(Optional.of(testUser));
        when(fileService.getFileMetadata(fileId, testUser)).thenReturn(metadataResponse);

        // Act & Assert
        mockMvc.perform(get("/api/files/{fileId}", fileId)
                .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.originalFileName").value("test.txt"))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andExpect(jsonPath("$.uploadedByUsername").value("testuser"));

        verify(fileService).getFileMetadata(fileId, testUser);
    }

    @Test
    void getFileMetadata_Anonymous_PublicFile() throws Exception {
        // Arrange
        Long fileId = 1L;
        metadataResponse.setPublicAccess(true);
        
        when(fileService.getFileMetadata(fileId, null)).thenReturn(metadataResponse);

        // Act & Assert
        mockMvc.perform(get("/api/files/{fileId}", fileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicAccess").value(true));

        verify(fileService).getFileMetadata(fileId, null);
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteFile_Success() throws Exception {
        // Arrange
        Long fileId = 1L;
        
        when(userRepository.findByUsernameActive("user")).thenReturn(Optional.of(testUser));
        doNothing().when(fileService).deleteFile(fileId, testUser);

        // Act & Assert
        mockMvc.perform(delete("/api/files/{fileId}", fileId))
                .andExpect(status().isNoContent());

        verify(fileService).deleteFile(fileId, testUser);
    }

    @Test
    void deleteFile_Unauthorized() throws Exception {
        // Arrange
        Long fileId = 1L;

        // Act & Assert
        mockMvc.perform(delete("/api/files/{fileId}", fileId))
                .andExpect(status().isUnauthorized());

        verify(fileService, never()).deleteFile(any(), any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void listMyFiles_Success() throws Exception {
        // Arrange
        List<FileMetadataResponse> files = Arrays.asList(metadataResponse);
        Page<FileMetadataResponse> filePage = new PageImpl<>(files, PageRequest.of(0, 20), 1);
        
        when(userRepository.findByUsernameActive("user")).thenReturn(Optional.of(testUser));
        when(fileService.listUserFiles(eq(testUser), any())).thenReturn(filePage);

        // Act & Assert
        mockMvc.perform(get("/api/files/my-files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(fileService).listUserFiles(eq(testUser), any());
    }

    @Test
    void listPublicFiles_Success() throws Exception {
        // Arrange
        metadataResponse.setPublicAccess(true);
        List<FileMetadataResponse> files = Arrays.asList(metadataResponse);
        Page<FileMetadataResponse> filePage = new PageImpl<>(files, PageRequest.of(0, 20), 1);
        
        when(fileService.listPublicFiles(any())).thenReturn(filePage);

        // Act & Assert
        mockMvc.perform(get("/api/files/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].publicAccess").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(fileService).listPublicFiles(any());
    }

    @Test
    void searchFiles_Success() throws Exception {
        // Arrange
        String query = "test";
        List<FileMetadataResponse> files = Arrays.asList(metadataResponse);
        Page<FileMetadataResponse> filePage = new PageImpl<>(files, PageRequest.of(0, 20), 1);
        
        when(userRepository.findByUsernameActive("user")).thenReturn(Optional.of(testUser));
        when(fileService.searchFilesByName(eq(query), eq(testUser), any())).thenReturn(filePage);

        // Act & Assert
        mockMvc.perform(get("/api/files/search")
                .param("q", query)
                .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(fileService).searchFilesByName(eq(query), eq(testUser), any());
    }

    @Test
    void searchFiles_Anonymous() throws Exception {
        // Arrange
        String query = "test";
        List<FileMetadataResponse> files = Arrays.asList(metadataResponse);
        Page<FileMetadataResponse> filePage = new PageImpl<>(files, PageRequest.of(0, 20), 1);
        
        when(fileService.searchFilesByName(eq(query), eq(null), any())).thenReturn(filePage);

        // Act & Assert
        mockMvc.perform(get("/api/files/search")
                .param("q", query))
                .andExpect(status().isOk());

        verify(fileService).searchFilesByName(eq(query), eq(null), any());
    }

    @Test
    void listFilesByCategory_Success() throws Exception {
        // Arrange
        String category = "document";
        List<FileMetadataResponse> files = Arrays.asList(metadataResponse);
        Page<FileMetadataResponse> filePage = new PageImpl<>(files, PageRequest.of(0, 20), 1);
        
        when(userRepository.findByUsernameActive("user")).thenReturn(Optional.of(testUser));
        when(fileService.listFilesByCategory(eq(FileMetadata.FileCategory.DOCUMENT), eq(testUser), any()))
                .thenReturn(filePage);

        // Act & Assert
        mockMvc.perform(get("/api/files/category/{category}", category)
                .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(fileService).listFilesByCategory(eq(FileMetadata.FileCategory.DOCUMENT), eq(testUser), any());
    }

    @Test
    void listFilesByCategory_InvalidCategory() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/files/category/{category}", "invalid")
                .with(user(userPrincipal)))
                .andExpect(status().isBadRequest());

        verify(fileService, never()).listFilesByCategory(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUserFileStatistics_Success() throws Exception {
        // Arrange
        FileService.FileStatistics stats = new FileService.FileStatistics(5L, 10240L, 25L, null, null);
        
        when(userRepository.findByUsernameActive("user")).thenReturn(Optional.of(testUser));
        when(fileService.getUserFileStatistics(testUser)).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/files/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFiles").value(5))
                .andExpect(jsonPath("$.totalStorage").value(10240))
                .andExpect(jsonPath("$.totalDownloads").value(25));

        verify(fileService).getUserFileStatistics(testUser);
    }

    @Test
    void getFileHeaders_Success() throws Exception {
        // Arrange
        String storedFileName = "uuid-test.txt";
        
        when(userRepository.findByUsernameActive("user")).thenReturn(Optional.of(testUser));
        when(fileService.getFileMetadataByStoredName(storedFileName, testUser)).thenReturn(metadataResponse);

        // Act & Assert
        mockMvc.perform(head("/api/files/download/{storedFileName}", storedFileName)
                .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""))
                .andExpect(header().string("Content-Length", "1024"));

        verify(fileService).getFileMetadataByStoredName(storedFileName, testUser);
    }

    @Test
    void handleOptions_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(options("/api/files/upload"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "GET, POST, DELETE, HEAD, OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, POST, DELETE, HEAD, OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type, Authorization"));
    }

    @Test
    void handleIllegalArgumentException() throws Exception {
        // Arrange
        Long fileId = 1L;
        when(userRepository.findByUsernameActive("user")).thenReturn(Optional.of(testUser));
        when(fileService.getFileMetadata(fileId, testUser))
                .thenThrow(new IllegalArgumentException("File not found"));

        // Act & Assert
        mockMvc.perform(get("/api/files/{fileId}", fileId)
                .with(user(userPrincipal)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("File not found"));
    }

    @Test
    void handleSecurityException() throws Exception {
        // Arrange
        Long fileId = 1L;
        when(userRepository.findByUsernameActive("user")).thenReturn(Optional.of(testUser));
        when(fileService.getFileMetadata(fileId, testUser))
                .thenThrow(new SecurityException("Access denied"));

        // Act & Assert
        mockMvc.perform(get("/api/files/{fileId}", fileId)
                .with(user(userPrincipal)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Access denied"));
    }

    @Test
    void handleRuntimeException() throws Exception {
        // Arrange
        Long fileId = 1L;
        when(userRepository.findByUsernameActive("user")).thenReturn(Optional.of(testUser));
        when(fileService.getFileMetadata(fileId, testUser))
                .thenThrow(new RuntimeException("Internal error"));

        // Act & Assert
        mockMvc.perform(get("/api/files/{fileId}", fileId)
                .with(user(userPrincipal)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("File operation failed: Internal error"));
    }
}