package com.enterprise.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Where;

/**
 * FileMetadata entity for tracking uploaded files and their metadata.
 * Extends AuditableEntity to track creation, modification, and soft deletion.
 * 
 * Requirements addressed:
 * - 2.1: File upload and management system
 * - 3.1: Audit trail for file operations
 */
@Entity
@Table(name = "file_metadata", indexes = {
    @Index(name = "idx_file_original_name", columnList = "original_file_name"),
    @Index(name = "idx_file_stored_name", columnList = "stored_file_name"),
    @Index(name = "idx_file_content_type", columnList = "content_type"),
    @Index(name = "idx_file_uploaded_by", columnList = "uploaded_by_id"),
    @Index(name = "idx_file_deleted", columnList = "deleted")
})
@Where(clause = "deleted = false")
public class FileMetadata extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Original file name is required")
    @Size(max = 255, message = "Original file name must not exceed 255 characters")
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @NotBlank(message = "Stored file name is required")
    @Size(max = 255, message = "Stored file name must not exceed 255 characters")
    @Column(name = "stored_file_name", nullable = false, unique = true)
    private String storedFileName;

    @NotBlank(message = "Content type is required")
    @Size(max = 100, message = "Content type must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9!#$&\\-\\^_]*\\/[a-zA-Z0-9][a-zA-Z0-9!#$&\\-\\^_.]*$", 
             message = "Content type must be a valid MIME type")
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @NotNull(message = "File size is required")
    @Min(value = 0, message = "File size must be non-negative")
    @Max(value = 104857600, message = "File size must not exceed 100MB") // 100MB limit
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @NotBlank(message = "File path is required")
    @Size(max = 500, message = "File path must not exceed 500 characters")
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Size(max = 64, message = "Checksum must not exceed 64 characters")
    @Pattern(regexp = "^[a-fA-F0-9]*$", message = "Checksum must contain only hexadecimal characters")
    @Column(name = "checksum", length = 64)
    private String checksum;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Column(name = "description")
    private String description;

    @Column(name = "download_count", nullable = false)
    private Long downloadCount = 0L;

    @Column(name = "public_access", nullable = false)
    private boolean publicAccess = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", foreignKey = @ForeignKey(name = "fk_file_metadata_user"))
    private User uploadedBy;

    // File categories enum
    public enum FileCategory {
        DOCUMENT("document"),
        IMAGE("image"),
        VIDEO("video"),
        AUDIO("audio"),
        ARCHIVE("archive"),
        OTHER("other");

        private final String value;

        FileCategory(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static FileCategory fromContentType(String contentType) {
            if (contentType == null) return OTHER;
            
            String type = contentType.toLowerCase();
            if (type.startsWith("image/")) return IMAGE;
            if (type.startsWith("video/")) return VIDEO;
            if (type.startsWith("audio/")) return AUDIO;
            if (type.startsWith("application/pdf") || 
                type.startsWith("application/msword") ||
                type.startsWith("application/vnd.openxmlformats-officedocument") ||
                type.startsWith("text/")) return DOCUMENT;
            if (type.startsWith("application/zip") ||
                type.startsWith("application/x-rar") ||
                type.startsWith("application/x-7z")) return ARCHIVE;
            
            return OTHER;
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private FileCategory category = FileCategory.OTHER;

    // Constructors
    public FileMetadata() {
        super();
    }

    public FileMetadata(String originalFileName, String storedFileName, String contentType, Long fileSize, String filePath) {
        this();
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.category = FileCategory.fromContentType(contentType);
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
        // Update category when content type changes
        if (contentType != null) {
            this.category = FileCategory.fromContentType(contentType);
        }
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public FileCategory getCategory() {
        return category;
    }

    public void setCategory(FileCategory category) {
        this.category = category;
    }

    // Helper methods
    public void incrementDownloadCount() {
        this.downloadCount = (this.downloadCount != null ? this.downloadCount : 0L) + 1L;
    }

    public String getFileExtension() {
        if (originalFileName != null && originalFileName.contains(".")) {
            return originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }

    public String getFileSizeFormatted() {
        if (fileSize == null) return "0 B";
        
        long size = fileSize;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return size + " " + units[unitIndex];
    }

    public boolean isImage() {
        return category == FileCategory.IMAGE;
    }

    public boolean isDocument() {
        return category == FileCategory.DOCUMENT;
    }

    public boolean isVideo() {
        return category == FileCategory.VIDEO;
    }

    public boolean isAudio() {
        return category == FileCategory.AUDIO;
    }

    public boolean isArchive() {
        return category == FileCategory.ARCHIVE;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        FileMetadata that = (FileMetadata) obj;
        
        // Use ID for equality if both entities are persisted
        if (id != null && that.id != null) {
            return id.equals(that.id);
        }
        
        // Use stored file name for equality if entities are not persisted
        return storedFileName != null && storedFileName.equals(that.storedFileName);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : (storedFileName != null ? storedFileName.hashCode() : 0);
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "id=" + id +
                ", originalFileName='" + originalFileName + '\'' +
                ", storedFileName='" + storedFileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", fileSize=" + fileSize +
                ", category=" + category +
                ", downloadCount=" + downloadCount +
                ", publicAccess=" + publicAccess +
                ", uploadedBy=" + (uploadedBy != null ? uploadedBy.getUsername() : null) +
                "} " + super.toString();
    }
}