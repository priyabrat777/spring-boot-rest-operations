package com.enterprise.api.service;

import com.enterprise.api.dto.request.LoginRequest;
import com.enterprise.api.dto.request.RefreshTokenRequest;
import com.enterprise.api.dto.response.AuthResponse;
import com.enterprise.api.dto.response.TokenResponse;
import com.enterprise.api.entity.Permission;
import com.enterprise.api.entity.Role;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.UserRepository;
import com.enterprise.api.security.CustomUserPrincipal;
import com.enterprise.api.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.
 * Tests authentication flows, token operations, and authorization checks.
 * 
 * Requirements addressed:
 * - 4.1: JWT-based authentication testing
 * - 4.2: Token validation and refresh testing
 * - 4.6: Role-based data filtering testing
 * - 4.7: User account status validation testing
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private Role testRole;
    private Permission testPermission;
    private CustomUserPrincipal userPrincipal;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Create test permission
        testPermission = new Permission("USER_READ", "user", "read");
        testPermission.setId(1L);

        // Create test role
        testRole = new Role("USER");
        testRole.setId(1L);
        testRole.addPermission(testPermission);

        // Create test user
        testUser = new User("testuser", "encodedPassword", "test@example.com");
        testUser.setId(1L);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setAccountNonExpired(true);
        testUser.setAccountNonLocked(true);
        testUser.setCredentialsNonExpired(true);
        testUser.addRole(testRole);

        // Create user principal and authentication
        userPrincipal = new CustomUserPrincipal(testUser);
        authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, userPrincipal.getAuthorities());
    }

    @Test
    void login_WithValidCredentials_ShouldReturnAuthResponse() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("testuser", "password");
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        long expiresIn = 3600000L;

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(authentication)).thenReturn(accessToken);
        when(jwtTokenProvider.generateRefreshToken(authentication)).thenReturn(refreshToken);
        when(jwtTokenProvider.getTokenRemainingTime(accessToken)).thenReturn(expiresIn);

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(expiresIn, response.getExpiresIn());
        
        assertNotNull(response.getUser());
        assertEquals(testUser.getId(), response.getUser().getId());
        assertEquals(testUser.getUsername(), response.getUser().getUsername());
        assertEquals(testUser.getEmail(), response.getUser().getEmail());
        assertTrue(response.getUser().isEnabled());
        assertEquals(1, response.getUser().getRoles().size());
        assertTrue(response.getUser().getRoles().contains("USER"));
        assertEquals(1, response.getUser().getPermissions().size());
        assertTrue(response.getUser().getPermissions().contains("USER_READ"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByIdWithRolesAndPermissions(1L);
        verify(jwtTokenProvider).generateToken(authentication);
        verify(jwtTokenProvider).generateRefreshToken(authentication);
    }

    @Test
    void login_WithInvalidCredentials_ShouldThrowAuthenticationException() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("testuser", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authService.login(loginRequest));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider, never()).generateToken(any());
        verify(jwtTokenProvider, never()).generateRefreshToken(any());
    }

    @Test
    void login_WithDisabledAccount_ShouldThrowAuthenticationException() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("testuser", "password");
        testUser.setEnabled(false);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("Account is disabled"));

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authService.login(loginRequest));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_WithLockedAccount_ShouldThrowAuthenticationException() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("testuser", "password");
        testUser.setAccountNonLocked(false);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new LockedException("Account is locked"));

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authService.login(loginRequest));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void logout_WithValidAuthentication_ShouldReturnSuccessMessage() {
        // Act
        String result = authService.logout(authentication);

        // Assert
        assertEquals("Logout successful", result);
    }

    @Test
    void logout_WithNullAuthentication_ShouldReturnNoSessionMessage() {
        // Act
        String result = authService.logout(null);

        // Assert
        assertEquals("No active session found", result);
    }

    @Test
    void refreshToken_WithValidRefreshToken_ShouldReturnNewAccessToken() {
        // Arrange
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest("valid-refresh-token");
        String newAccessToken = "new-access-token";
        long expiresIn = 3600000L;

        when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("valid-refresh-token")).thenReturn("testuser");
        when(jwtTokenProvider.getUserIdFromToken("valid-refresh-token")).thenReturn(1L);
        when(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn(newAccessToken);
        when(jwtTokenProvider.getTokenRemainingTime(newAccessToken)).thenReturn(expiresIn);

        // Act
        TokenResponse response = authService.refreshToken(refreshTokenRequest);

        // Assert
        assertNotNull(response);
        assertEquals(newAccessToken, response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(expiresIn, response.getExpiresIn());

        verify(jwtTokenProvider).validateToken("valid-refresh-token");
        verify(jwtTokenProvider).isRefreshToken("valid-refresh-token");
        verify(jwtTokenProvider).generateToken(any(Authentication.class));
    }

    @Test
    void refreshToken_WithInvalidRefreshToken_ShouldThrowAuthenticationException() {
        // Arrange
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest("invalid-refresh-token");

        when(jwtTokenProvider.validateToken("invalid-refresh-token")).thenReturn(false);

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authService.refreshToken(refreshTokenRequest));

        verify(jwtTokenProvider).validateToken("invalid-refresh-token");
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void refreshToken_WithAccessTokenInsteadOfRefreshToken_ShouldThrowAuthenticationException() {
        // Arrange
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest("access-token");

        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("access-token")).thenReturn(false);

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authService.refreshToken(refreshTokenRequest));

        verify(jwtTokenProvider).validateToken("access-token");
        verify(jwtTokenProvider).isRefreshToken("access-token");
    }

    @Test
    void refreshToken_WithNonExistentUser_ShouldThrowAuthenticationException() {
        // Arrange
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest("valid-refresh-token");

        when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("valid-refresh-token")).thenReturn("testuser");
        when(jwtTokenProvider.getUserIdFromToken("valid-refresh-token")).thenReturn(1L);
        when(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authService.refreshToken(refreshTokenRequest));

        verify(userRepository).findByIdWithRolesAndPermissions(1L);
    }

    @Test
    void validateAuthentication_WithValidAuthentication_ShouldReturnTrue() {
        // Arrange
        when(userRepository.findByIdActive(1L)).thenReturn(Optional.of(testUser));

        // Act
        boolean result = authService.validateAuthentication(authentication);

        // Assert
        assertTrue(result);
        verify(userRepository).findByIdActive(1L);
    }

    @Test
    void validateAuthentication_WithNullAuthentication_ShouldReturnFalse() {
        // Act
        boolean result = authService.validateAuthentication(null);

        // Assert
        assertFalse(result);
        verify(userRepository, never()).findByIdActive(any());
    }

    @Test
    void validateAuthentication_WithNonExistentUser_ShouldReturnFalse() {
        // Arrange
        when(userRepository.findByIdActive(1L)).thenReturn(Optional.empty());

        // Act
        boolean result = authService.validateAuthentication(authentication);

        // Assert
        assertFalse(result);
        verify(userRepository).findByIdActive(1L);
    }

    @Test
    void validateAuthentication_WithDisabledUser_ShouldReturnFalse() {
        // Arrange
        testUser.setEnabled(false);
        // Create new user principal with disabled user
        CustomUserPrincipal disabledUserPrincipal = new CustomUserPrincipal(testUser);
        Authentication disabledAuth = new UsernamePasswordAuthenticationToken(
                disabledUserPrincipal, null, disabledUserPrincipal.getAuthorities());
        
        when(userRepository.findByIdActive(1L)).thenReturn(Optional.of(testUser));

        // Act
        boolean result = authService.validateAuthentication(disabledAuth);

        // Assert
        assertFalse(result);
    }

    @Test
    void isAccountActive_WithActiveAccount_ShouldReturnTrue() {
        // Arrange
        when(userRepository.findByUsernameOrEmailActive("testuser")).thenReturn(Optional.of(testUser));

        // Act
        boolean result = authService.isAccountActive("testuser");

        // Assert
        assertTrue(result);
        verify(userRepository).findByUsernameOrEmailActive("testuser");
    }

    @Test
    void isAccountActive_WithNonExistentUser_ShouldReturnFalse() {
        // Arrange
        when(userRepository.findByUsernameOrEmailActive("nonexistent")).thenReturn(Optional.empty());

        // Act
        boolean result = authService.isAccountActive("nonexistent");

        // Assert
        assertFalse(result);
    }

    @Test
    void isAccountActive_WithDisabledAccount_ShouldReturnFalse() {
        // Arrange
        testUser.setEnabled(false);
        when(userRepository.findByUsernameOrEmailActive("testuser")).thenReturn(Optional.of(testUser));

        // Act
        boolean result = authService.isAccountActive("testuser");

        // Assert
        assertFalse(result);
    }

    @Test
    void getCurrentUserId_WithValidAuthentication_ShouldReturnUserId() {
        // Act
        Long userId = authService.getCurrentUserId(authentication);

        // Assert
        assertEquals(1L, userId);
    }

    @Test
    void getCurrentUserId_WithNullAuthentication_ShouldReturnNull() {
        // Act
        Long userId = authService.getCurrentUserId(null);

        // Assert
        assertNull(userId);
    }

    @Test
    void getCurrentUsername_WithValidAuthentication_ShouldReturnUsername() {
        // Act
        String username = authService.getCurrentUsername(authentication);

        // Assert
        assertEquals("testuser", username);
    }

    @Test
    void getCurrentUsername_WithNullAuthentication_ShouldReturnNull() {
        // Act
        String username = authService.getCurrentUsername(null);

        // Assert
        assertNull(username);
    }

    @Test
    void hasRole_WithValidRole_ShouldReturnTrue() {
        // Act
        boolean result = authService.hasRole(authentication, "USER");

        // Assert
        assertTrue(result);
    }

    @Test
    void hasRole_WithInvalidRole_ShouldReturnFalse() {
        // Act
        boolean result = authService.hasRole(authentication, "ADMIN");

        // Assert
        assertFalse(result);
    }

    @Test
    void hasRole_WithNullAuthentication_ShouldReturnFalse() {
        // Act
        boolean result = authService.hasRole(null, "USER");

        // Assert
        assertFalse(result);
    }

    @Test
    void hasPermission_WithValidPermission_ShouldReturnTrue() {
        // Act
        boolean result = authService.hasPermission(authentication, "user", "read");

        // Assert
        assertTrue(result);
    }

    @Test
    void hasPermission_WithInvalidPermission_ShouldReturnFalse() {
        // Act
        boolean result = authService.hasPermission(authentication, "user", "delete");

        // Assert
        assertFalse(result);
    }

    @Test
    void hasPermission_WithNullAuthentication_ShouldReturnFalse() {
        // Act
        boolean result = authService.hasPermission(null, "user", "read");

        // Assert
        assertFalse(result);
    }

    @Test
    void login_WithRememberMeFlag_ShouldGenerateTokens() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("testuser", "password", true);
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        long expiresIn = 3600000L;

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(authentication)).thenReturn(accessToken);
        when(jwtTokenProvider.generateRefreshToken(authentication)).thenReturn(refreshToken);
        when(jwtTokenProvider.getTokenRemainingTime(accessToken)).thenReturn(expiresIn);

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());
    }

    @Test
    void refreshToken_WithDisabledUserAccount_ShouldThrowAuthenticationException() {
        // Arrange
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest("valid-refresh-token");
        testUser.setEnabled(false);

        when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("valid-refresh-token")).thenReturn("testuser");
        when(jwtTokenProvider.getUserIdFromToken("valid-refresh-token")).thenReturn(1L);
        when(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authService.refreshToken(refreshTokenRequest));
    }
}