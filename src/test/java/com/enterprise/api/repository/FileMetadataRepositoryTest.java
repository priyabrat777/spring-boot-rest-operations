package com.enterprise.api.repository;

import com.enterprise.api.entity.FileMetadata;
import com.enterprise.api.entity.Role;
import com.enterprise.api.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for FileMetadataRepository.
 * Tests repository operations with H2 database.
 * 
 * Requirements addressed:
 * - 2.1: File upload and management system
 * - 5.1: Repository layer with Spring Data JPA
 * - 10.2: Integration tests with H2 database
 */
@DataJpaTest
class FileMetadataRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;
    private User otherUser;
    private FileMetadata testFile1;
    private FileMetadata testFile2;
    private FileMetadata publicFile;

    @BeforeEach
    void setUp() {
        // Create roles
        Role userRole = new Role();
        userRole.setName("USER");
        userRole = roleRepository.save(userRole);

        // Create users
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setRoles(Set.of(userRole));
        testUser = userRepository.save(testUser);

        otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password");
        otherUser.setRoles(Set.of(userRole));
        otherUser = userRepository.save(otherUser);

        // Create test files
        testFile1 = new FileMetadata();
        testFile1.setOriginalFileName("test1.txt");
        testFile1.setStoredFileName("uuid1-test1.txt");
        testFile1.setContentType("text/plain");
        testFile1.setFileSize(1024L);
        testFile1.setFilePath("/uploads/uuid1-test1.txt");
        testFile1.setChecksum("checksum1");
        testFile1.setUploadedBy(testUser);
        testFile1.setPublicAccess(false);
        testFile1.setDownloadCount(5L);
        testFile1.setCategory(FileMetadata.FileCategory.DOCUMENT);
        testFile1 = fileMetadataRepository.save(testFile1);

        testFile2 = new FileMetadata();
        testFile2.setOriginalFileName("image.jpg");
        testFile2.setStoredFileName("uuid2-image.jpg");
        testFile2.setContentType("image/jpeg");
        testFile2.setFileSize(2048L);
        testFile2.setFilePath("/uploads/uuid2-image.jpg");
        testFile2.setChecksum("checksum2");
        testFile2.setUploadedBy(testUser);
        testFile2.setPublicAccess(false);
        testFile2.setDownloadCount(10L);
        testFile2.setCategory(FileMetadata.FileCategory.IMAGE);
        testFile2 = fileMetadataRepository.save(testFile2);

        publicFile = new FileMetadata();
        publicFile.setOriginalFileName("public.pdf");
        publicFile.setStoredFileName("uuid3-public.pdf");
        publicFile.setContentType("application/pdf");
        publicFile.setFileSize(4096L);
        publicFile.setFilePath("/uploads/uuid3-public.pdf");
        publicFile.setChecksum("checksum3");
        publicFile.setUploadedBy(otherUser);
        publicFile.setPublicAccess(true);
        publicFile.setDownloadCount(15L);
        publicFile.setCategory(FileMetadata.FileCategory.DOCUMENT);
        publicFile = fileMetadataRepository.save(publicFile);

        entityManager.flush();
    }

    @Test
    void findByStoredFileName_Success() {
        // Act
        Optional<FileMetadata> result = fileMetadataRepository.findByStoredFileName("uuid1-test1.txt");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testFile1.getId(), result.get().getId());
        assertEquals("test1.txt", result.get().getOriginalFileName());
    }

    @Test
    void findByStoredFileName_NotFound() {
        // Act
        Optional<FileMetadata> result = fileMetadataRepository.findByStoredFileName("nonexistent.txt");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void findByUploadedBy_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<FileMetadata> result = fileMetadataRepository.findByUploadedBy(testUser, pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(f -> f.getUploadedBy().equals(testUser)));
    }

    @Test
    void findByContentTypeStartingWith_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<FileMetadata> result = fileMetadataRepository.findByContentTypeStartingWith("text/", pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("text/plain", result.getContent().get(0).getContentType());
    }

    @Test
    void findByCategory_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<FileMetadata> result = fileMetadataRepository.findByCategory(FileMetadata.FileCategory.DOCUMENT, pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(f -> f.getCategory() == FileMetadata.FileCategory.DOCUMENT));
    }

    @Test
    void findByPublicAccessTrue_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<FileMetadata> result = fileMetadataRepository.findByPublicAccessTrue(pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).isPublicAccess());
        assertEquals("public.pdf", result.getContent().get(0).getOriginalFileName());
    }

    @Test
    void findByUploadDateRange_Success() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<FileMetadata> result = fileMetadataRepository.findByUploadDateRange(startDate, endDate, pageable);

        // Assert
        assertEquals(3, result.getTotalElements());
    }

    @Test
    void findByOriginalFileNameContainingIgnoreCase_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<FileMetadata> result = fileMetadataRepository.findByOriginalFileNameContainingIgnoreCase("TEST", pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("test1.txt", result.getContent().get(0).getOriginalFileName());
    }

    @Test
    void findByFileSizeGreaterThan_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<FileMetadata> result = fileMetadataRepository.findByFileSizeGreaterThan(1500L, pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(f -> f.getFileSize() > 1500L));
    }

    @Test
    void findByFileSizeLessThan_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<FileMetadata> result = fileMetadataRepository.findByFileSizeLessThan(1500L, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(testFile1.getId(), result.getContent().get(0).getId());
    }

    @Test
    void getTotalStorageUsedByUser_Success() {
        // Act
        Long totalStorage = fileMetadataRepository.getTotalStorageUsedByUser(testUser);

        // Assert
        assertEquals(3072L, totalStorage); // 1024 + 2048
    }

    @Test
    void getTotalStorageUsed_Success() {
        // Act
        Long totalStorage = fileMetadataRepository.getTotalStorageUsed();

        // Assert
        assertEquals(7168L, totalStorage); // 1024 + 2048 + 4096
    }

    @Test
    void countByCategory_Success() {
        // Act
        Long documentCount = fileMetadataRepository.countByCategory(FileMetadata.FileCategory.DOCUMENT);
        Long imageCount = fileMetadataRepository.countByCategory(FileMetadata.FileCategory.IMAGE);

        // Assert
        assertEquals(2L, documentCount);
        assertEquals(1L, imageCount);
    }

    @Test
    void countByUploadedBy_Success() {
        // Act
        Long testUserCount = fileMetadataRepository.countByUploadedBy(testUser);
        Long otherUserCount = fileMetadataRepository.countByUploadedBy(otherUser);

        // Assert
        assertEquals(2L, testUserCount);
        assertEquals(1L, otherUserCount);
    }

    @Test
    void findMostDownloaded_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 5);

        // Act
        Page<FileMetadata> result = fileMetadataRepository.findMostDownloaded(pageable);

        // Assert
        assertEquals(3, result.getTotalElements());
        // Should be ordered by download count descending
        assertEquals(publicFile.getId(), result.getContent().get(0).getId()); // 15 downloads
        assertEquals(testFile2.getId(), result.getContent().get(1).getId()); // 10 downloads
        assertEquals(testFile1.getId(), result.getContent().get(2).getId()); // 5 downloads
    }

    @Test
    void findRecentlyUploaded_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 5);

        // Act
        Page<FileMetadata> result = fileMetadataRepository.findRecentlyUploaded(pageable);

        // Assert
        assertEquals(3, result.getTotalElements());
        // Should be ordered by creation date descending (most recent first)
    }

    @Test
    void existsByChecksum_Success() {
        // Act
        boolean exists = fileMetadataRepository.existsByChecksum("checksum1");
        boolean notExists = fileMetadataRepository.existsByChecksum("nonexistent");

        // Assert
        assertTrue(exists);
        assertFalse(notExists);
    }

    @Test
    void findByChecksum_Success() {
        // Act
        Optional<FileMetadata> result = fileMetadataRepository.findByChecksum("checksum1");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testFile1.getId(), result.get().getId());
    }

    @Test
    void findByDownloadCount_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<FileMetadata> result = fileMetadataRepository.findByDownloadCount(10L, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(testFile2.getId(), result.getContent().get(0).getId());
    }

    @Test
    void findOrphanedFiles_Success() {
        // Arrange - Create an orphaned file
        FileMetadata orphanedFile = new FileMetadata();
        orphanedFile.setOriginalFileName("orphaned.txt");
        orphanedFile.setStoredFileName("uuid-orphaned.txt");
        orphanedFile.setContentType("text/plain");
        orphanedFile.setFileSize(512L);
        orphanedFile.setFilePath("/uploads/uuid-orphaned.txt");
        orphanedFile.setChecksum("orphaned-checksum");
        orphanedFile.setUploadedBy(null); // Orphaned
        orphanedFile.setPublicAccess(false);
        fileMetadataRepository.save(orphanedFile);
        entityManager.flush();

        // Act
        List<FileMetadata> result = fileMetadataRepository.findOrphanedFiles();

        // Assert
        assertEquals(1, result.size());
        assertEquals("orphaned.txt", result.get(0).getOriginalFileName());
        assertNull(result.get(0).getUploadedBy());
    }

    @Test
    void findFilesOlderThan_Success() {
        // Arrange
        LocalDateTime cutoffDate = LocalDateTime.now().plusHours(1); // Future date to include all files

        // Act
        List<FileMetadata> result = fileMetadataRepository.findFilesOlderThan(cutoffDate);

        // Assert
        assertEquals(3, result.size());
    }

    @Test
    void findFilesOlderThan_NoResults() {
        // Arrange
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(1); // Past date to exclude all files

        // Act
        List<FileMetadata> result = fileMetadataRepository.findFilesOlderThan(cutoffDate);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    void softDelete_Success() {
        // Arrange
        Long fileId = testFile1.getId();

        // Act
        fileMetadataRepository.softDelete(fileId);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<FileMetadata> result = fileMetadataRepository.findById(fileId);
        assertFalse(result.isPresent()); // Should not be found due to @Where clause

        // But should be found when querying without the @Where clause
        FileMetadata deletedFile = entityManager.find(FileMetadata.class, fileId);
        assertNotNull(deletedFile);
        assertTrue(deletedFile.isDeleted());
    }

    @Test
    void findAllActive_Success() {
        // Act
        List<FileMetadata> result = fileMetadataRepository.findAllActive();

        // Assert
        assertEquals(3, result.size());
        assertTrue(result.stream().noneMatch(FileMetadata::isDeleted));
    }

    @Test
    void findByIdActive_Success() {
        // Act
        Optional<FileMetadata> result = fileMetadataRepository.findByIdActive(testFile1.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testFile1.getId(), result.get().getId());
        assertFalse(result.get().isDeleted());
    }

    @Test
    void findByIdActive_SoftDeleted_NotFound() {
        // Arrange
        fileMetadataRepository.softDelete(testFile1.getId());
        entityManager.flush();
        entityManager.clear();

        // Act
        Optional<FileMetadata> result = fileMetadataRepository.findByIdActive(testFile1.getId());

        // Assert
        assertFalse(result.isPresent());
    }
}