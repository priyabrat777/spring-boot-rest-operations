package com.enterprise.api.repository;

import com.enterprise.api.entity.FileMetadata;
import com.enterprise.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for FileMetadata entity operations.
 * Extends BaseRepository to inherit common CRUD operations and audit functionality.
 * 
 * Requirements addressed:
 * - 2.1: File upload and management system
 * - 5.1: Repository layer with Spring Data JPA
 * - 5.2: Custom queries and specifications
 */
@Repository
public interface FileMetadataRepository extends BaseRepository<FileMetadata, Long> {

    /**
     * Find file metadata by stored file name.
     * Used for file retrieval and duplicate checking.
     */
    Optional<FileMetadata> findByStoredFileName(String storedFileName);

    /**
     * Find files uploaded by a specific user.
     * Supports pagination for large file lists.
     */
    Page<FileMetadata> findByUploadedBy(User uploadedBy, Pageable pageable);

    /**
     * Find files by content type pattern.
     * Useful for filtering by file categories.
     */
    Page<FileMetadata> findByContentTypeStartingWith(String contentTypePrefix, Pageable pageable);

    /**
     * Find files by category.
     */
    Page<FileMetadata> findByCategory(FileMetadata.FileCategory category, Pageable pageable);

    /**
     * Find public files that can be accessed without authentication.
     */
    Page<FileMetadata> findByPublicAccessTrue(Pageable pageable);

    /**
     * Find files uploaded within a date range.
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.createdDate BETWEEN :startDate AND :endDate ORDER BY f.createdDate DESC")
    Page<FileMetadata> findByUploadDateRange(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate, 
                                           Pageable pageable);

    /**
     * Find files by original file name pattern (case-insensitive).
     */
    @Query("SELECT f FROM FileMetadata f WHERE LOWER(f.originalFileName) LIKE LOWER(CONCAT('%', :fileName, '%'))")
    Page<FileMetadata> findByOriginalFileNameContainingIgnoreCase(@Param("fileName") String fileName, Pageable pageable);

    /**
     * Find files larger than specified size.
     */
    Page<FileMetadata> findByFileSizeGreaterThan(Long fileSize, Pageable pageable);

    /**
     * Find files smaller than specified size.
     */
    Page<FileMetadata> findByFileSizeLessThan(Long fileSize, Pageable pageable);

    /**
     * Get total storage used by a user.
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f WHERE f.uploadedBy = :user")
    Long getTotalStorageUsedByUser(@Param("user") User user);

    /**
     * Get total storage used across all files.
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f")
    Long getTotalStorageUsed();

    /**
     * Count files by category.
     */
    Long countByCategory(FileMetadata.FileCategory category);

    /**
     * Count files uploaded by user.
     */
    Long countByUploadedBy(User uploadedBy);

    /**
     * Find most downloaded files.
     */
    @Query("SELECT f FROM FileMetadata f ORDER BY f.downloadCount DESC")
    Page<FileMetadata> findMostDownloaded(Pageable pageable);

    /**
     * Find recently uploaded files.
     */
    @Query("SELECT f FROM FileMetadata f ORDER BY f.createdDate DESC")
    Page<FileMetadata> findRecentlyUploaded(Pageable pageable);

    /**
     * Check if file with checksum already exists.
     * Used for duplicate file detection.
     */
    boolean existsByChecksum(String checksum);

    /**
     * Find file by checksum.
     */
    Optional<FileMetadata> findByChecksum(String checksum);

    /**
     * Find files that haven't been downloaded.
     */
    Page<FileMetadata> findByDownloadCount(Long downloadCount, Pageable pageable);

    /**
     * Find orphaned files (files without valid user reference).
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.uploadedBy IS NULL")
    List<FileMetadata> findOrphanedFiles();

    /**
     * Find files older than specified date for cleanup.
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.createdDate < :cutoffDate")
    List<FileMetadata> findFilesOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}