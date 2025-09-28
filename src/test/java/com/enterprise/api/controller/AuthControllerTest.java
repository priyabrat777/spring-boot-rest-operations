package com.enterprise.api.controller;

import com.enterprise.api.dto.request.LoginRequest;
import com.enterprise.api.dto.request.RefreshTokenRequest;
import com.enterprise.api.dto.response.AuthResponse;
import com.enterprise.api.dto.response.TokenResponse;
import com.enterprise.api.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController.
 * Tests all authentication endpoints with proper HTTP status codes and response handling.
 * 
 * Requirements addressed:
 * - 1.1: GET operations testing
 * - 1.2: POST operations testing
 * - 1.4: HTTP status codes and response handling
 * - 4.4: JWT-based authentication endpoints
 * - 4.5: Authentication error handling
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequest loginRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private AuthResponse authResponse;
    private TokenResponse tokenResponse;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("testuser", "password123");
        refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("refresh-token-123");

        // Create auth response
        authResponse = new AuthResponse();
        authResponse.setAccessToken("access-token-123");
        authResponse.setRefreshToken("refresh-token-123");
        authResponse.setExpiresIn(3600L);

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
        userInfo.setId(1L);
        userInfo.setUsername("testuser");
        userInfo.setEmail("test@example.com");
        authResponse.setUser(userInfo);

        // Create token response
        tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("new-access-token-123");
        tokenResponse.setExpiresIn(3600L);

        // Create authentication
        authentication = new UsernamePasswordAuthenticationToken(
                "testuser", 
                null, 
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void login_WithValidCredentials_ShouldReturnAuthResponse() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnBadRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("", "");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void logout_WithValidAuthentication_ShouldReturnSuccessMessage() throws Exception {
        when(authService.logout(any(Authentication.class))).thenReturn("Logout successful");

        mockMvc.perform(post("/api/v1/auth/logout")
                .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    void logout_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_WithValidToken_ShouldReturnNewTokens() throws Exception {
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("new-access-token-123"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void refreshToken_WithInvalidToken_ShouldReturnBadRequest() throws Exception {
        RefreshTokenRequest invalidRequest = new RefreshTokenRequest();
        // Empty refresh token

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void validateToken_WithValidAuthentication_ShouldReturnValidationInfo() throws Exception {
        when(authService.validateAuthentication(any(Authentication.class))).thenReturn(true);
        when(authService.getCurrentUserId(any(Authentication.class))).thenReturn(1L);
        when(authService.getCurrentUsername(any(Authentication.class))).thenReturn("testuser");

        mockMvc.perform(get("/api/v1/auth/validate")
                .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void validateToken_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/validate"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getCurrentUser_WithValidAuthentication_ShouldReturnUserInfo() throws Exception {
        when(authService.getCurrentUserId(any(Authentication.class))).thenReturn(1L);
        when(authService.getCurrentUsername(any(Authentication.class))).thenReturn("testuser");

        mockMvc.perform(get("/api/v1/auth/me")
                .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void getCurrentUser_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void handleOptions_ShouldReturnAllowedMethods() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("Allow", "GET, POST, PUT, DELETE, OPTIONS, HEAD"));
    }

    @Test
    void handleHead_ShouldReturnHeadersWithoutBody() throws Exception {
        mockMvc.perform(head("/api/v1/auth/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(content().string(""));
    }

    @Test
    void login_WithMissingUsername_ShouldReturnBadRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithMissingPassword_ShouldReturnBadRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setUsernameOrEmail("testuser");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithShortPassword_ShouldReturnBadRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("testuser", "123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_WithEmptyBody_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void validateToken_WithInvalidAuthentication_ShouldReturnUnauthorized() throws Exception {
        when(authService.validateAuthentication(any(Authentication.class))).thenReturn(false);

        mockMvc.perform(get("/api/v1/auth/validate")
                .with(authentication(authentication)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.error").value("Invalid token"));
    }
}