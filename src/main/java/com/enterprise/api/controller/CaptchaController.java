package com.enterprise.api.controller;

import com.enterprise.api.dto.request.CaptchaValidationRequest;
import com.enterprise.api.dto.response.CaptchaResponse;
import com.enterprise.api.service.CaptchaService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * REST Controller for CAPTCHA operations.
 * Provides endpoints for generating visual challenges and validating responses.
 */
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {
    
    private final CaptchaService captchaService;
    
    @Autowired
    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }
    
    /**
     * Generates a new CAPTCHA image.
     * 
     * @param response HTTP response to write image data
     * @return ResponseEntity with session ID and success message
     */
    @GetMapping("/generate")
    public ResponseEntity<CaptchaResponse> generateCaptcha(HttpServletResponse response) {
        try {
            String sessionId = UUID.randomUUID().toString();
            BufferedImage captchaImage = captchaService.generateCaptcha(sessionId);
            
            // Convert image to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(captchaImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            
            // Set response headers
            response.setContentType(MediaType.IMAGE_PNG_VALUE);
            response.setContentLength(imageBytes.length);
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            response.setHeader("X-Session-ID", sessionId);
            
            // Write image to response
            response.getOutputStream().write(imageBytes);
            response.getOutputStream().flush();
            
            return ResponseEntity.ok(new CaptchaResponse(sessionId, "CAPTCHA generated successfully"));
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CaptchaResponse(null, "Failed to generate CAPTCHA: " + e.getMessage()));
        }
    }
    
    /**
     * Generates a new CAPTCHA and returns session ID in JSON format.
     * 
     * @return ResponseEntity with session ID
     */
    @PostMapping("/generate")
    public ResponseEntity<CaptchaResponse> generateCaptchaJson() {
        String sessionId = UUID.randomUUID().toString();
        captchaService.generateCaptcha(sessionId);
        
        return ResponseEntity.ok(new CaptchaResponse(sessionId, "CAPTCHA session created successfully"));
    }
    
    /**
     * Gets CAPTCHA image for a specific session.
     * 
     * @param sessionId the session identifier
     * @param response HTTP response to write image data
     * @return ResponseEntity with result
     */
    @GetMapping("/image/{sessionId}")
    public ResponseEntity<Void> getCaptchaImage(@PathVariable String sessionId, HttpServletResponse response) {
        try {
            // Check if session exists
            String captchaText = captchaService.getCaptchaText(sessionId);
            if (captchaText == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Regenerate image for existing session
            BufferedImage captchaImage = captchaService.generateCaptcha(sessionId);
            
            // Convert image to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(captchaImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            
            // Set response headers
            response.setContentType(MediaType.IMAGE_PNG_VALUE);
            response.setContentLength(imageBytes.length);
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            
            // Write image to response
            response.getOutputStream().write(imageBytes);
            response.getOutputStream().flush();
            
            return ResponseEntity.ok().build();
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Validates a CAPTCHA response.
     * 
     * @param request the validation request containing session ID and response
     * @return ResponseEntity with validation result
     */
    @PostMapping("/validate")
    public ResponseEntity<CaptchaResponse> validateCaptcha(@Valid @RequestBody CaptchaValidationRequest request) {
        boolean isValid = captchaService.validateCaptcha(request.getSessionId(), request.getResponse());
        
        String message = isValid ? "CAPTCHA validation successful" : "CAPTCHA validation failed";
        HttpStatus status = isValid ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        
        return ResponseEntity.status(status)
                .body(new CaptchaResponse(request.getSessionId(), message, isValid));
    }
    
    /**
     * Clears a CAPTCHA session.
     * 
     * @param sessionId the session identifier to clear
     * @return ResponseEntity with result
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<CaptchaResponse> clearCaptcha(@PathVariable String sessionId) {
        captchaService.clearCaptcha(sessionId);
        return ResponseEntity.ok(new CaptchaResponse(sessionId, "CAPTCHA session cleared successfully"));
    }
    
    /**
     * Handles OPTIONS requests for CORS support.
     * 
     * @return ResponseEntity with allowed methods
     */
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok()
                .header("Allow", "GET, POST, DELETE, OPTIONS")
                .build();
    }
    
    /**
     * Handles HEAD requests.
     * 
     * @return ResponseEntity with headers only
     */
    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<Void> handleHead() {
        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .build();
    }
}