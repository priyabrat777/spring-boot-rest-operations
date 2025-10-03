package com.enterprise.api.documentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for validating OpenAPI specifications.
 * Provides methods to validate various aspects of the generated OpenAPI documentation.
 */
@Component
public class OpenApiSpecificationValidator {

    private final ObjectMapper objectMapper;

    public OpenApiSpecificationValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Validates the basic structure of an OpenAPI specification.
     *
     * @param openApiJson the OpenAPI specification as JSON string
     * @return validation result with any errors found
     */
    public ValidationResult validateBasicStructure(String openApiJson) {
        ValidationResult result = new ValidationResult();
        
        try {
            JsonNode spec = objectMapper.readTree(openApiJson);
            
            // Validate required root properties
            validateRequiredProperty(spec, "openapi", result);
            validateRequiredProperty(spec, "info", result);
            validateRequiredProperty(spec, "paths", result);
            
            // Validate OpenAPI version
            if (spec.has("openapi")) {
                String version = spec.get("openapi").asText();
                if (!version.startsWith("3.0") && !version.startsWith("3.1")) {
                    result.addError("Invalid OpenAPI version: " + version);
                }
            }
            
            // Validate info section
            if (spec.has("info")) {
                JsonNode info = spec.get("info");
                validateRequiredProperty(info, "title", result);
                validateRequiredProperty(info, "version", result);
            }
            
        } catch (Exception e) {
            result.addError("Failed to parse OpenAPI specification: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Validates security schemes in the OpenAPI specification.
     *
     * @param openApiJson the OpenAPI specification as JSON string
     * @return validation result with any errors found
     */
    public ValidationResult validateSecuritySchemes(String openApiJson) {
        ValidationResult result = new ValidationResult();
        
        try {
            JsonNode spec = objectMapper.readTree(openApiJson);
            
            if (!spec.has("components") || !spec.get("components").has("securitySchemes")) {
                result.addError("No security schemes defined");
                return result;
            }
            
            JsonNode securitySchemes = spec.get("components").get("securitySchemes");
            
            // Validate JWT Bearer scheme
            if (securitySchemes.has("bearerAuth")) {
                JsonNode bearerAuth = securitySchemes.get("bearerAuth");
                validateSecurityScheme(bearerAuth, "bearerAuth", "http", result);
                
                if (bearerAuth.has("scheme") && !bearerAuth.get("scheme").asText().equals("bearer")) {
                    result.addError("bearerAuth scheme should be 'bearer'");
                }
            } else {
                result.addError("Missing bearerAuth security scheme");
            }
            
            // Validate API Key scheme
            if (securitySchemes.has("apiKeyAuth")) {
                JsonNode apiKeyAuth = securitySchemes.get("apiKeyAuth");
                validateSecurityScheme(apiKeyAuth, "apiKeyAuth", "apiKey", result);
                
                if (apiKeyAuth.has("in") && !apiKeyAuth.get("in").asText().equals("header")) {
                    result.addError("apiKeyAuth should be in header");
                }
                
                if (apiKeyAuth.has("name") && !apiKeyAuth.get("name").asText().equals("X-API-Key")) {
                    result.addError("apiKeyAuth name should be 'X-API-Key'");
                }
            } else {
                result.addError("Missing apiKeyAuth security scheme");
            }
            
        } catch (Exception e) {
            result.addError("Failed to validate security schemes: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Validates that all expected endpoints are documented.
     *
     * @param openApiJson the OpenAPI specification as JSON string
     * @param expectedEndpoints list of expected endpoint paths
     * @return validation result with any errors found
     */
    public ValidationResult validateEndpoints(String openApiJson, List<String> expectedEndpoints) {
        ValidationResult result = new ValidationResult();
        
        try {
            JsonNode spec = objectMapper.readTree(openApiJson);
            
            if (!spec.has("paths")) {
                result.addError("No paths defined in specification");
                return result;
            }
            
            JsonNode paths = spec.get("paths");
            
            for (String expectedEndpoint : expectedEndpoints) {
                if (!paths.has(expectedEndpoint)) {
                    result.addError("Missing endpoint: " + expectedEndpoint);
                }
            }
            
        } catch (Exception e) {
            result.addError("Failed to validate endpoints: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Validates that endpoints have proper examples in their responses.
     *
     * @param openApiJson the OpenAPI specification as JSON string
     * @return validation result with any errors found
     */
    public ValidationResult validateExamples(String openApiJson) {
        ValidationResult result = new ValidationResult();
        
        try {
            JsonNode spec = objectMapper.readTree(openApiJson);
            
            if (!spec.has("paths")) {
                return result;
            }
            
            JsonNode paths = spec.get("paths");
            paths.fieldNames().forEachRemaining(pathName -> {
                JsonNode path = paths.get(pathName);
                path.fieldNames().forEachRemaining(method -> {
                    if (!method.equals("parameters")) {
                        JsonNode operation = path.get(method);
                        validateOperationExamples(operation, pathName, method, result);
                    }
                });
            });
            
        } catch (Exception e) {
            result.addError("Failed to validate examples: " + e.getMessage());
        }
        
        return result;
    }

    private void validateRequiredProperty(JsonNode node, String property, ValidationResult result) {
        if (!node.has(property)) {
            result.addError("Missing required property: " + property);
        }
    }

    private void validateSecurityScheme(JsonNode scheme, String schemeName, String expectedType, ValidationResult result) {
        if (!scheme.has("type")) {
            result.addError(schemeName + " missing type");
        } else if (!scheme.get("type").asText().equals(expectedType)) {
            result.addError(schemeName + " type should be '" + expectedType + "'");
        }
    }

    private void validateOperationExamples(JsonNode operation, String path, String method, ValidationResult result) {
        if (!operation.has("responses")) {
            return;
        }
        
        JsonNode responses = operation.get("responses");
        
        // Check if success responses (2xx) have examples
        responses.fieldNames().forEachRemaining(statusCode -> {
            if (statusCode.startsWith("2")) {
                JsonNode response = responses.get(statusCode);
                if (response.has("content")) {
                    JsonNode content = response.get("content");
                    content.fieldNames().forEachRemaining(mediaType -> {
                        JsonNode mediaTypeContent = content.get(mediaType);
                        if (!mediaTypeContent.has("examples") && !mediaTypeContent.has("example")) {
                            // Only warn for important endpoints
                            if (isImportantEndpoint(path, method)) {
                                result.addWarning("Missing examples for " + method.toUpperCase() + " " + path + " response " + statusCode);
                            }
                        }
                    });
                }
            }
        });
    }

    private boolean isImportantEndpoint(String path, String method) {
        // Consider authentication, user management, and file operations as important
        return path.contains("/auth/") || path.contains("/users") || path.contains("/files") || 
               (method.equals("post") && (path.contains("/login") || path.contains("/upload")));
    }

    /**
     * Result of OpenAPI specification validation.
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (!errors.isEmpty()) {
                sb.append("Errors:\n");
                errors.forEach(error -> sb.append("  - ").append(error).append("\n"));
            }
            if (!warnings.isEmpty()) {
                sb.append("Warnings:\n");
                warnings.forEach(warning -> sb.append("  - ").append(warning).append("\n"));
            }
            if (errors.isEmpty() && warnings.isEmpty()) {
                sb.append("Validation passed successfully");
            }
            return sb.toString();
        }
    }
}