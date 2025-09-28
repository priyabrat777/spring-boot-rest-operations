package com.enterprise.api.controller;

import com.enterprise.api.dto.request.CaptchaValidationRequest;
import com.enterprise.api.dto.response.CaptchaResponse;
import com.enterprise.api.service.CaptchaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CaptchaController.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CaptchaControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private CaptchaService captchaService;
    
    @Test
    void captchaWorkflow_GenerateAndValidate_ShouldWork() {
        // Step 1: Generate CAPTCHA session
        ResponseEntity<CaptchaResponse> generateResponse = restTemplate.postForEntity(
                "/api/captcha/generate", null, CaptchaResponse.class);
        
        assertEquals(HttpStatus.OK, generateResponse.getStatusCode());
        assertNotNull(generateResponse.getBody());
        String sessionId = generateResponse.getBody().getSessionId();
        assertNotNull(sessionId);
        assertEquals("CAPTCHA session created successfully", generateResponse.getBody().getMessage());
        
        // Step 2: Get the CAPTCHA text (this would normally be done by looking at the image)
        String captchaText = captchaService.getCaptchaText(sessionId);
        assertNotNull(captchaText);
        assertEquals(5, captchaText.length());
        
        // Step 3: Validate with correct response
        CaptchaValidationRequest validRequest = new CaptchaValidationRequest(sessionId, captchaText);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CaptchaValidationRequest> requestEntity = new HttpEntity<>(validRequest, headers);
        
        ResponseEntity<CaptchaResponse> validateResponse = restTemplate.postForEntity(
                "/api/captcha/validate", requestEntity, CaptchaResponse.class);
        
        assertEquals(HttpStatus.OK, validateResponse.getStatusCode());
        assertNotNull(validateResponse.getBody());
        assertEquals(sessionId, validateResponse.getBody().getSessionId());
        assertEquals("CAPTCHA validation successful", validateResponse.getBody().getMessage());
        assertTrue(validateResponse.getBody().isValid());
        
        // Step 4: Verify CAPTCHA is removed after validation
        assertNull(captchaService.getCaptchaText(sessionId));
    }
    
    @Test
    void captchaWorkflow_GenerateAndValidateWithWrongResponse_ShouldFail() {
        // Step 1: Generate CAPTCHA session
        ResponseEntity<CaptchaResponse> generateResponse = restTemplate.postForEntity(
                "/api/captcha/generate", null, CaptchaResponse.class);
        
        assertEquals(HttpStatus.OK, generateResponse.getStatusCode());
        assertNotNull(generateResponse.getBody());
        String sessionId = generateResponse.getBody().getSessionId();
        
        // Step 2: Validate with incorrect response
        CaptchaValidationRequest invalidRequest = new CaptchaValidationRequest(sessionId, "WRONG");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CaptchaValidationRequest> requestEntity = new HttpEntity<>(invalidRequest, headers);
        
        ResponseEntity<CaptchaResponse> validateResponse = restTemplate.postForEntity(
                "/api/captcha/validate", requestEntity, CaptchaResponse.class);
        
        assertEquals(HttpStatus.BAD_REQUEST, validateResponse.getStatusCode());
        assertNotNull(validateResponse.getBody());
        assertEquals(sessionId, validateResponse.getBody().getSessionId());
        assertEquals("CAPTCHA validation failed", validateResponse.getBody().getMessage());
        assertFalse(validateResponse.getBody().isValid());
        
        // Step 3: Verify CAPTCHA is removed after failed validation
        assertNull(captchaService.getCaptchaText(sessionId));
    }
    
    @Test
    void clearCaptcha_ShouldRemoveSession() {
        // Step 1: Generate CAPTCHA session
        ResponseEntity<CaptchaResponse> generateResponse = restTemplate.postForEntity(
                "/api/captcha/generate", null, CaptchaResponse.class);
        
        assertEquals(HttpStatus.OK, generateResponse.getStatusCode());
        String sessionId = generateResponse.getBody().getSessionId();
        
        // Verify session exists
        assertNotNull(captchaService.getCaptchaText(sessionId));
        
        // Step 2: Clear CAPTCHA session
        restTemplate.delete("/api/captcha/{sessionId}", sessionId);
        
        // Step 3: Verify session is removed
        assertNull(captchaService.getCaptchaText(sessionId));
    }
    
    @Test
    void validateCaptcha_CaseInsensitive_ShouldWork() {
        // Step 1: Generate CAPTCHA session
        ResponseEntity<CaptchaResponse> generateResponse = restTemplate.postForEntity(
                "/api/captcha/generate", null, CaptchaResponse.class);
        
        assertEquals(HttpStatus.OK, generateResponse.getStatusCode());
        String sessionId = generateResponse.getBody().getSessionId();
        
        // Step 2: Get CAPTCHA text and convert to lowercase
        String captchaText = captchaService.getCaptchaText(sessionId);
        String lowercaseResponse = captchaText.toLowerCase();
        
        // Step 3: Validate with lowercase response
        CaptchaValidationRequest request = new CaptchaValidationRequest(sessionId, lowercaseResponse);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CaptchaValidationRequest> requestEntity = new HttpEntity<>(request, headers);
        
        ResponseEntity<CaptchaResponse> validateResponse = restTemplate.postForEntity(
                "/api/captcha/validate", requestEntity, CaptchaResponse.class);
        
        assertEquals(HttpStatus.OK, validateResponse.getStatusCode());
        assertTrue(validateResponse.getBody().isValid());
    }
    
    @Test
    void validateCaptcha_WithWhitespace_ShouldTrimAndValidate() {
        // Step 1: Generate CAPTCHA session
        ResponseEntity<CaptchaResponse> generateResponse = restTemplate.postForEntity(
                "/api/captcha/generate", null, CaptchaResponse.class);
        
        assertEquals(HttpStatus.OK, generateResponse.getStatusCode());
        String sessionId = generateResponse.getBody().getSessionId();
        
        // Step 2: Get CAPTCHA text and add whitespace
        String captchaText = captchaService.getCaptchaText(sessionId);
        String responseWithWhitespace = "  " + captchaText + "  ";
        
        // Step 3: Validate with whitespace response
        CaptchaValidationRequest request = new CaptchaValidationRequest(sessionId, responseWithWhitespace);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CaptchaValidationRequest> requestEntity = new HttpEntity<>(request, headers);
        
        ResponseEntity<CaptchaResponse> validateResponse = restTemplate.postForEntity(
                "/api/captcha/validate", requestEntity, CaptchaResponse.class);
        
        assertEquals(HttpStatus.OK, validateResponse.getStatusCode());
        assertTrue(validateResponse.getBody().isValid());
    }
    
    @Test
    void validateCaptcha_NonExistentSession_ShouldFail() {
        // Given
        CaptchaValidationRequest request = new CaptchaValidationRequest("non-existent-session", "ABCDE");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CaptchaValidationRequest> requestEntity = new HttpEntity<>(request, headers);
        
        // When & Then
        ResponseEntity<CaptchaResponse> validateResponse = restTemplate.postForEntity(
                "/api/captcha/validate", requestEntity, CaptchaResponse.class);
        
        assertEquals(HttpStatus.BAD_REQUEST, validateResponse.getStatusCode());
        assertFalse(validateResponse.getBody().isValid());
    }
    
    @Test
    void multipleCaptchaSessions_ShouldWorkIndependently() {
        // Step 1: Generate two CAPTCHA sessions
        ResponseEntity<CaptchaResponse> response1 = restTemplate.postForEntity(
                "/api/captcha/generate", null, CaptchaResponse.class);
        ResponseEntity<CaptchaResponse> response2 = restTemplate.postForEntity(
                "/api/captcha/generate", null, CaptchaResponse.class);
        
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        
        String sessionId1 = response1.getBody().getSessionId();
        String sessionId2 = response2.getBody().getSessionId();
        
        // Step 2: Verify both sessions exist and are different
        String captcha1 = captchaService.getCaptchaText(sessionId1);
        String captcha2 = captchaService.getCaptchaText(sessionId2);
        
        assertNotNull(captcha1);
        assertNotNull(captcha2);
        assertNotEquals(sessionId1, sessionId2);
        
        // Step 3: Validate first session
        CaptchaValidationRequest request1 = new CaptchaValidationRequest(sessionId1, captcha1);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CaptchaValidationRequest> requestEntity1 = new HttpEntity<>(request1, headers);
        
        ResponseEntity<CaptchaResponse> validateResponse1 = restTemplate.postForEntity(
                "/api/captcha/validate", requestEntity1, CaptchaResponse.class);
        
        assertEquals(HttpStatus.OK, validateResponse1.getStatusCode());
        assertTrue(validateResponse1.getBody().isValid());
        
        // Step 4: Verify first session is removed but second still exists
        assertNull(captchaService.getCaptchaText(sessionId1));
        assertNotNull(captchaService.getCaptchaText(sessionId2));
        
        // Step 5: Validate second session
        CaptchaValidationRequest request2 = new CaptchaValidationRequest(sessionId2, captcha2);
        HttpEntity<CaptchaValidationRequest> requestEntity2 = new HttpEntity<>(request2, headers);
        
        ResponseEntity<CaptchaResponse> validateResponse2 = restTemplate.postForEntity(
                "/api/captcha/validate", requestEntity2, CaptchaResponse.class);
        
        assertEquals(HttpStatus.OK, validateResponse2.getStatusCode());
        assertTrue(validateResponse2.getBody().isValid());
        
        // Step 6: Verify both sessions are now removed
        assertNull(captchaService.getCaptchaText(sessionId1));
        assertNull(captchaService.getCaptchaText(sessionId2));
    }
}