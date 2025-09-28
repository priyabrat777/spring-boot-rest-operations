package com.enterprise.api.dto.response;

import com.enterprise.api.entity.FileMetadata;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Response DTO for file metadata operations.
 * Contains file information returned to clients.
 * 
 * Requirements addressed:
 * - 2.1: File upload and management system
 */
public class FileMetadataResponse {

    private Long id;
    private String originalFileName;
    private String storedFileName;
    private String contentType;
    private Long fileSize;
    private String fileSizeFormatted;
    private String checksum;
    private String description;
    private Long downloadCount;
    private boolean publicAccess;
    private String category;
    private String uploadedByUsername;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastModified;

    // Constructors
    public FileMetadataResponse() {}

    public FileMetadataResponse(FileMetadata fileMetadata) {
        this.id = fileMetadata.getId();
        this.originalFileName = fileMetadata.getOriginalFileName();
        this.storedFileName = fileMetadata.getStoredFileName();
        this.contentType = fileMetadata.getContentType();
        this.fileSize = fileMetadata.getFileSize();
        this.fileSizeFormatted = fileMetadata.getFileSizeFormatted();
        this.checksum = fileMetadata.getChecksum();
        this.description = fileMetadata.getDescription();
        this.downloadCount = fileMetadata.getDownloadCount();
        this.publicAccess = fileMetadata.isPublicAccess();
        this.category = fileMetadata.getCategory().getValue();
        this.uploadedByUsername = fileMetadata.getUploadedBy() != null ? 
                                 fileMetadata.getUploadedBy().getUsername() : null;
        this.uploadedAt = fileMetadata.getCreatedDate();
        this.lastModified = fileMetadata.getLastModifiedDate();
    }

    // Static factory method
    public static FileMetadataResponse from(FileMetadata fileMetadata) {
        return new FileMetadataResponse(fileMetadata);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileSizeFormatted() {
        return fileSizeFormatted;
    }

    public void setFileSizeFormatted(String fileSizeFormatted) {
        this.fileSizeFormatted = fileSizeFormatted;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public void setPublicAccess(boolean publicAccess) {
        this.publicAccess = publicAccess;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUploadedByUsername() {
        return uploadedByUsername;
    }

    public void setUploadedByUsername(String uploadedByUsername) {
        this.uploadedByUsername = uploadedByUsername;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "FileMetadataResponse{" +
                "id=" + id +
                ", originalFileName='" + originalFileName + '\'' +
                ", storedFileName='" + storedFileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", fileSize=" + fileSize +
                ", category='" + category + '\'' +
                ", uploadedByUsername='" + uploadedByUsername + '\'' +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
}