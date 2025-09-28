package com.enterprise.api.controller;

import com.enterprise.api.dto.request.LoginRequest;
import com.enterprise.api.dto.request.RefreshTokenRequest;
import com.enterprise.api.dto.response.AuthResponse;
import com.enterprise.api.dto.response.TokenResponse;
import com.enterprise.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for authentication operations.
 * Handles login, logout, and token refresh endpoints.
 * 
 * Requirements addressed:
 * - 1.1: GET operations for authentication status
 * - 1.2: POST operations for login and token refresh
 * - 1.3: PUT operations for logout
 * - 1.4: Proper HTTP status codes and response handling
 * - 4.4: JWT-based authentication endpoints
 * - 4.5: Authentication error handling
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates a user and returns JWT tokens.
     * 
     * @param loginRequest the login credentials
     * @return authentication response with tokens and user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Login attempt for user: {}", loginRequest.getUsernameOrEmail());
        
        try {
            AuthResponse authResponse = authService.login(loginRequest);
            logger.info("Login successful for user: {}", loginRequest.getUsernameOrEmail());
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            logger.error("Login failed for user: {}", loginRequest.getUsernameOrEmail(), e);
            throw e;
        }
    }

    /**
     * Logs out the current user by invalidating tokens.
     * 
     * @param authentication the current authentication
     * @return success message
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        logger.info("Logout request for user: {}", authentication.getName());
        
        try {
            String message = authService.logout(authentication);
            logger.info("Logout successful for user: {}", authentication.getName());
            return ResponseEntity.ok(Map.of("message", message));
        } catch (Exception e) {
            logger.error("Logout failed for user: {}", authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Refreshes an access token using a valid refresh token.
     * 
     * @param refreshTokenRequest the refresh token request
     * @return new token response
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        logger.info("Token refresh request received");
        
        try {
            TokenResponse tokenResponse = authService.refreshToken(refreshTokenRequest);
            logger.info("Token refresh successful");
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            throw e;
        }
    }

    /**
     * Validates the current authentication status.
     * 
     * @param authentication the current authentication
     * @return authentication status
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(Authentication authentication) {
        logger.debug("Token validation request for user: {}", authentication.getName());
        
        try {
            boolean isValid = authService.validateAuthentication(authentication);
            Long userId = authService.getCurrentUserId(authentication);
            String username = authService.getCurrentUsername(authentication);
            
            Map<String, Object> response = Map.of(
                "valid", isValid,
                "userId", userId,
                "username", username,
                "authenticated", authentication.isAuthenticated()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Token validation failed for user: {}", authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "Invalid token"));
        }
    }

    /**
     * Gets the current user's authentication information.
     * 
     * @param authentication the current authentication
     * @return current user info
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        logger.debug("Current user info request for: {}", authentication.getName());
        
        try {
            Long userId = authService.getCurrentUserId(authentication);
            String username = authService.getCurrentUsername(authentication);
            
            Map<String, Object> userInfo = Map.of(
                "id", userId,
                "username", username,
                "authorities", authentication.getAuthorities(),
                "authenticated", authentication.isAuthenticated()
            );
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            logger.error("Failed to get current user info for: {}", authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Updates user password using current authentication.
     * 
     * @param passwordRequest the password change request
     * @param authentication the current authentication
     * @return success message
     */
    @PatchMapping("/password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @RequestBody Map<String, String> passwordRequest,
            Authentication authentication) {
        
        logger.info("Password change request for user: {}", authentication.getName());
        
        try {
            String currentPassword = passwordRequest.get("currentPassword");
            String newPassword = passwordRequest.get("newPassword");
            
            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Current password and new password are required"
                ));
            }
            
            boolean success = authService.changeCurrentUserPassword(currentPassword, newPassword, authentication);
            if (success) {
                logger.info("Password changed successfully for user: {}", authentication.getName());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password changed successfully"
                ));
            } else {
                logger.warn("Failed to change password for user: {}", authentication.getName());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to change password. Current password may be incorrect."
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to change password for user: {}", authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Updates user profile information partially.
     * 
     * @param profileRequest the profile update request
     * @param authentication the current authentication
     * @return success message
     */
    @PatchMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody Map<String, Object> profileRequest,
            Authentication authentication) {
        
        logger.info("Profile update request for user: {}", authentication.getName());
        
        try {
            boolean success = authService.updateCurrentUserProfile(profileRequest, authentication);
            if (success) {
                logger.info("Profile updated successfully for user: {}", authentication.getName());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile updated successfully"
                ));
            } else {
                logger.warn("Failed to update profile for user: {}", authentication.getName());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to update profile"
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to update profile for user: {}", authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Handles OPTIONS requests for CORS preflight and method discovery.
     * 
     * @param request the HTTP request
     * @return allowed methods and CORS headers
     */
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        // Determine allowed methods based on endpoint
        String allowedMethods;
        if (requestURI.endsWith("/login") || requestURI.endsWith("/refresh")) {
            allowedMethods = "POST, OPTIONS, HEAD";
        } else if (requestURI.endsWith("/logout")) {
            allowedMethods = "POST, OPTIONS, HEAD";
        } else if (requestURI.endsWith("/validate") || requestURI.endsWith("/me")) {
            allowedMethods = "GET, OPTIONS, HEAD";
        } else if (requestURI.endsWith("/password") || requestURI.endsWith("/profile")) {
            allowedMethods = "PATCH, OPTIONS, HEAD";
        } else {
            allowedMethods = "GET, POST, PATCH, OPTIONS, HEAD";
        }
        
        return ResponseEntity.ok()
                .header("Allow", allowedMethods)
                .header("Access-Control-Allow-Methods", allowedMethods)
                .header("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With, Accept, Origin")
                .header("Access-Control-Max-Age", "3600")
                .build();
    }

    /**
     * Handles HEAD requests for endpoint availability and metadata.
     * 
     * @param request the HTTP request
     * @return response headers without body
     */
    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<Void> handleHead(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0");
        
        // Add endpoint-specific headers
        if (requestURI.endsWith("/validate") || requestURI.endsWith("/me")) {
            response.header("X-Endpoint-Type", "authentication-info");
        } else if (requestURI.endsWith("/login") || requestURI.endsWith("/refresh")) {
            response.header("X-Endpoint-Type", "authentication-action");
        } else if (requestURI.endsWith("/password") || requestURI.endsWith("/profile")) {
            response.header("X-Endpoint-Type", "user-update");
        }
        
        return response.build();
    }
}