package com.enterprise.api.security;

import com.enterprise.api.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for JwtAuthenticationFilter.
 * Tests JWT token processing, validation, and security context setup.
 * 
 * Requirements addressed:
 * - 4.1: JWT token processing for request authentication testing
 * - 4.2: Token validation and security context setup testing
 * - 4.3: Integration with Spring Security filter chain testing
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Authentication Filter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private CustomUserPrincipal userPrincipal;
    private final String validToken = "valid.jwt.token";
    private final String invalidToken = "invalid.jwt.token";

    @BeforeEach
    void setUp() {
        // Create test user and principal
        User testUser = createTestUser();
        userPrincipal = new CustomUserPrincipal(testUser);

        // Set up security context
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(null);
    }

    @Test
    @DisplayName("Should authenticate user with valid JWT token")
    void shouldAuthenticateUserWithValidJwtToken() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(request.getRequestURI()).thenReturn("/api/users/me");
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validToken)).thenReturn(1L);
        when(customUserDetailsService.loadUserById(1L)).thenReturn(userPrincipal);
        when(customUserDetailsService.isAccountValid(userPrincipal)).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider).validateToken(validToken);
        verify(jwtTokenProvider).getUserIdFromToken(validToken);
        verify(customUserDetailsService).loadUserById(1L);
        verify(customUserDetailsService).isAccountValid(userPrincipal);
        verify(securityContext).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate with invalid JWT token")
    void shouldNotAuthenticateWithInvalidJwtToken() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(request.getRequestURI()).thenReturn("/api/users/me");
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider).validateToken(invalidToken);
        verify(jwtTokenProvider, never()).getUserIdFromToken(anyString());
        verify(customUserDetailsService, never()).loadUserById(anyLong());
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate when no authorization header")
    void shouldNotAuthenticateWhenNoAuthorizationHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(customUserDetailsService, never()).loadUserById(anyLong());
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate when authorization header does not start with Bearer")
    void shouldNotAuthenticateWhenAuthorizationHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Basic " + validToken);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(customUserDetailsService, never()).loadUserById(anyLong());
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate when user account is invalid")
    void shouldNotAuthenticateWhenUserAccountIsInvalid() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(request.getRequestURI()).thenReturn("/api/users/me");
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validToken)).thenReturn(1L);
        when(customUserDetailsService.loadUserById(1L)).thenReturn(userPrincipal);
        when(customUserDetailsService.isAccountValid(userPrincipal)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider).validateToken(validToken);
        verify(jwtTokenProvider).getUserIdFromToken(validToken);
        verify(customUserDetailsService).loadUserById(1L);
        verify(customUserDetailsService).isAccountValid(userPrincipal);
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate when user is not found")
    void shouldNotAuthenticateWhenUserIsNotFound() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validToken)).thenReturn(999L);
        when(customUserDetailsService.loadUserById(999L))
                .thenThrow(new UsernameNotFoundException("User not found"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider).validateToken(validToken);
        verify(jwtTokenProvider).getUserIdFromToken(validToken);
        verify(customUserDetailsService).loadUserById(999L);
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate when authentication already exists")
    void shouldNotAuthenticateWhenAuthenticationAlreadyExists() throws ServletException, IOException {
        // Given
        Authentication existingAuth = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(existingAuth);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validToken)).thenReturn(1L);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider).validateToken(validToken);
        verify(jwtTokenProvider).getUserIdFromToken(validToken);
        verify(customUserDetailsService, never()).loadUserById(anyLong());
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should skip authentication for public endpoints")
    void shouldSkipAuthenticationForPublicEndpoints() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        // When - Call doFilter instead of doFilterInternal to test shouldNotFilter
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should skip authentication for swagger endpoints")
    void shouldSkipAuthenticationForSwaggerEndpoints() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should skip authentication for actuator health endpoint")
    void shouldSkipAuthenticationForActuatorHealthEndpoint() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // When
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle empty bearer token")
    void shouldHandleEmptyBearerToken() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(customUserDetailsService, never()).loadUserById(anyLong());
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle whitespace-only bearer token")
    void shouldHandleWhitespaceOnlyBearerToken() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer    ");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(customUserDetailsService, never()).loadUserById(anyLong());
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should clear security context on exception")
    void shouldClearSecurityContextOnException() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validToken)).thenReturn(1L);
        when(customUserDetailsService.loadUserById(1L))
                .thenThrow(new RuntimeException("Database error"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider).validateToken(validToken);
        verify(jwtTokenProvider).getUserIdFromToken(validToken);
        verify(customUserDetailsService).loadUserById(1L);
        verify(filterChain).doFilter(request, response);
        // SecurityContextHolder.clearContext() is called internally
    }

    @Test
    @DisplayName("Should handle null user ID from token")
    void shouldHandleNullUserIdFromToken() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validToken)).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider).validateToken(validToken);
        verify(jwtTokenProvider).getUserIdFromToken(validToken);
        verify(customUserDetailsService, never()).loadUserById(anyLong());
        verify(securityContext, never()).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should process multiple public endpoints correctly")
    void shouldProcessMultiplePublicEndpointsCorrectly() throws ServletException, IOException {
        String[] publicPaths = {
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/refresh",
                "/api/captcha/generate",
                "/actuator/health",
                "/swagger-ui/index.html",
                "/v3/api-docs/swagger-config",
                "/favicon.ico",
                "/error"
        };

        for (String path : publicPaths) {
            // Given
            reset(request, filterChain);
            when(request.getRequestURI()).thenReturn(path);

            // When
            jwtAuthenticationFilter.doFilter(request, response, filterChain);

            // Then
            verify(jwtTokenProvider, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }
    }

    /**
     * Creates a test user for testing.
     */
    private User createTestUser() {
        User user = new User("testuser", "encodedPassword", "test@example.com");
        user.setId(1L);
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        return user;
    }
}