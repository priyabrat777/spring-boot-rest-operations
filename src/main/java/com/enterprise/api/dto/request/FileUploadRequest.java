package com.enterprise.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for file upload operations.
 * Contains metadata that can be provided during file upload.
 * 
 * Requirements addressed:
 * - 2.1: File upload and management system
 */
public class FileUploadRequest {

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @NotNull(message = "Public access flag is required")
    private Boolean publicAccess = false;

    // Constructors
    public FileUploadRequest() {}

    public FileUploadRequest(String description, Boolean publicAccess) {
        this.description = description;
        this.publicAccess = publicAccess;
    }

    // Getters and Setters
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getPublicAccess() {
        return publicAccess;
    }

    public void setPublicAccess(Boolean publicAccess) {
        this.publicAccess = publicAccess;
    }

    @Override
    public String toString() {
        return "FileUploadRequest{" +
                "description='" + description + '\'' +
                ", publicAccess=" + publicAccess +
                '}';
    }
}