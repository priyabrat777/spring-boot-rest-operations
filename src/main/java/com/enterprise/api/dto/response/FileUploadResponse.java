package com.enterprise.api.dto.response;

import com.enterprise.api.entity.FileMetadata;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Response DTO for file upload operations.
 * Contains essential information about the uploaded file.
 * 
 * Requirements addressed:
 * - 2.1: File upload and management system
 */
public class FileUploadResponse {

    private Long id;
    private String originalFileName;
    private String storedFileName;
    private String contentType;
    private Long fileSize;
    private String fileSizeFormatted;
    private String category;
    private boolean publicAccess;
    private String downloadUrl;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadedAt;

    // Constructors
    public FileUploadResponse() {}

    public FileUploadResponse(FileMetadata fileMetadata, String downloadUrl) {
        this.id = fileMetadata.getId();
        this.originalFileName = fileMetadata.getOriginalFileName();
        this.storedFileName = fileMetadata.getStoredFileName();
        this.contentType = fileMetadata.getContentType();
        this.fileSize = fileMetadata.getFileSize();
        this.fileSizeFormatted = fileMetadata.getFileSizeFormatted();
        this.category = fileMetadata.getCategory().getValue();
        this.publicAccess = fileMetadata.isPublicAccess();
        this.downloadUrl = downloadUrl;
        this.uploadedAt = fileMetadata.getCreatedDate();
    }

    // Static factory method
    public static FileUploadResponse from(FileMetadata fileMetadata, String downloadUrl) {
        return new FileUploadResponse(fileMetadata, downloadUrl);
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public void setPublicAccess(boolean publicAccess) {
        this.publicAccess = publicAccess;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    @Override
    public String toString() {
        return "FileUploadResponse{" +
                "id=" + id +
                ", originalFileName='" + originalFileName + '\'' +
                ", storedFileName='" + storedFileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", fileSize=" + fileSize +
                ", category='" + category + '\'' +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
}