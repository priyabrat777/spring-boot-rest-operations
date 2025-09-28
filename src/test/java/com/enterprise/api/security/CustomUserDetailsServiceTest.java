package com.enterprise.api.security;

import com.enterprise.api.entity.Permission;
import com.enterprise.api.entity.Role;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomUserDetailsService.
 * Tests user loading, validation, and exception handling.
 * 
 * Requirements addressed:
 * - 4.1: User authentication with custom UserDetailsService testing
 * - 4.2: User data loading for JWT token validation testing
 * - 4.3: Role and permission loading for RBAC testing
 * - 4.7: User account status validation testing
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Custom User Details Service Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
    }

    @Test
    @DisplayName("Should load user by username successfully")
    void shouldLoadUserByUsernameSuccessfully() {
        // Given
        when(userRepository.findByUsernameWithRolesAndPermissions("testuser"))
            .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails).isInstanceOf(CustomUserPrincipal.class);
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities()).hasSize(3); // ROLE_USER + 2 permissions

        verify(userRepository).findByUsernameWithRolesAndPermissions("testuser");
    }

    @Test
    @DisplayName("Should throw exception when user not found by username")
    void shouldThrowExceptionWhenUserNotFoundByUsername() {
        // Given
        when(userRepository.findByUsernameWithRolesAndPermissions("nonexistent"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found with username: nonexistent");

        verify(userRepository).findByUsernameWithRolesAndPermissions("nonexistent");
    }

    @Test
    @DisplayName("Should throw exception when user is soft deleted")
    void shouldThrowExceptionWhenUserIsSoftDeleted() {
        // Given
        testUser.setDeleted(true);
        when(userRepository.findByUsernameWithRolesAndPermissions("testuser"))
            .thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("testuser"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User account is no longer active: testuser");

        verify(userRepository).findByUsernameWithRolesAndPermissions("testuser");
    }

    @Test
    @DisplayName("Should load user by ID successfully")
    void shouldLoadUserByIdSuccessfully() {
        // Given
        when(userRepository.findByIdWithRolesAndPermissions(1L))
            .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserById(1L);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails).isInstanceOf(CustomUserPrincipal.class);
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(((CustomUserPrincipal) userDetails).getId()).isEqualTo(1L);

        verify(userRepository).findByIdWithRolesAndPermissions(1L);
    }

    @Test
    @DisplayName("Should throw exception when user not found by ID")
    void shouldThrowExceptionWhenUserNotFoundById() {
        // Given
        when(userRepository.findByIdWithRolesAndPermissions(999L))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserById(999L))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found with ID: 999");

        verify(userRepository).findByIdWithRolesAndPermissions(999L);
    }

    @Test
    @DisplayName("Should throw exception when user found by ID is soft deleted")
    void shouldThrowExceptionWhenUserFoundByIdIsSoftDeleted() {
        // Given
        testUser.setDeleted(true);
        when(userRepository.findByIdWithRolesAndPermissions(1L))
            .thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserById(1L))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User account is no longer active with ID: 1");

        verify(userRepository).findByIdWithRolesAndPermissions(1L);
    }

    @Test
    @DisplayName("Should load user by email successfully")
    void shouldLoadUserByEmailSuccessfully() {
        // Given
        when(userRepository.findByEmailWithRolesAndPermissions("test@example.com"))
            .thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByEmail("test@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails).isInstanceOf(CustomUserPrincipal.class);
        assertThat(((CustomUserPrincipal) userDetails).getEmail()).isEqualTo("test@example.com");

        verify(userRepository).findByEmailWithRolesAndPermissions("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when user not found by email")
    void shouldThrowExceptionWhenUserNotFoundByEmail() {
        // Given
        when(userRepository.findByEmailWithRolesAndPermissions("nonexistent@example.com"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByEmail("nonexistent@example.com"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found with email: nonexistent@example.com");

        verify(userRepository).findByEmailWithRolesAndPermissions("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should throw exception when user found by email is soft deleted")
    void shouldThrowExceptionWhenUserFoundByEmailIsSoftDeleted() {
        // Given
        testUser.setDeleted(true);
        when(userRepository.findByEmailWithRolesAndPermissions("test@example.com"))
            .thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByEmail("test@example.com"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User account is no longer active with email: test@example.com");

        verify(userRepository).findByEmailWithRolesAndPermissions("test@example.com");
    }

    @Test
    @DisplayName("Should check if user exists by username")
    void shouldCheckIfUserExistsByUsername() {
        // Given
        when(userRepository.existsByUsernameAndDeletedFalse("testuser")).thenReturn(true);
        when(userRepository.existsByUsernameAndDeletedFalse("nonexistent")).thenReturn(false);

        // When & Then
        assertThat(userDetailsService.existsByUsername("testuser")).isTrue();
        assertThat(userDetailsService.existsByUsername("nonexistent")).isFalse();

        verify(userRepository).existsByUsernameAndDeletedFalse("testuser");
        verify(userRepository).existsByUsernameAndDeletedFalse("nonexistent");
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void shouldCheckIfUserExistsByEmail() {
        // Given
        when(userRepository.existsByEmailAndDeletedFalse("test@example.com")).thenReturn(true);
        when(userRepository.existsByEmailAndDeletedFalse("nonexistent@example.com")).thenReturn(false);

        // When & Then
        assertThat(userDetailsService.existsByEmail("test@example.com")).isTrue();
        assertThat(userDetailsService.existsByEmail("nonexistent@example.com")).isFalse();

        verify(userRepository).existsByEmailAndDeletedFalse("test@example.com");
        verify(userRepository).existsByEmailAndDeletedFalse("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should validate account status correctly for valid account")
    void shouldValidateAccountStatusCorrectlyForValidAccount() {
        // Given
        CustomUserPrincipal validPrincipal = new CustomUserPrincipal(testUser);

        // When
        boolean isValid = userDetailsService.isAccountValid(validPrincipal);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should validate account status correctly for disabled account")
    void shouldValidateAccountStatusCorrectlyForDisabledAccount() {
        // Given
        testUser.setEnabled(false);
        CustomUserPrincipal disabledPrincipal = new CustomUserPrincipal(testUser);

        // When
        boolean isValid = userDetailsService.isAccountValid(disabledPrincipal);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should validate account status correctly for locked account")
    void shouldValidateAccountStatusCorrectlyForLockedAccount() {
        // Given
        testUser.setAccountNonLocked(false);
        CustomUserPrincipal lockedPrincipal = new CustomUserPrincipal(testUser);

        // When
        boolean isValid = userDetailsService.isAccountValid(lockedPrincipal);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should validate account status correctly for expired account")
    void shouldValidateAccountStatusCorrectlyForExpiredAccount() {
        // Given
        testUser.setAccountNonExpired(false);
        CustomUserPrincipal expiredPrincipal = new CustomUserPrincipal(testUser);

        // When
        boolean isValid = userDetailsService.isAccountValid(expiredPrincipal);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should validate account status correctly for expired credentials")
    void shouldValidateAccountStatusCorrectlyForExpiredCredentials() {
        // Given
        testUser.setCredentialsNonExpired(false);
        CustomUserPrincipal expiredCredentialsPrincipal = new CustomUserPrincipal(testUser);

        // When
        boolean isValid = userDetailsService.isAccountValid(expiredCredentialsPrincipal);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void shouldHandleRepositoryExceptionsGracefully() {
        // Given
        when(userRepository.findByUsernameWithRolesAndPermissions(anyString()))
            .thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("testuser"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Database connection error");

        verify(userRepository).findByUsernameWithRolesAndPermissions("testuser");
    }

    @Test
    @DisplayName("Should load user with multiple roles and permissions")
    void shouldLoadUserWithMultipleRolesAndPermissions() {
        // Given
        User userWithMultipleRoles = createUserWithMultipleRoles();
        when(userRepository.findByUsernameWithRolesAndPermissions("multiuser"))
            .thenReturn(Optional.of(userWithMultipleRoles));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("multiuser");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).hasSize(5); // 2 roles + 3 permissions
        
        CustomUserPrincipal principal = (CustomUserPrincipal) userDetails;
        assertThat(principal.hasRole("USER")).isTrue();
        assertThat(principal.hasRole("ADMIN")).isTrue();
        assertThat(principal.hasPermission("USER:READ")).isTrue();
        assertThat(principal.hasPermission("USER:WRITE")).isTrue();
        assertThat(principal.hasPermission("ADMIN:ALL")).isTrue();

        verify(userRepository).findByUsernameWithRolesAndPermissions("multiuser");
    }

    /**
     * Creates a test user with a single role and permissions.
     */
    private User createTestUser() {
        User user = new User("testuser", "encodedPassword", "test@example.com");
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

    /**
     * Creates a test user with multiple roles and permissions.
     */
    private User createUserWithMultipleRoles() {
        User user = new User("multiuser", "encodedPassword", "multi@example.com");
        user.setId(2L);
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        // Create USER role
        Role userRole = new Role("USER", "Standard user role");
        userRole.setId(1L);

        Permission readPermission = new Permission("USER:READ", "Read user data", "user", "read");
        readPermission.setId(1L);
        
        Permission writePermission = new Permission("USER:WRITE", "Write user data", "user", "write");
        writePermission.setId(2L);

        userRole.addPermission(readPermission);
        userRole.addPermission(writePermission);

        // Create ADMIN role
        Role adminRole = new Role("ADMIN", "Administrator role");
        adminRole.setId(2L);

        Permission adminPermission = new Permission("ADMIN:ALL", "All admin permissions", "admin", "all");
        adminPermission.setId(3L);

        adminRole.addPermission(adminPermission);

        // Add roles to user
        user.addRole(userRole);
        user.addRole(adminRole);

        return user;
    }
}