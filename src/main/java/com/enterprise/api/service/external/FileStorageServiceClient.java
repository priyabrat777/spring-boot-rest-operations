package com.enterprise.api.service.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Client for external file storage service integration.
 * Handles file operations through external cloud storage providers.
 */
@Service
public class FileStorageServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${app.external.file-storage.enabled:false}")
    private boolean fileStorageServiceEnabled;

    public FileStorageServiceClient(@Qualifier("fileStorageServiceRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean uploadFile(MultipartFile file, String storedFileName) {
        if (!fileStorageServiceEnabled) {
            logger.info("File storage service disabled, simulating file upload: {}", file.getOriginalFilename());
            return true;
        }

        try {
            logger.info("Uploading file: {} to external storage", file.getOriginalFilename());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("storedFileName", storedFileName);
            body.add("originalFileName", file.getOriginalFilename());
            body.add("contentType", file.getContentType());

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity("/api/storage/upload", request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("File uploaded successfully: {}", file.getOriginalFilename());
                return true;
            } else {
                logger.error("Failed to upload file: {}. Status: {}", file.getOriginalFilename(), response.getStatusCode());
                return false;
            }

        } catch (IOException ex) {
            logger.error("Error reading file: {}", file.getOriginalFilename(), ex);
            return false;
        } catch (Exception ex) {
            logger.error("Error uploading file: {}", file.getOriginalFilename(), ex);
            return false;
        }
    }

    public Resource downloadFile(String storedFileName) {
        if (!fileStorageServiceEnabled) {
            logger.info("File storage service disabled, simulating file download: {}", storedFileName);
            return null;
        }

        try {
            logger.info("Downloading file: {} from external storage", storedFileName);

            ResponseEntity<Resource> response = restTemplate.getForEntity(
                "/api/storage/download/" + storedFileName, Resource.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("File downloaded successfully: {}", storedFileName);
                return response.getBody();
            } else {
                logger.error("Failed to download file: {}. Status: {}", storedFileName, response.getStatusCode());
                return null;
            }

        } catch (Exception ex) {
            logger.error("Error downloading file: {}", storedFileName, ex);
            return null;
        }
    }

    public boolean deleteFile(String storedFileName) {
        if (!fileStorageServiceEnabled) {
            logger.info("File storage service disabled, simulating file deletion: {}", storedFileName);
            return true;
        }

        try {
            logger.info("Deleting file: {} from external storage", storedFileName);

            restTemplate.delete("/api/storage/files/" + storedFileName);
            logger.info("File deleted successfully: {}", storedFileName);
            return true;

        } catch (Exception ex) {
            logger.error("Error deleting file: {}", storedFileName, ex);
            return false;
        }
    }
}