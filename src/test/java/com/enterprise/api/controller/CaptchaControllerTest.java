package com.enterprise.api.controller;

import com.enterprise.api.dto.request.CaptchaValidationRequest;
import com.enterprise.api.dto.response.CaptchaResponse;
import com.enterprise.api.service.CaptchaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.awt.image.BufferedImage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CaptchaController.
 */
@WebMvcTest(CaptchaController.class)
class CaptchaControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CaptchaService captchaService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void generateCaptcha_ShouldReturnImageAndSessionId() throws Exception {
        // Given
        BufferedImage mockImage = new BufferedImage(200, 60, BufferedImage.TYPE_INT_RGB);
        when(captchaService.generateCaptcha(anyString())).thenReturn(mockImage);
        
        // When & Then
        mockMvc.perform(get("/api/captcha/generate"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Session-ID"))
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));
        
        verify(captchaService).generateCaptcha(anyString());
    }
    
    @Test
    void generateCaptchaJson_ShouldReturnSessionIdInJson() throws Exception {
        // Given
        BufferedImage mockImage = new BufferedImage(200, 60, BufferedImage.TYPE_INT_RGB);
        when(captchaService.generateCaptcha(anyString())).thenReturn(mockImage);
        
        // When & Then
        mockMvc.perform(post("/api/captcha/generate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.message").value("CAPTCHA session created successfully"));
        
        verify(captchaService).generateCaptcha(anyString());
    }
    
    @Test
    void getCaptchaImage_WithValidSession_ShouldReturnImage() throws Exception {
        // Given
        String sessionId = "test-session-id";
        BufferedImage mockImage = new BufferedImage(200, 60, BufferedImage.TYPE_INT_RGB);
        when(captchaService.getCaptchaText(sessionId)).thenReturn("ABCDE");
        when(captchaService.generateCaptcha(sessionId)).thenReturn(mockImage);
        
        // When & Then
        mockMvc.perform(get("/api/captcha/image/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"));
        
        verify(captchaService).getCaptchaText(sessionId);
        verify(captchaService).generateCaptcha(sessionId);
    }
    
    @Test
    void getCaptchaImage_WithInvalidSession_ShouldReturnNotFound() throws Exception {
        // Given
        String sessionId = "invalid-session-id";
        when(captchaService.getCaptchaText(sessionId)).thenReturn(null);
        
        // When & Then
        mockMvc.perform(get("/api/captcha/image/{sessionId}", sessionId))
                .andExpect(status().isNotFound());
        
        verify(captchaService).getCaptchaText(sessionId);
        verify(captchaService, never()).generateCaptcha(anyString());
    }
    
    @Test
    void validateCaptcha_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        CaptchaValidationRequest request = new CaptchaValidationRequest("test-session", "ABCDE");
        when(captchaService.validateCaptcha("test-session", "ABCDE")).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/captcha/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("test-session"))
                .andExpect(jsonPath("$.message").value("CAPTCHA validation successful"))
                .andExpect(jsonPath("$.valid").value(true));
        
        verify(captchaService).validateCaptcha("test-session", "ABCDE");
    }
    
    @Test
    void validateCaptcha_WithInvalidResponse_ShouldReturnBadRequest() throws Exception {
        // Given
        CaptchaValidationRequest request = new CaptchaValidationRequest("test-session", "WRONG");
        when(captchaService.validateCaptcha("test-session", "WRONG")).thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/captcha/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sessionId").value("test-session"))
                .andExpect(jsonPath("$.message").value("CAPTCHA validation failed"))
                .andExpect(jsonPath("$.valid").value(false));
        
        verify(captchaService).validateCaptcha("test-session", "WRONG");
    }
    
    @Test
    void validateCaptcha_WithEmptySessionId_ShouldReturnBadRequest() throws Exception {
        // Given
        CaptchaValidationRequest request = new CaptchaValidationRequest("", "ABCDE");
        
        // When & Then
        mockMvc.perform(post("/api/captcha/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        
        verify(captchaService, never()).validateCaptcha(anyString(), anyString());
    }
    
    @Test
    void validateCaptcha_WithEmptyResponse_ShouldReturnBadRequest() throws Exception {
        // Given
        CaptchaValidationRequest request = new CaptchaValidationRequest("test-session", "");
        
        // When & Then
        mockMvc.perform(post("/api/captcha/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        
        verify(captchaService, never()).validateCaptcha(anyString(), anyString());
    }
    
    @Test
    void validateCaptcha_WithNullSessionId_ShouldReturnBadRequest() throws Exception {
        // Given
        CaptchaValidationRequest request = new CaptchaValidationRequest(null, "ABCDE");
        
        // When & Then
        mockMvc.perform(post("/api/captcha/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        
        verify(captchaService, never()).validateCaptcha(anyString(), anyString());
    }
    
    @Test
    void validateCaptcha_WithNullResponse_ShouldReturnBadRequest() throws Exception {
        // Given
        CaptchaValidationRequest request = new CaptchaValidationRequest("test-session", null);
        
        // When & Then
        mockMvc.perform(post("/api/captcha/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        
        verify(captchaService, never()).validateCaptcha(anyString(), anyString());
    }
    
    @Test
    void validateCaptcha_WithTooLongResponse_ShouldReturnBadRequest() throws Exception {
        // Given
        CaptchaValidationRequest request = new CaptchaValidationRequest("test-session", "ABCDEFGHIJK"); // 11 chars
        
        // When & Then
        mockMvc.perform(post("/api/captcha/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        
        verify(captchaService, never()).validateCaptcha(anyString(), anyString());
    }
    
    @Test
    void clearCaptcha_ShouldReturnSuccess() throws Exception {
        // Given
        String sessionId = "test-session";
        doNothing().when(captchaService).clearCaptcha(sessionId);
        
        // When & Then
        mockMvc.perform(delete("/api/captcha/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.message").value("CAPTCHA session cleared successfully"));
        
        verify(captchaService).clearCaptcha(sessionId);
    }
    
    @Test
    void handleOptions_ShouldReturnAllowedMethods() throws Exception {
        // When & Then
        mockMvc.perform(options("/api/captcha"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "GET, POST, DELETE, OPTIONS"));
    }
    
    @Test
    void handleHead_ShouldReturnHeadersOnly() throws Exception {
        // When & Then
        mockMvc.perform(head("/api/captcha"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"));
    }
    
    @Test
    void validateCaptcha_WithMalformedJson_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/captcha/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
        
        verify(captchaService, never()).validateCaptcha(anyString(), anyString());
    }
    
    @Test
    void validateCaptcha_WithMissingContentType_ShouldReturnUnsupportedMediaType() throws Exception {
        // Given
        CaptchaValidationRequest request = new CaptchaValidationRequest("test-session", "ABCDE");
        
        // When & Then
        mockMvc.perform(post("/api/captcha/validate")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());
        
        verify(captchaService, never()).validateCaptcha(anyString(), anyString());
    }
}