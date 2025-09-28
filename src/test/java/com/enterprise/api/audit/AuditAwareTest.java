package com.enterprise.api.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuditAware implementation.
 */
@ExtendWith(MockitoExtension.class)
class AuditAwareTest {

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private AuditAware auditAware;

    @BeforeEach
    void setUp() {
        auditAware = new AuditAware();
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getCurrentAuditor_WhenNoAuthentication_ShouldReturnSystem() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        Optional<String> result = auditAware.getCurrentAuditor();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("SYSTEM");
    }

    @Test
    void getCurrentAuditor_WhenNotAuthenticated_ShouldReturnAnonymous() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        Optional<String> result = auditAware.getCurrentAuditor();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("ANONYMOUS");
    }

    @Test
    void getCurrentAuditor_WhenAnonymousUser_ShouldReturnAnonymous() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("anonymousUser");

        // When
        Optional<String> result = auditAware.getCurrentAuditor();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("ANONYMOUS");
    }

    @Test
    void getCurrentAuditor_WhenAuthenticatedUser_ShouldReturnUsername() {
        // Given
        String username = "testuser";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);

        // When
        Optional<String> result = auditAware.getCurrentAuditor();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(username);
    }

    @Test
    void getCurrentAuditor_WhenUsernameIsNull_ShouldReturnSystem() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(null);

        // When
        Optional<String> result = auditAware.getCurrentAuditor();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("SYSTEM");
    }

    @Test
    void getCurrentAuditorName_ShouldReturnCurrentAuditorWithoutOptional() {
        // Given
        String username = "testuser";
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);

        // When
        String result = auditAware.getCurrentAuditorName();

        // Then
        assertThat(result).isEqualTo(username);
    }

    @Test
    void isUserAuthenticated_WhenNoAuthentication_ShouldReturnFalse() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        boolean result = auditAware.isUserAuthenticated();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isUserAuthenticated_WhenNotAuthenticated_ShouldReturnFalse() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        boolean result = auditAware.isUserAuthenticated();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isUserAuthenticated_WhenAnonymousUser_ShouldReturnFalse() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("anonymousUser");

        // When
        boolean result = auditAware.isUserAuthenticated();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isUserAuthenticated_WhenAuthenticatedUser_ShouldReturnTrue() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");

        // When
        boolean result = auditAware.isUserAuthenticated();

        // Then
        assertThat(result).isTrue();
    }
}