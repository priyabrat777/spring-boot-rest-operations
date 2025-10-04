package com.enterprise.api.wiremock.stubs;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * WireMock stubs for external file storage services.
 * Provides mock responses for cloud storage operations like upload, download, and delete.
 */
public class FileStorageStubs {

    /**
     * Set up successful file upload stubs.
     */
    public static void setupFileUploadSuccess(WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/storage/upload"))
            .withHeader("Content-Type", containing("multipart/form-data"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "fileId": "file-123456789",
                        "fileName": "document.pdf",
                        "fileSize": 1048576,
                        "contentType": "application/pdf",
                        "uploadUrl": "https://storage.example.com/files/file-123456789",
                        "downloadUrl": "https://storage.example.com/download/file-123456789",
                        "checksum": "sha256:abcdef123456789",
                        "timestamp": "2024-01-01T12:00:00Z",
                        "expiresAt": "2024-01-08T12:00:00Z"
                    }
                    """)));
    }

    /**
     * Set up file upload failure due to size limit.
     */
    public static void setupFileUploadSizeLimit(WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/storage/upload"))
            .withHeader("Content-Type", containing("multipart/form-data"))
            .willReturn(aResponse()
                .withStatus(413)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "FILE_TOO_LARGE",
                        "message": "File size exceeds maximum allowed limit of 100MB",
                        "code": "UPLOAD_SIZE_EXCEEDED",
                        "maxSize": 104857600,
                        "timestamp": "2024-01-01T12:00:00Z"
                    }
                    """)));
    }

    /**
     * Set up file upload failure due to invalid file type.
     */
    public static void setupFileUploadInvalidType(WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/storage/upload"))
            .withHeader("Content-Type", containing("multipart/form-data"))
            .willReturn(aResponse()
                .withStatus(415)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "UNSUPPORTED_MEDIA_TYPE",
                        "message": "File type not allowed",
                        "code": "UPLOAD_TYPE_NOT_ALLOWED",
                        "allowedTypes": ["image/jpeg", "image/png", "application/pdf"],
                        "timestamp": "2024-01-01T12:00:00Z"
                    }
                    """)));
    }

    /**
     * Set up file upload failure due to storage quota exceeded.
     */
    public static void setupFileUploadQuotaExceeded(WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/storage/upload"))
            .withHeader("Content-Type", containing("multipart/form-data"))
            .willReturn(aResponse()
                .withStatus(507)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "INSUFFICIENT_STORAGE",
                        "message": "Storage quota exceeded",
                        "code": "QUOTA_EXCEEDED",
                        "quotaUsed": 5368709120,
                        "quotaLimit": 5368709120,
                        "timestamp": "2024-01-01T12:00:00Z"
                    }
                    """)));
    }

    /**
     * Set up successful file download stubs.
     */
    public static void setupFileDownloadSuccess(WireMockServer wireMockServer) {
        wireMockServer.stubFor(get(urlPathMatching("/api/storage/download/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/pdf")
                .withHeader("Content-Disposition", "attachment; filename=\"document.pdf\"")
                .withHeader("Content-Length", "1048576")
                .withHeader("ETag", "\"abcdef123456789\"")
                .withBody("Mock PDF file content")));
    }

    /**
     * Set up file download failure - file not found.
     */
    public static void setupFileDownloadNotFound(WireMockServer wireMockServer) {
        wireMockServer.stubFor(get(urlPathMatching("/api/storage/download/.*"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "FILE_NOT_FOUND",
                        "message": "The requested file was not found",
                        "code": "DOWNLOAD_FILE_NOT_FOUND",
                        "timestamp": "2024-01-01T12:00:00Z"
                    }
                    """)));
    }

    /**
     * Set up file download failure - access denied.
     */
    public static void setupFileDownloadAccessDenied(WireMockServer wireMockServer) {
        wireMockServer.stubFor(get(urlPathMatching("/api/storage/download/.*"))
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "ACCESS_DENIED",
                        "message": "You do not have permission to access this file",
                        "code": "DOWNLOAD_ACCESS_DENIED",
                        "timestamp": "2024-01-01T12:00:00Z"
                    }
                    """)));
    }

    /**
     * Set up successful file deletion stubs.
     */
    public static void setupFileDeleteSuccess(WireMockServer wireMockServer) {
        wireMockServer.stubFor(delete(urlPathMatching("/api/storage/files/.*"))
            .willReturn(aResponse()
                .withStatus(204)
                .withHeader("Content-Type", "application/json")));
    }

    /**
     * Set up file deletion failure - file not found.
     */
    public static void setupFileDeleteNotFound(WireMockServer wireMockServer) {
        wireMockServer.stubFor(delete(urlPathMatching("/api/storage/files/.*"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "FILE_NOT_FOUND",
                        "message": "The file to delete was not found",
                        "code": "DELETE_FILE_NOT_FOUND",
                        "timestamp": "2024-01-01T12:00:00Z"
                    }
                    """)));
    }

    /**
     * Set up file metadata retrieval success.
     */
    public static void setupFileMetadataSuccess(WireMockServer wireMockServer) {
        wireMockServer.stubFor(get(urlPathMatching("/api/storage/metadata/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "fileId": "file-123456789",
                        "fileName": "document.pdf",
                        "fileSize": 1048576,
                        "contentType": "application/pdf",
                        "checksum": "sha256:abcdef123456789",
                        "createdAt": "2024-01-01T12:00:00Z",
                        "modifiedAt": "2024-01-01T12:00:00Z",
                        "downloadCount": 5,
                        "tags": ["document", "important"],
                        "isPublic": false
                    }
                    """)));
    }

    /**
     * Set up storage service unavailable.
     */
    public static void setupStorageServiceUnavailable(WireMockServer wireMockServer) {
        wireMockServer.stubFor(any(urlPathMatching("/api/storage/.*"))
            .willReturn(aResponse()
                .withStatus(503)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "SERVICE_UNAVAILABLE",
                        "message": "Storage service is temporarily unavailable",
                        "code": "STORAGE_SERVICE_DOWN",
                        "retryAfter": 300,
                        "timestamp": "2024-01-01T12:00:00Z"
                    }
                    """)));
    }

    /**
     * Set up storage service timeout.
     */
    public static void setupStorageServiceTimeout(WireMockServer wireMockServer) {
        wireMockServer.stubFor(any(urlPathMatching("/api/storage/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(30000) // 30 second delay
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\": \"timeout\"}")));
    }

    /**
     * Verify file upload was called with correct parameters.
     */
    public static void verifyFileUploadCall(WireMockServer wireMockServer, String fileName) {
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/storage/upload"))
            .withHeader("Content-Type", containing("multipart/form-data")));
    }

    /**
     * Verify file download was called.
     */
    public static void verifyFileDownloadCall(WireMockServer wireMockServer, String fileId) {
        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/api/storage/download/" + fileId)));
    }

    /**
     * Verify file deletion was called.
     */
    public static void verifyFileDeleteCall(WireMockServer wireMockServer, String fileId) {
        wireMockServer.verify(deleteRequestedFor(urlPathEqualTo("/api/storage/files/" + fileId)));
    }

    /**
     * Get the count of upload requests.
     */
    public static int getUploadRequestCount(WireMockServer wireMockServer) {
        return wireMockServer.countRequestsMatching(
            postRequestedFor(urlPathEqualTo("/api/storage/upload")).build()
        ).getCount();
    }

    /**
     * Get the count of download requests.
     */
    public static int getDownloadRequestCount(WireMockServer wireMockServer) {
        return wireMockServer.countRequestsMatching(
            getRequestedFor(urlPathMatching("/api/storage/download/.*")).build()
        ).getCount();
    }
}