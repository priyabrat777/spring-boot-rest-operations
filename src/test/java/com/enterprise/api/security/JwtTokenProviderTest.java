package com.enterprise.api.security;

import com.enterprise.api.entity.Permission;
import com.enterprise.api.entity.Role;
import com.enterprise.api.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider.
 * Tests JWT token generation, validation, and claims extraction.
 * 
 * Requirements addressed:
 * - 4.1: JWT token generation and validation testing
 * - 4.2: Token signature validation and expiration handling testing
 * - 4.3: Claims extraction for user roles and permissions testing
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Token Provider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;
    private CustomUserPrincipal userPrincipal;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Initialize JWT token provider with test configuration
        jwtTokenProvider = new JwtTokenProvider(
            "testSecretKeyThatIsLongEnoughForHS256Algorithm", 
            3600000L, // 1 hour
            86400000L // 24 hours
        );

        // Create test user with roles and permissions
        testUser = createTestUser();
        userPrincipal = new CustomUserPrincipal(testUser);
        authentication = new UsernamePasswordAuthenticationToken(
            userPrincipal, 
            null, 
            userPrincipal.getAuthorities()
        );
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void shouldGenerateValidJwtToken() {
        // When
        String token = jwtTokenProvider.generateToken(authentication);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts separated by dots
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void shouldGenerateValidRefreshToken() {
        // When
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(refreshToken.split("\\.")).hasSize(3);
        assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(refreshToken)).isTrue();
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsernameFromToken() {
        // Given
        String token = jwtTokenProvider.generateToken(authentication);

        // When
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertThat(username).isEqualTo(testUser.getUsername());
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserIdFromToken() {
        // Given
        String token = jwtTokenProvider.generateToken(authentication);

        // When
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertThat(userId).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should extract email from token")
    void shouldExtractEmailFromToken() {
        // Given
        String token = jwtTokenProvider.generateToken(authentication);

        // When
        String email = jwtTokenProvider.getEmailFromToken(token);

        // Then
        assertThat(email).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should extract authorities from token")
    void shouldExtractAuthoritiesFromToken() {
        // Given
        String token = jwtTokenProvider.generateToken(authentication);

        // When
        Collection<? extends GrantedAuthority> authorities = jwtTokenProvider.getAuthoritiesFromToken(token);

        // Then
        assertThat(authorities).isNotEmpty();
        assertThat(authorities).hasSize(3); // ROLE_USER, PERMISSION_USER:READ, PERMISSION_USER:WRITE
        
        Set<String> authorityNames = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(java.util.stream.Collectors.toSet());
        
        assertThat(authorityNames).contains("ROLE_USER", "PERMISSION_USER:READ", "PERMISSION_USER:WRITE");
    }

    @Test
    @DisplayName("Should get expiration date from token")
    void shouldGetExpirationDateFromToken() {
        // Given
        String token = jwtTokenProvider.generateToken(authentication);

        // When
        Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(token);

        // Then
        assertThat(expirationDate).isNotNull();
        assertThat(expirationDate).isAfter(new Date());
        
        // Should expire in approximately 1 hour (allowing for small time differences)
        long expectedExpiration = System.currentTimeMillis() + 3600000L;
        assertThat(expirationDate.getTime()).isBetween(
            expectedExpiration - 5000L, // 5 seconds tolerance
            expectedExpiration + 5000L
        );
    }

    @Test
    @DisplayName("Should validate token correctly")
    void shouldValidateTokenCorrectly() {
        // Given
        String validToken = jwtTokenProvider.generateToken(authentication);

        // When & Then
        assertThat(jwtTokenProvider.validateToken(validToken)).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid token signature")
    void shouldRejectInvalidTokenSignature() {
        // Given
        String validToken = jwtTokenProvider.generateToken(authentication);
        String invalidToken = validToken + "invalid";

        // When & Then - The validateToken method catches exceptions and returns false
        assertThat(jwtTokenProvider.validateToken(invalidToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject malformed token")
    void shouldRejectMalformedToken() {
        // Given
        String malformedToken = "not.a.jwt.token";

        // When & Then
        assertThat(jwtTokenProvider.validateToken(malformedToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject null or empty token")
    void shouldRejectNullOrEmptyToken() {
        // When & Then
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
        assertThat(jwtTokenProvider.validateToken("   ")).isFalse();
    }

    @Test
    @DisplayName("Should detect expired token")
    void shouldDetectExpiredToken() {
        // Given - Create provider with very short expiration
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(
            "testSecretKeyThatIsLongEnoughForHS256Algorithm", 
            1L, // 1 millisecond
            86400000L
        );
        
        String token = shortExpirationProvider.generateToken(authentication);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When & Then
        assertThat(shortExpirationProvider.validateToken(token)).isFalse();
        assertThat(shortExpirationProvider.isTokenExpired(token)).isTrue();
    }

    @Test
    @DisplayName("Should calculate remaining time correctly")
    void shouldCalculateRemainingTimeCorrectly() {
        // Given
        String token = jwtTokenProvider.generateToken(authentication);

        // When
        long remainingTime = jwtTokenProvider.getTokenRemainingTime(token);

        // Then
        assertThat(remainingTime).isPositive();
        assertThat(remainingTime).isLessThanOrEqualTo(3600000L); // Should be less than or equal to 1 hour
        assertThat(remainingTime).isGreaterThan(3590000L); // Should be greater than 59.5 minutes
    }

    @Test
    @DisplayName("Should identify refresh token correctly")
    void shouldIdentifyRefreshTokenCorrectly() {
        // Given
        String accessToken = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        // When & Then
        assertThat(jwtTokenProvider.isRefreshToken(accessToken)).isFalse();
        assertThat(jwtTokenProvider.isRefreshToken(refreshToken)).isTrue();
    }

    @Test
    @DisplayName("Should handle token with no authorities")
    void shouldHandleTokenWithNoAuthorities() {
        // Given
        User userWithoutRoles = new User("testuser", "password", "test@example.com");
        userWithoutRoles.setId(1L);
        
        CustomUserPrincipal principalWithoutRoles = new CustomUserPrincipal(userWithoutRoles);
        Authentication authWithoutRoles = new UsernamePasswordAuthenticationToken(
            principalWithoutRoles, null, principalWithoutRoles.getAuthorities()
        );
        
        String token = jwtTokenProvider.generateToken(authWithoutRoles);

        // When
        Collection<? extends GrantedAuthority> authorities = jwtTokenProvider.getAuthoritiesFromToken(token);

        // Then
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception when extracting claims from invalid token")
    void shouldThrowExceptionWhenExtractingClaimsFromInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.getUsernameFromToken(invalidToken))
            .isInstanceOf(JwtException.class);
        
        assertThatThrownBy(() -> jwtTokenProvider.getUserIdFromToken(invalidToken))
            .isInstanceOf(JwtException.class);
        
        assertThatThrownBy(() -> jwtTokenProvider.getEmailFromToken(invalidToken))
            .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Should handle expired token gracefully in remaining time calculation")
    void shouldHandleExpiredTokenGracefullyInRemainingTimeCalculation() {
        // Given - Create provider with very short expiration
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(
            "testSecretKeyThatIsLongEnoughForHS256Algorithm", 
            1L, // 1 millisecond
            86400000L
        );
        
        String token = shortExpirationProvider.generateToken(authentication);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        long remainingTime = shortExpirationProvider.getTokenRemainingTime(token);

        // Then
        assertThat(remainingTime).isZero();
    }

    /**
     * Creates a test user with roles and permissions for testing.
     */
    private User createTestUser() {
        User user = new User("testuser", "password", "test@example.com");
        user.setId(1L);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        // Create role with permissions
        Role userRole = new Role("USER", "Standard user role");
        userRole.setId(1L);

        Permission readPermission = new Permission("USER:READ", "Read user data", "user", "read");
        readPermission.setId(1L);
        
        Permission writePermission = new Permission("USER:WRITE", "Write user data", "user", "write");
        writePermission.setId(2L);

        userRole.addPermission(readPermission);
        userRole.addPermission(writePermission);
        user.addRole(userRole);

        return user;
    }
}