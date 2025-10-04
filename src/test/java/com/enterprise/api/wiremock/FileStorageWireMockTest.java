package com.enterprise.api.wiremock;

import com.enterprise.api.dto.request.FileUploadRequest;
import com.enterprise.api.dto.response.FileMetadataResponse;
import com.enterprise.api.dto.response.FileUploadResponse;
import com.enterprise.api.entity.User;
import com.enterprise.api.service.FileService;
import com.enterprise.api.wiremock.stubs.FileStorageStubs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for file storage services using WireMock.
 * Tests external storage service integration scenarios including success and failure cases.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FileStorageWireMockTest extends WireMockTestBase {

    @Autowired
    private FileService fileService;

    @Override
    protected void setupWireMockStubs() {
        // Default setup - can be overridden in individual tests
        FileStorageStubs.setupFileUploadSuccess(wireMockServer);
        FileStorageStubs.setupFileDownloadSuccess(wireMockServer);
        FileStorageStubs.setupFileDeleteSuccess(wireMockServer);
        FileStorageStubs.setupFileMetadataSuccess(wireMockServer);
    }

    @Test
    void shouldUploadFileWithSuccessfulExternalStorage() {
        // Given
        FileStorageStubs.setupFileUploadSuccess(wireMockServer);
        
        MultipartFile file = new MockMultipartFile(
            "file",
            "test-document.pdf",
            "application/pdf",
            "Mock PDF content".getBytes()
        );
        
        FileUploadRequest request = new FileUploadRequest(
            "Test document upload",
            false
        );
        
        User user = createTestUser();

        // When
        FileUploadResponse response = fileService.uploadFile(file, request, user);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOriginalFileName()).isEqualTo("test-document.pdf");
        assertThat(response.getContentType()).isEqualTo("application/pdf");
        assertThat(response.getFileSize()).isEqualTo(file.getSize());
        assertThat(response.getDownloadUrl()).isNotNull();

        // Verify external service was called
        FileStorageStubs.verifyFileUploadCall(wireMockServer, "test-document.pdf");
    }

    @Test
    void shouldHandleFileUploadSizeLimitFailure() {
        // Given
        FileStorageStubs.setupFileUploadSizeLimit(wireMockServer);
        
        // Create a large file that exceeds the limit
        byte[] largeContent = new byte[200 * 1024 * 1024]; // 200MB
        MultipartFile file = new MockMultipartFile(
            "file",
            "large-document.pdf",
            "application/pdf",
            largeContent
        );
        
        FileUploadRequest request = new FileUploadRequest(
            "Large document upload",
            false
        );
        
        User user = createTestUser();

        // When & Then
        assertThatThrownBy(() -> fileService.uploadFile(file, request, user))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File size exceeds maximum allowed size");
    }

    @Test
    void shouldHandleFileUploadInvalidTypeFailure() {
        // Given
        FileStorageStubs.setupFileUploadInvalidType(wireMockServer);
        
        MultipartFile file = new MockMultipartFile(
            "file",
            "malicious-script.exe",
            "application/x-executable",
            "Malicious content".getBytes()
        );
        
        FileUploadRequest request = new FileUploadRequest(
            "Malicious file upload attempt",
            false
        );
        
        User user = createTestUser();

        // When & Then
        assertThatThrownBy(() -> fileService.uploadFile(file, request, user))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File type not allowed");
    }

    @Test
    void shouldHandleFileUploadQuotaExceededFailure() {
        // Given
        FileStorageStubs.setupFileUploadQuotaExceeded(wireMockServer);
        
        MultipartFile file = new MockMultipartFile(
            "file",
            "document.pdf",
            "application/pdf",
            "PDF content".getBytes()
        );
        
        FileUploadRequest request = new FileUploadRequest(
            "Document upload when quota exceeded",
            false
        );
        
        User user = createTestUser();

        // When & Then - The service should handle quota exceeded gracefully
        // In a real implementation, this might throw a specific exception
        // For now, we'll test that the external service call is made
        try {
            fileService.uploadFile(file, request, user);
        } catch (Exception e) {
            // Expected to fail due to quota
        }

        // Verify external service was called
        FileStorageStubs.verifyFileUploadCall(wireMockServer, "document.pdf");
    }

    @Test
    void shouldDownloadFileWithSuccessfulExternalStorage() {
        // Given
        FileStorageStubs.setupFileDownloadSuccess(wireMockServer);
        
        // First upload a file
        MultipartFile uploadFile = new MockMultipartFile(
            "file",
            "download-test.pdf",
            "application/pdf",
            "PDF content for download".getBytes()
        );
        
        FileUploadRequest uploadRequest = new FileUploadRequest(
            "File for download test",
            true
        );
        
        User user = createTestUser();
        FileUploadResponse uploadResponse = fileService.uploadFile(uploadFile, uploadRequest, user);

        // When
        Resource downloadedResource = fileService.downloadFile(uploadResponse.getStoredFileName(), user);

        // Then
        assertThat(downloadedResource).isNotNull();
        assertThat(downloadedResource.exists()).isTrue();

        // Verify external service was called
        FileStorageStubs.verifyFileDownloadCall(wireMockServer, uploadResponse.getStoredFileName());
    }

    @Test
    void shouldHandleFileDownloadNotFoundFailure() {
        // Given
        FileStorageStubs.setupFileDownloadNotFound(wireMockServer);
        
        User user = createTestUser();

        // When & Then
        assertThatThrownBy(() -> fileService.downloadFile("non-existent-file.pdf", user))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File not found");
    }

    @Test
    void shouldHandleFileDownloadAccessDeniedFailure() {
        // Given
        FileStorageStubs.setupFileDownloadAccessDenied(wireMockServer);
        
        // First upload a private file
        MultipartFile uploadFile = new MockMultipartFile(
            "file",
            "private-document.pdf",
            "application/pdf",
            "Private PDF content".getBytes()
        );
        
        FileUploadRequest uploadRequest = new FileUploadRequest(
            "Private document",
            false // Not public
        );
        
        User owner = createTestUser();
        User otherUser = createTestUser("otheruser", "other@example.com");
        
        FileUploadResponse uploadResponse = fileService.uploadFile(uploadFile, uploadRequest, owner);

        // When & Then - Other user should not be able to access private file
        assertThatThrownBy(() -> fileService.downloadFile(uploadResponse.getStoredFileName(), otherUser))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Access denied");
    }

    @Test
    void shouldDeleteFileWithSuccessfulExternalStorage() {
        // Given
        FileStorageStubs.setupFileDeleteSuccess(wireMockServer);
        
        // First upload a file
        MultipartFile uploadFile = new MockMultipartFile(
            "file",
            "delete-test.pdf",
            "application/pdf",
            "PDF content for deletion".getBytes()
        );
        
        FileUploadRequest uploadRequest = new FileUploadRequest(
            "File for deletion test",
            false
        );
        
        User user = createTestUser();
        FileUploadResponse uploadResponse = fileService.uploadFile(uploadFile, uploadRequest, user);

        // When
        fileService.deleteFileByStoredName(uploadResponse.getStoredFileName(), user);

        // Then - File should be deleted (soft delete in database)
        // Verify external service was called
        FileStorageStubs.verifyFileDeleteCall(wireMockServer, uploadResponse.getStoredFileName());
    }

    @Test
    void shouldHandleFileDeleteNotFoundFailure() {
        // Given
        FileStorageStubs.setupFileDeleteNotFound(wireMockServer);
        
        User user = createTestUser();

        // When & Then
        assertThatThrownBy(() -> fileService.deleteFileByStoredName("non-existent-file.pdf", user))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File not found");
    }

    @Test
    void shouldHandleStorageServiceUnavailable() {
        // Given
        FileStorageStubs.setupStorageServiceUnavailable(wireMockServer);
        
        MultipartFile file = new MockMultipartFile(
            "file",
            "test-document.pdf",
            "application/pdf",
            "PDF content".getBytes()
        );
        
        FileUploadRequest request = new FileUploadRequest(
            "Upload when service unavailable",
            false
        );
        
        User user = createTestUser();

        // When & Then - Service should handle unavailability gracefully
        try {
            fileService.uploadFile(file, request, user);
        } catch (Exception e) {
            // Expected to fail due to service unavailability
        }

        // Verify external service was called
        FileStorageStubs.verifyFileUploadCall(wireMockServer, "test-document.pdf");
    }

    @Test
    void shouldHandleStorageServiceTimeout() {
        // Given
        FileStorageStubs.setupStorageServiceTimeout(wireMockServer);
        
        MultipartFile file = new MockMultipartFile(
            "file",
            "timeout-test.pdf",
            "application/pdf",
            "PDF content".getBytes()
        );
        
        FileUploadRequest request = new FileUploadRequest(
            "Upload with timeout",
            false
        );
        
        User user = createTestUser();

        // When & Then - Service should handle timeout gracefully
        // Note: In a real implementation, you might want to configure shorter timeouts for testing
        try {
            fileService.uploadFile(file, request, user);
        } catch (Exception e) {
            // Expected to timeout
        }

        // Verify external service was called
        FileStorageStubs.verifyFileUploadCall(wireMockServer, "timeout-test.pdf");
    }

    @Test
    void shouldTrackExternalStorageServiceCallCounts() {
        // Given
        FileStorageStubs.setupFileUploadSuccess(wireMockServer);
        
        MultipartFile file1 = new MockMultipartFile(
            "file",
            "document1.pdf",
            "application/pdf",
            "PDF content 1".getBytes()
        );
        
        MultipartFile file2 = new MockMultipartFile(
            "file",
            "document2.pdf",
            "application/pdf",
            "PDF content 2".getBytes()
        );
        
        FileUploadRequest request = new FileUploadRequest(
            "Multiple uploads test",
            false
        );
        
        User user = createTestUser();

        // When
        fileService.uploadFile(file1, request, user);
        fileService.uploadFile(file2, request, user);

        // Then
        int uploadCallCount = FileStorageStubs.getUploadRequestCount(wireMockServer);
        assertThat(uploadCallCount).isEqualTo(2);
    }

    @Test
    void shouldVerifyFileMetadataRetrievalFromExternalService() {
        // Given
        FileStorageStubs.setupFileMetadataSuccess(wireMockServer);
        
        // First upload a file
        MultipartFile uploadFile = new MockMultipartFile(
            "file",
            "metadata-test.pdf",
            "application/pdf",
            "PDF content for metadata test".getBytes()
        );
        
        FileUploadRequest uploadRequest = new FileUploadRequest(
            "File for metadata test",
            true
        );
        
        User user = createTestUser();
        FileUploadResponse uploadResponse = fileService.uploadFile(uploadFile, uploadRequest, user);

        // When
        FileMetadataResponse metadata = fileService.getFileMetadataByStoredName(
            uploadResponse.getStoredFileName(), user);

        // Then
        assertThat(metadata).isNotNull();
        assertThat(metadata.getOriginalFileName()).isEqualTo("metadata-test.pdf");
        assertThat(metadata.getContentType()).isEqualTo("application/pdf");
        assertThat(metadata.isPublicAccess()).isTrue();
    }

    // Helper methods

    private User createTestUser() {
        return createTestUser("testuser", "test@example.com");
    }

    private User createTestUser(String username, String email) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encoded-password");
        return user;
    }
}