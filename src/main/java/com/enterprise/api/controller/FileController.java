package com.enterprise.api.controller;

import com.enterprise.api.dto.request.FileUploadRequest;
import com.enterprise.api.dto.response.ErrorResponse;
import com.enterprise.api.dto.response.FileMetadataResponse;
import com.enterprise.api.dto.response.FileUploadResponse;
import com.enterprise.api.entity.FileMetadata;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.UserRepository;
import com.enterprise.api.security.CustomUserPrincipal;
import com.enterprise.api.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * REST Controller for file management operations.
 * Provides endpoints for file upload, download, deletion, and metadata management.
 * 
 * Requirements addressed:
 * - 2.1: File upload and management system
 * - 1.1-1.8: Complete REST API operations (GET, POST, DELETE, HEAD, OPTIONS)
 * - 4.3: RBAC authorization for file operations
 */
@Tag(name = "File Management", description = "File upload, download, and metadata management operations with security validation")
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;
    private final UserRepository userRepository;

    public FileController(FileService fileService, UserRepository userRepository) {
        this.fileService = fileService;
        this.userRepository = userRepository;
    }

    /**
     * Helper method to get User entity from CustomUserPrincipal.
     */
    private User getUserFromPrincipal(CustomUserPrincipal principal) {
        return userRepository.findByUsernameActive(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + principal.getUsername()));
    }

    /**
     * Upload a file with metadata.
     * POST /api/files/upload
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "publicAccess", required = false, defaultValue = "false") Boolean publicAccess,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        
        logger.info("File upload request from user: {}", principal.getUsername());

        User user = getUserFromPrincipal(principal);
        
        FileUploadRequest request = new FileUploadRequest(description, publicAccess);
        FileUploadResponse response = fileService.uploadFile(file, request, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/files/" + response.getId())
                .body(response);
    }

    /**
     * Download a file by stored file name.
     * GET /api/files/download/{storedFileName}
     */
    @GetMapping("/download/{storedFileName}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String storedFileName,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        
        logger.info("File download request: {} from user: {}", storedFileName, 
                   principal != null ? principal.getUsername() : "anonymous");

        User user = principal != null ? getUserFromPrincipal(principal) : null;
        Resource resource = fileService.downloadFile(storedFileName, user);
        
        // Get file metadata for headers
        FileMetadataResponse metadata = fileService.getFileMetadataByStoredName(storedFileName, user);
        
        String encodedFileName = URLEncoder.encode(metadata.getOriginalFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + encodedFileName + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, metadata.getFileSize().toString())
                .body(resource);
    }

    /**
     * Download a file by ID.
     * GET /api/files/{fileId}/download
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFileById(
            @PathVariable Long fileId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        
        logger.info("File download request by ID: {} from user: {}", fileId, 
                   principal != null ? principal.getUsername() : "anonymous");

        User user = principal != null ? getUserFromPrincipal(principal) : null;
        Resource resource = fileService.downloadFileById(fileId, user);
        
        // Get file metadata for headers
        FileMetadataResponse metadata = fileService.getFileMetadata(fileId, user);
        
        String encodedFileName = URLEncoder.encode(metadata.getOriginalFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + encodedFileName + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, metadata.getFileSize().toString())
                .body(resource);
    }

    /**
     * Get file metadata by ID.
     * GET /api/files/{fileId}
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<FileMetadataResponse> getFileMetadata(
            @PathVariable Long fileId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        
        User user = principal != null ? getUserFromPrincipal(principal) : null;
        FileMetadataResponse response = fileService.getFileMetadata(fileId, user);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a file by ID.
     * DELETE /api/files/{fileId}
     */
    @DeleteMapping("/{fileId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        
        logger.info("File deletion request: {} from user: {}", fileId, principal.getUsername());

        User user = getUserFromPrincipal(principal);
        fileService.deleteFile(fileId, user);

        return ResponseEntity.noContent().build();
    }

    /**
     * List files uploaded by the current user.
     * GET /api/files/my-files
     */
    @GetMapping("/my-files")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<FileMetadataResponse>> listMyFiles(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        
        User user = getUserFromPrincipal(principal);
        Page<FileMetadataResponse> files = fileService.listUserFiles(user, pageable);
        
        return ResponseEntity.ok(files);
    }

    /**
     * List public files accessible to all users.
     * GET /api/files/public
     */
    @GetMapping("/public")
    public ResponseEntity<Page<FileMetadataResponse>> listPublicFiles(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<FileMetadataResponse> files = fileService.listPublicFiles(pageable);
        return ResponseEntity.ok(files);
    }

    /**
     * Search files by name.
     * GET /api/files/search?q={query}
     */
    @GetMapping("/search")
    public ResponseEntity<Page<FileMetadataResponse>> searchFiles(
            @RequestParam("q") String query,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        
        User user = principal != null ? getUserFromPrincipal(principal) : null;
        Page<FileMetadataResponse> files = fileService.searchFilesByName(query, user, pageable);
        
        return ResponseEntity.ok(files);
    }

    /**
     * List files by category.
     * GET /api/files/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<FileMetadataResponse>> listFilesByCategory(
            @PathVariable String category,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        
        try {
            FileMetadata.FileCategory fileCategory = FileMetadata.FileCategory.valueOf(category.toUpperCase());
            User user = principal != null ? getUserFromPrincipal(principal) : null;
            Page<FileMetadataResponse> files = fileService.listFilesByCategory(fileCategory, user, pageable);
            
            return ResponseEntity.ok(files);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get user file statistics.
     * GET /api/files/statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FileService.FileStatistics> getUserFileStatistics(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        
        User user = getUserFromPrincipal(principal);
        FileService.FileStatistics statistics = fileService.getUserFileStatistics(user);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * HEAD method for file download - returns headers without body.
     * HEAD /api/files/download/{storedFileName}
     */
    @RequestMapping(value = "/download/{storedFileName}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> getFileHeaders(
            @PathVariable String storedFileName,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        
        User user = principal != null ? getUserFromPrincipal(principal) : null;
        FileMetadataResponse metadata = fileService.getFileMetadataByStoredName(storedFileName, user);
        
        String encodedFileName = URLEncoder.encode(metadata.getOriginalFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + encodedFileName + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, metadata.getFileSize().toString())
                .build();
    }

    /**
     * OPTIONS method for CORS preflight requests.
     * OPTIONS /api/files/**
     */
    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok()
                .header(HttpHeaders.ALLOW, "GET, POST, DELETE, HEAD, OPTIONS")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, DELETE, HEAD, OPTIONS")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization")
                .build();
    }

    /**
     * Exception handler for file-related exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        logger.error("File operation error: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurityException(SecurityException ex) {
        logger.error("File access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        logger.error("File operation failed: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("File operation failed: " + ex.getMessage());
    }
}