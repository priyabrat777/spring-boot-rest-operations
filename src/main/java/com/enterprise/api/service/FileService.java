package com.enterprise.api.service;

import com.enterprise.api.dto.request.FileUploadRequest;
import com.enterprise.api.dto.response.FileMetadataResponse;
import com.enterprise.api.dto.response.FileUploadResponse;
import com.enterprise.api.entity.FileMetadata;
import com.enterprise.api.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for file management operations.
 * Provides methods for file upload, download, deletion, and metadata management.
 * 
 * Requirements addressed:
 * - 2.1: File upload and management system
 * - 2.6: File type validation and size limits
 * - 2.7: Security checks for file operations
 */
public interface FileService {

    /**
     * Upload a file with metadata.
     * Validates file type, size, and security constraints.
     * 
     * @param file The multipart file to upload
     * @param request Upload request with metadata
     * @param uploadedBy The user uploading the file
     * @return FileUploadResponse with upload details
     */
    FileUploadResponse uploadFile(MultipartFile file, FileUploadRequest request, User uploadedBy);

    /**
     * Download a file by its stored file name.
     * Checks access permissions and increments download count.
     * 
     * @param storedFileName The stored file name
     * @param requestingUser The user requesting the download (null for public files)
     * @return Resource containing the file data
     */
    Resource downloadFile(String storedFileName, User requestingUser);

    /**
     * Download a file by its ID.
     * Checks access permissions and increments download count.
     * 
     * @param fileId The file ID
     * @param requestingUser The user requesting the download (null for public files)
     * @return Resource containing the file data
     */
    Resource downloadFileById(Long fileId, User requestingUser);

    /**
     * Delete a file by its ID.
     * Performs soft delete and removes physical file.
     * 
     * @param fileId The file ID to delete
     * @param requestingUser The user requesting deletion
     */
    void deleteFile(Long fileId, User requestingUser);

    /**
     * Delete a file by its stored file name.
     * Performs soft delete and removes physical file.
     * 
     * @param storedFileName The stored file name to delete
     * @param requestingUser The user requesting deletion
     */
    void deleteFileByStoredName(String storedFileName, User requestingUser);

    /**
     * Get file metadata by ID.
     * 
     * @param fileId The file ID
     * @param requestingUser The user requesting metadata (null for public files)
     * @return FileMetadataResponse with file details
     */
    FileMetadataResponse getFileMetadata(Long fileId, User requestingUser);

    /**
     * Get file metadata by stored file name.
     * 
     * @param storedFileName The stored file name
     * @param requestingUser The user requesting metadata (null for public files)
     * @return FileMetadataResponse with file details
     */
    FileMetadataResponse getFileMetadataByStoredName(String storedFileName, User requestingUser);

    /**
     * List files uploaded by a user.
     * 
     * @param user The user whose files to list
     * @param pageable Pagination parameters
     * @return Page of FileMetadataResponse
     */
    Page<FileMetadataResponse> listUserFiles(User user, Pageable pageable);

    /**
     * List public files accessible to all users.
     * 
     * @param pageable Pagination parameters
     * @return Page of FileMetadataResponse
     */
    Page<FileMetadataResponse> listPublicFiles(Pageable pageable);

    /**
     * Search files by original file name.
     * 
     * @param fileName The file name to search for
     * @param requestingUser The user performing the search
     * @param pageable Pagination parameters
     * @return Page of FileMetadataResponse
     */
    Page<FileMetadataResponse> searchFilesByName(String fileName, User requestingUser, Pageable pageable);

    /**
     * List files by category.
     * 
     * @param category The file category
     * @param requestingUser The user requesting files
     * @param pageable Pagination parameters
     * @return Page of FileMetadataResponse
     */
    Page<FileMetadataResponse> listFilesByCategory(FileMetadata.FileCategory category, User requestingUser, Pageable pageable);

    /**
     * Get total storage used by a user.
     * 
     * @param user The user
     * @return Total storage in bytes
     */
    Long getTotalStorageUsedByUser(User user);

    /**
     * Get file statistics for a user.
     * 
     * @param user The user
     * @return FileStatistics object
     */
    FileStatistics getUserFileStatistics(User user);

    /**
     * Validate if a file type is allowed.
     * 
     * @param contentType The MIME type to validate
     * @return true if allowed, false otherwise
     */
    boolean isFileTypeAllowed(String contentType);

    /**
     * Validate if a file size is within limits.
     * 
     * @param fileSize The file size in bytes
     * @return true if within limits, false otherwise
     */
    boolean isFileSizeAllowed(Long fileSize);

    /**
     * Generate a unique stored file name.
     * 
     * @param originalFileName The original file name
     * @return Unique stored file name
     */
    String generateStoredFileName(String originalFileName);

    /**
     * Calculate file checksum.
     * 
     * @param file The multipart file
     * @return SHA-256 checksum
     */
    String calculateChecksum(MultipartFile file);

    /**
     * Check if user can access a file.
     * 
     * @param fileMetadata The file metadata
     * @param user The user (null for anonymous)
     * @return true if accessible, false otherwise
     */
    boolean canUserAccessFile(FileMetadata fileMetadata, User user);

    /**
     * Clean up old files based on retention policy.
     * 
     * @param cutoffDate Files older than this date will be considered for cleanup
     * @return Number of files cleaned up
     */
    int cleanupOldFiles(LocalDateTime cutoffDate);

    /**
     * File statistics inner class.
     */
    class FileStatistics {
        private final long totalFiles;
        private final long totalStorage;
        private final long totalDownloads;
        private final FileMetadata mostDownloadedFile;
        private final FileMetadata newestFile;

        public FileStatistics(long totalFiles, long totalStorage, long totalDownloads, 
                            FileMetadata mostDownloadedFile, FileMetadata newestFile) {
            this.totalFiles = totalFiles;
            this.totalStorage = totalStorage;
            this.totalDownloads = totalDownloads;
            this.mostDownloadedFile = mostDownloadedFile;
            this.newestFile = newestFile;
        }

        // Getters
        public long getTotalFiles() { return totalFiles; }
        public long getTotalStorage() { return totalStorage; }
        public long getTotalDownloads() { return totalDownloads; }
        public FileMetadata getMostDownloadedFile() { return mostDownloadedFile; }
        public FileMetadata getNewestFile() { return newestFile; }
    }
}