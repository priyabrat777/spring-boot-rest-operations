package com.enterprise.api.service;

import com.enterprise.api.dto.request.FileUploadRequest;
import com.enterprise.api.dto.response.FileMetadataResponse;
import com.enterprise.api.dto.response.FileUploadResponse;
import com.enterprise.api.entity.FileMetadata;
import com.enterprise.api.entity.Role;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FileServiceImpl.
 * Tests file upload, download, deletion, and metadata management operations.
 * 
 * Requirements addressed:
 * - 2.1: File upload and management system
 * - 10.1: Unit tests with 100% coverage for business logic
 * - 10.5: Test coverage and quality validation
 */
@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    private FileServiceImpl fileService;

    @TempDir
    Path tempDir;

    private User testUser;
    private User adminUser;
    private FileMetadata testFileMetadata;
    private MockMultipartFile testFile;

    @BeforeEach
    void setUp() {
        // Create FileService instance manually
        fileService = new FileServiceImpl(fileMetadataRepository);
        
        // Set up test configuration
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(fileService, "maxFileSize", 104857600L); // 100MB
        ReflectionTestUtils.setField(fileService, "allowedTypes", "image/jpeg,image/png,text/plain,application/pdf");
        
        // Initialize file storage manually for testing
        ReflectionTestUtils.invokeMethod(fileService, "initializeFileStorage");

        // Create test users
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRoles(new HashSet<>());

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        
        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRoles(Set.of(adminRole));

        // Create test file metadata
        testFileMetadata = new FileMetadata();
        testFileMetadata.setId(1L);
        testFileMetadata.setOriginalFileName("test.txt");
        testFileMetadata.setStoredFileName("uuid-test.txt");
        testFileMetadata.setContentType("text/plain");
        testFileMetadata.setFileSize(1024L);
        testFileMetadata.setFilePath(tempDir.resolve("uuid-test.txt").toString());
        testFileMetadata.setChecksum("abc123");
        testFileMetadata.setUploadedBy(testUser);
        testFileMetadata.setPublicAccess(false);
        testFileMetadata.setDownloadCount(0L);
        testFileMetadata.setCreatedDate(LocalDateTime.now());

        // Create test multipart file
        testFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello World".getBytes()
        );
    }

    @Test
    void uploadFile_Success() {
        // Arrange
        FileUploadRequest request = new FileUploadRequest("Test description", false);
        
        when(fileMetadataRepository.existsByChecksum(anyString())).thenReturn(false);
        when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation -> {
            FileMetadata saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        FileUploadResponse response = fileService.uploadFile(testFile, request, testUser);

        // Assert
        assertNotNull(response);
        assertEquals("test.txt", response.getOriginalFileName());
        assertEquals("text/plain", response.getContentType());
        assertEquals(testFile.getSize(), response.getFileSize()); // Actual file size
        assertFalse(response.isPublicAccess());
        
        verify(fileMetadataRepository).existsByChecksum(anyString());
        verify(fileMetadataRepository).save(any(FileMetadata.class));
    }

    @Test
    void uploadFile_EmptyFile_ThrowsException() {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        FileUploadRequest request = new FileUploadRequest();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> fileService.uploadFile(emptyFile, request, testUser));
        
        assertEquals("File is empty", exception.getMessage());
    }

    @Test
    void uploadFile_FileTooLarge_ThrowsException() {
        // Arrange
        ReflectionTestUtils.setField(fileService, "maxFileSize", 5L); // 5 bytes max
        FileUploadRequest request = new FileUploadRequest();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> fileService.uploadFile(testFile, request, testUser));
        
        assertTrue(exception.getMessage().contains("File size exceeds maximum allowed size"));
    }

    @Test
    void uploadFile_InvalidFileType_ThrowsException() {
        // Arrange
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "test.exe", "application/x-executable", "content".getBytes());
        FileUploadRequest request = new FileUploadRequest();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> fileService.uploadFile(invalidFile, request, testUser));
        
        assertTrue(exception.getMessage().contains("File type not allowed"));
    }

    @Test
    void uploadFile_DuplicateFile_ThrowsException() {
        // Arrange
        FileUploadRequest request = new FileUploadRequest();
        when(fileMetadataRepository.existsByChecksum(anyString())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> fileService.uploadFile(testFile, request, testUser));
        
        assertEquals("File already exists", exception.getMessage());
    }

    @Test
    void downloadFile_Success() throws Exception {
        // Arrange
        String storedFileName = "uuid-test.txt";
        
        // Create actual file in temp directory
        Path filePath = tempDir.resolve(storedFileName);
        java.nio.file.Files.write(filePath, "Hello World".getBytes());
        testFileMetadata.setFilePath(filePath.toString());
        
        when(fileMetadataRepository.findByStoredFileName(storedFileName)).thenReturn(Optional.of(testFileMetadata));
        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(testFileMetadata);

        // Act
        Resource resource = fileService.downloadFile(storedFileName, testUser);

        // Assert
        assertNotNull(resource);
        assertTrue(resource.exists());
        
        verify(fileMetadataRepository).findByStoredFileName(storedFileName);
        verify(fileMetadataRepository).save(testFileMetadata);
        assertEquals(1L, testFileMetadata.getDownloadCount());
    }

    @Test
    void downloadFile_FileNotFound_ThrowsException() {
        // Arrange
        String storedFileName = "nonexistent.txt";
        when(fileMetadataRepository.findByStoredFileName(storedFileName)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> fileService.downloadFile(storedFileName, testUser));
        
        assertEquals("File not found: " + storedFileName, exception.getMessage());
    }

    @Test
    void downloadFile_AccessDenied_ThrowsException() {
        // Arrange
        String storedFileName = "uuid-test.txt";
        testFileMetadata.setPublicAccess(false);
        testFileMetadata.setUploadedBy(adminUser); // Different user
        
        when(fileMetadataRepository.findByStoredFileName(storedFileName)).thenReturn(Optional.of(testFileMetadata));

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, 
                () -> fileService.downloadFile(storedFileName, testUser));
        
        assertTrue(exception.getMessage().contains("Access denied"));
    }

    @Test
    void downloadFile_PublicFile_Success() throws Exception {
        // Arrange
        String storedFileName = "uuid-test.txt";
        testFileMetadata.setPublicAccess(true);
        
        // Create actual file in temp directory
        Path filePath = tempDir.resolve(storedFileName);
        java.nio.file.Files.write(filePath, "Hello World".getBytes());
        testFileMetadata.setFilePath(filePath.toString());
        
        when(fileMetadataRepository.findByStoredFileName(storedFileName)).thenReturn(Optional.of(testFileMetadata));
        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(testFileMetadata);

        // Act - Anonymous user (null) should be able to access public file
        Resource resource = fileService.downloadFile(storedFileName, null);

        // Assert
        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void deleteFile_Success() {
        // Arrange
        Long fileId = 1L;
        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(testFileMetadata));

        // Act
        fileService.deleteFile(fileId, testUser);

        // Assert
        verify(fileMetadataRepository).findById(fileId);
        verify(fileMetadataRepository).softDelete(fileId);
    }

    @Test
    void deleteFile_FileNotFound_ThrowsException() {
        // Arrange
        Long fileId = 999L;
        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> fileService.deleteFile(fileId, testUser));
        
        assertEquals("File not found with ID: " + fileId, exception.getMessage());
    }

    @Test
    void deleteFile_AccessDenied_ThrowsException() {
        // Arrange
        Long fileId = 1L;
        testFileMetadata.setUploadedBy(adminUser); // Different user
        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(testFileMetadata));

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, 
                () -> fileService.deleteFile(fileId, testUser));
        
        assertTrue(exception.getMessage().contains("Access denied"));
    }

    @Test
    void deleteFile_AdminCanDelete() {
        // Arrange
        Long fileId = 1L;
        testFileMetadata.setUploadedBy(testUser); // Different user than admin
        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(testFileMetadata));

        // Act
        fileService.deleteFile(fileId, adminUser);

        // Assert
        verify(fileMetadataRepository).softDelete(fileId);
    }

    @Test
    void getFileMetadata_Success() {
        // Arrange
        Long fileId = 1L;
        when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(testFileMetadata));

        // Act
        FileMetadataResponse response = fileService.getFileMetadata(fileId, testUser);

        // Assert
        assertNotNull(response);
        assertEquals(testFileMetadata.getId(), response.getId());
        assertEquals(testFileMetadata.getOriginalFileName(), response.getOriginalFileName());
        assertEquals(testFileMetadata.getContentType(), response.getContentType());
    }

    @Test
    void listUserFiles_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<FileMetadata> files = Arrays.asList(testFileMetadata);
        Page<FileMetadata> filePage = new PageImpl<>(files, pageable, 1);
        
        when(fileMetadataRepository.findByUploadedBy(testUser, pageable)).thenReturn(filePage);

        // Act
        Page<FileMetadataResponse> response = fileService.listUserFiles(testUser, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(testFileMetadata.getOriginalFileName(), response.getContent().get(0).getOriginalFileName());
    }

    @Test
    void listPublicFiles_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        testFileMetadata.setPublicAccess(true);
        List<FileMetadata> files = Arrays.asList(testFileMetadata);
        Page<FileMetadata> filePage = new PageImpl<>(files, pageable, 1);
        
        when(fileMetadataRepository.findByPublicAccessTrue(pageable)).thenReturn(filePage);

        // Act
        Page<FileMetadataResponse> response = fileService.listPublicFiles(pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertTrue(response.getContent().get(0).isPublicAccess());
    }

    @Test
    void getTotalStorageUsedByUser_Success() {
        // Arrange
        Long expectedStorage = 2048L;
        when(fileMetadataRepository.getTotalStorageUsedByUser(testUser)).thenReturn(expectedStorage);

        // Act
        Long actualStorage = fileService.getTotalStorageUsedByUser(testUser);

        // Assert
        assertEquals(expectedStorage, actualStorage);
    }

    @Test
    void getUserFileStatistics_Success() {
        // Arrange
        when(fileMetadataRepository.countByUploadedBy(testUser)).thenReturn(5L);
        when(fileMetadataRepository.getTotalStorageUsedByUser(testUser)).thenReturn(10240L);
        
        List<FileMetadata> userFiles = Arrays.asList(testFileMetadata);
        Page<FileMetadata> filePage = new PageImpl<>(userFiles);
        when(fileMetadataRepository.findByUploadedBy(eq(testUser), any(Pageable.class))).thenReturn(filePage);

        // Act
        FileService.FileStatistics stats = fileService.getUserFileStatistics(testUser);

        // Assert
        assertNotNull(stats);
        assertEquals(5L, stats.getTotalFiles());
        assertEquals(10240L, stats.getTotalStorage());
    }

    @Test
    void isFileTypeAllowed_ValidType_ReturnsTrue() {
        // Act & Assert
        assertTrue(fileService.isFileTypeAllowed("image/jpeg"));
        assertTrue(fileService.isFileTypeAllowed("text/plain"));
        assertTrue(fileService.isFileTypeAllowed("application/pdf"));
    }

    @Test
    void isFileTypeAllowed_InvalidType_ReturnsFalse() {
        // Act & Assert
        assertFalse(fileService.isFileTypeAllowed("application/x-executable"));
        assertFalse(fileService.isFileTypeAllowed("video/mp4"));
        assertFalse(fileService.isFileTypeAllowed(null));
    }

    @Test
    void isFileSizeAllowed_ValidSize_ReturnsTrue() {
        // Act & Assert
        assertTrue(fileService.isFileSizeAllowed(1024L));
        assertTrue(fileService.isFileSizeAllowed(104857600L)); // Max size
    }

    @Test
    void isFileSizeAllowed_InvalidSize_ReturnsFalse() {
        // Act & Assert
        assertFalse(fileService.isFileSizeAllowed(104857601L)); // Over max size
        assertFalse(fileService.isFileSizeAllowed(0L));
        assertFalse(fileService.isFileSizeAllowed(-1L));
        assertFalse(fileService.isFileSizeAllowed(null));
    }

    @Test
    void generateStoredFileName_Success() {
        // Act
        String storedFileName = fileService.generateStoredFileName("test.txt");

        // Assert
        assertNotNull(storedFileName);
        assertTrue(storedFileName.endsWith(".txt"));
        assertNotEquals("test.txt", storedFileName);
    }

    @Test
    void calculateChecksum_Success() {
        // Act
        String checksum = fileService.calculateChecksum(testFile);

        // Assert
        assertNotNull(checksum);
        assertEquals(64, checksum.length()); // SHA-256 produces 64 character hex string
    }

    @Test
    void canUserAccessFile_OwnerAccess_ReturnsTrue() {
        // Act & Assert
        assertTrue(fileService.canUserAccessFile(testFileMetadata, testUser));
    }

    @Test
    void canUserAccessFile_PublicFile_ReturnsTrue() {
        // Arrange
        testFileMetadata.setPublicAccess(true);

        // Act & Assert
        assertTrue(fileService.canUserAccessFile(testFileMetadata, null)); // Anonymous user
        assertTrue(fileService.canUserAccessFile(testFileMetadata, testUser));
    }

    @Test
    void canUserAccessFile_AdminAccess_ReturnsTrue() {
        // Arrange
        testFileMetadata.setUploadedBy(testUser); // Different user

        // Act & Assert
        assertTrue(fileService.canUserAccessFile(testFileMetadata, adminUser));
    }

    @Test
    void canUserAccessFile_NoAccess_ReturnsFalse() {
        // Arrange
        testFileMetadata.setPublicAccess(false);
        testFileMetadata.setUploadedBy(adminUser); // Different user

        // Act & Assert
        assertFalse(fileService.canUserAccessFile(testFileMetadata, testUser));
        assertFalse(fileService.canUserAccessFile(testFileMetadata, null));
    }

    @Test
    void cleanupOldFiles_Success() {
        // Arrange
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        List<FileMetadata> oldFiles = Arrays.asList(testFileMetadata);
        
        when(fileMetadataRepository.findFilesOlderThan(cutoffDate)).thenReturn(oldFiles);

        // Act
        int cleanedCount = fileService.cleanupOldFiles(cutoffDate);

        // Assert
        assertEquals(1, cleanedCount);
        verify(fileMetadataRepository).findFilesOlderThan(cutoffDate);
        verify(fileMetadataRepository).delete(testFileMetadata);
    }
}