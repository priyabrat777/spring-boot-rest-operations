package com.enterprise.api.service;

import com.enterprise.api.dto.request.LoginRequest;
import com.enterprise.api.dto.request.RefreshTokenRequest;
import com.enterprise.api.dto.response.AuthResponse;
import com.enterprise.api.dto.response.TokenResponse;
import org.springframework.security.core.Authentication;

/**
 * Service interface for authentication operations.
 * Handles login, logout, token refresh, and authentication validation.
 * 
 * Requirements addressed:
 * - 4.1: JWT-based authentication with login and logout
 * - 4.2: Token validation and refresh functionality
 * - 4.6: Role-based data filtering during authentication
 * - 4.7: User account status validation
 */
public interface AuthService {

    /**
     * Authenticates a user with username/email and password.
     * 
     * @param loginRequest the login request containing credentials
     * @return authentication response with tokens and user info
     * @throws AuthenticationException if authentication fails
     */
    AuthResponse login(LoginRequest loginRequest);

    /**
     * Logs out a user by invalidating their tokens.
     * 
     * @param authentication the current authentication
     * @return success message
     */
    String logout(Authentication authentication);

    /**
     * Refreshes an access token using a valid refresh token.
     * 
     * @param refreshTokenRequest the refresh token request
     * @return new token response with refreshed access token
     * @throws AuthenticationException if refresh token is invalid
     */
    TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    /**
     * Validates if the current authentication is valid and active.
     * 
     * @param authentication the authentication to validate
     * @return true if authentication is valid, false otherwise
     */
    boolean validateAuthentication(Authentication authentication);

    /**
     * Checks if a user account is locked or disabled.
     * 
     * @param username the username to check
     * @return true if account is active, false if locked/disabled
     */
    boolean isAccountActive(String username);

    /**
     * Gets the current authenticated user's ID.
     * 
     * @param authentication the current authentication
     * @return the user ID
     */
    Long getCurrentUserId(Authentication authentication);

    /**
     * Gets the current authenticated user's username.
     * 
     * @param authentication the current authentication
     * @return the username
     */
    String getCurrentUsername(Authentication authentication);

    /**
     * Checks if the current user has a specific role.
     * 
     * @param authentication the current authentication
     * @param roleName the role name to check
     * @return true if user has the role, false otherwise
     */
    boolean hasRole(Authentication authentication, String roleName);

    /**
     * Checks if the current user has a specific permission.
     * 
     * @param authentication the current authentication
     * @param resource the resource name
     * @param action the action name
     * @return true if user has the permission, false otherwise
     */
    boolean hasPermission(Authentication authentication, String resource, String action);
}