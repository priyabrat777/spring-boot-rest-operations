package com.enterprise.api.security;

import com.enterprise.api.entity.Permission;
import com.enterprise.api.entity.Role;
import com.enterprise.api.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CustomUserPrincipal.
 * Tests UserDetails implementation and authority mapping.
 * 
 * Requirements addressed:
 * - 4.1: User authentication with UserDetails implementation testing
 * - 4.3: Role-based access control with authorities mapping testing
 * - 4.7: User account status validation testing
 */
@DisplayName("Custom User Principal Tests")
class CustomUserPrincipalTest {

    private User testUser;
    private CustomUserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        testUser = createTestUserWithRolesAndPermissions();
        userPrincipal = new CustomUserPrincipal(testUser);
    }

    @Test
    @DisplayName("Should create user principal with correct basic properties")
    void shouldCreateUserPrincipalWithCorrectBasicProperties() {
        // Then
        assertThat(userPrincipal.getId()).isEqualTo(testUser.getId());
        assertThat(userPrincipal.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(userPrincipal.getPassword()).isEqualTo(testUser.getPassword());
        assertThat(userPrincipal.getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should map account status correctly")
    void shouldMapAccountStatusCorrectly() {
        // Then
        assertThat(userPrincipal.isEnabled()).isEqualTo(testUser.isEnabled());
        assertThat(userPrincipal.isAccountNonExpired()).isEqualTo(testUser.isAccountNonExpired());
        assertThat(userPrincipal.isAccountNonLocked()).isEqualTo(testUser.isAccountNonLocked());
        assertThat(userPrincipal.isCredentialsNonExpired()).isEqualTo(testUser.isCredentialsNonExpired());
    }

    @Test
    @DisplayName("Should map roles to authorities correctly")
    void shouldMapRolesToAuthoritiesCorrectly() {
        // When
        Collection<? extends GrantedAuthority> authorities = userPrincipal.getAuthorities();

        // Then
        assertThat(authorities).hasSize(5); // 2 roles + 3 permissions
        
        Set<String> authorityNames = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(java.util.stream.Collectors.toSet());
        
        // Check role authorities
        assertThat(authorityNames).contains("ROLE_USER", "ROLE_ADMIN");
        
        // Check permission authorities
        assertThat(authorityNames).contains(
            "PERMISSION_USER:READ", 
            "PERMISSION_USER:WRITE", 
            "PERMISSION_ADMIN:ALL"
        );
    }

    @Test
    @DisplayName("Should check role existence correctly")
    void shouldCheckRoleExistenceCorrectly() {
        // When & Then
        assertThat(userPrincipal.hasRole("USER")).isTrue();
        assertThat(userPrincipal.hasRole("ADMIN")).isTrue();
        assertThat(userPrincipal.hasRole("MODERATOR")).isFalse();
        assertThat(userPrincipal.hasRole("GUEST")).isFalse();
    }

    @Test
    @DisplayName("Should check permission existence correctly")
    void shouldCheckPermissionExistenceCorrectly() {
        // When & Then
        assertThat(userPrincipal.hasPermission("USER:READ")).isTrue();
        assertThat(userPrincipal.hasPermission("USER:WRITE")).isTrue();
        assertThat(userPrincipal.hasPermission("ADMIN:ALL")).isTrue();
        assertThat(userPrincipal.hasPermission("USER:DELETE")).isFalse();
        assertThat(userPrincipal.hasPermission("NONEXISTENT:PERMISSION")).isFalse();
    }

    @Test
    @DisplayName("Should get role names correctly")
    void shouldGetRoleNamesCorrectly() {
        // When
        Set<String> roleNames = userPrincipal.getRoleNames();

        // Then
        assertThat(roleNames).hasSize(2);
        assertThat(roleNames).contains("USER", "ADMIN");
    }

    @Test
    @DisplayName("Should get permission names correctly")
    void shouldGetPermissionNamesCorrectly() {
        // When
        Set<String> permissionNames = userPrincipal.getPermissionNames();

        // Then
        assertThat(permissionNames).hasSize(3);
        assertThat(permissionNames).contains("USER:READ", "USER:WRITE", "ADMIN:ALL");
    }

    @Test
    @DisplayName("Should handle user with no roles")
    void shouldHandleUserWithNoRoles() {
        // Given
        User userWithoutRoles = new User("norolesuser", "password", "noroles@example.com");
        userWithoutRoles.setId(2L);
        userWithoutRoles.setEnabled(true);
        
        CustomUserPrincipal principalWithoutRoles = new CustomUserPrincipal(userWithoutRoles);

        // When
        Collection<? extends GrantedAuthority> authorities = principalWithoutRoles.getAuthorities();
        Set<String> roleNames = principalWithoutRoles.getRoleNames();
        Set<String> permissionNames = principalWithoutRoles.getPermissionNames();

        // Then
        assertThat(authorities).isEmpty();
        assertThat(roleNames).isEmpty();
        assertThat(permissionNames).isEmpty();
        assertThat(principalWithoutRoles.hasRole("USER")).isFalse();
        assertThat(principalWithoutRoles.hasPermission("USER:READ")).isFalse();
    }

    @Test
    @DisplayName("Should handle user with roles but no permissions")
    void shouldHandleUserWithRolesButNoPermissions() {
        // Given
        User user = new User("rolesonlyuser", "password", "rolesonly@example.com");
        user.setId(3L);
        user.setEnabled(true);
        
        Role emptyRole = new Role("EMPTY_ROLE", "Role with no permissions");
        emptyRole.setId(3L);
        user.addRole(emptyRole);
        
        CustomUserPrincipal principal = new CustomUserPrincipal(user);

        // When
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        Set<String> roleNames = principal.getRoleNames();
        Set<String> permissionNames = principal.getPermissionNames();

        // Then
        assertThat(authorities).hasSize(1); // Only the role authority
        assertThat(roleNames).containsExactly("EMPTY_ROLE");
        assertThat(permissionNames).isEmpty();
        assertThat(principal.hasRole("EMPTY_ROLE")).isTrue();
        assertThat(principal.hasPermission("ANY:PERMISSION")).isFalse();
    }

    @Test
    @DisplayName("Should handle disabled user correctly")
    void shouldHandleDisabledUserCorrectly() {
        // Given
        testUser.setEnabled(false);
        CustomUserPrincipal disabledPrincipal = new CustomUserPrincipal(testUser);

        // When & Then
        assertThat(disabledPrincipal.isEnabled()).isFalse();
        assertThat(disabledPrincipal.isAccountNonExpired()).isTrue();
        assertThat(disabledPrincipal.isAccountNonLocked()).isTrue();
        assertThat(disabledPrincipal.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should handle locked user correctly")
    void shouldHandleLockedUserCorrectly() {
        // Given
        testUser.setAccountNonLocked(false);
        CustomUserPrincipal lockedPrincipal = new CustomUserPrincipal(testUser);

        // When & Then
        assertThat(lockedPrincipal.isEnabled()).isTrue();
        assertThat(lockedPrincipal.isAccountNonExpired()).isTrue();
        assertThat(lockedPrincipal.isAccountNonLocked()).isFalse();
        assertThat(lockedPrincipal.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should handle expired credentials correctly")
    void shouldHandleExpiredCredentialsCorrectly() {
        // Given
        testUser.setCredentialsNonExpired(false);
        CustomUserPrincipal expiredCredentialsPrincipal = new CustomUserPrincipal(testUser);

        // When & Then
        assertThat(expiredCredentialsPrincipal.isEnabled()).isTrue();
        assertThat(expiredCredentialsPrincipal.isAccountNonExpired()).isTrue();
        assertThat(expiredCredentialsPrincipal.isAccountNonLocked()).isTrue();
        assertThat(expiredCredentialsPrincipal.isCredentialsNonExpired()).isFalse();
    }

    @Test
    @DisplayName("Should handle expired account correctly")
    void shouldHandleExpiredAccountCorrectly() {
        // Given
        testUser.setAccountNonExpired(false);
        CustomUserPrincipal expiredAccountPrincipal = new CustomUserPrincipal(testUser);

        // When & Then
        assertThat(expiredAccountPrincipal.isEnabled()).isTrue();
        assertThat(expiredAccountPrincipal.isAccountNonExpired()).isFalse();
        assertThat(expiredAccountPrincipal.isAccountNonLocked()).isTrue();
        assertThat(expiredAccountPrincipal.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
        // Given
        CustomUserPrincipal samePrincipal = new CustomUserPrincipal(testUser);
        
        User differentUser = new User("different", "password", "different@example.com");
        differentUser.setId(999L);
        CustomUserPrincipal differentPrincipal = new CustomUserPrincipal(differentUser);

        // When & Then
        assertThat(userPrincipal).isEqualTo(samePrincipal);
        assertThat(userPrincipal).isNotEqualTo(differentPrincipal);
        assertThat(userPrincipal).isNotEqualTo(null);
        assertThat(userPrincipal).isNotEqualTo("not a principal");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
        // Given
        CustomUserPrincipal samePrincipal = new CustomUserPrincipal(testUser);

        // When & Then
        assertThat(userPrincipal.hashCode()).isEqualTo(samePrincipal.hashCode());
        assertThat(userPrincipal.hashCode()).isEqualTo(testUser.getId().hashCode());
    }

    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
        // When
        String toString = userPrincipal.toString();

        // Then
        assertThat(toString).contains("CustomUserPrincipal");
        assertThat(toString).contains("id=" + testUser.getId());
        assertThat(toString).contains("username='" + testUser.getUsername() + "'");
        assertThat(toString).contains("email='" + testUser.getEmail() + "'");
        assertThat(toString).contains("enabled=" + testUser.isEnabled());
        assertThat(toString).contains("authoritiesCount=5");
    }

    /**
     * Creates a test user with roles and permissions for testing.
     */
    private User createTestUserWithRolesAndPermissions() {
        User user = new User("testuser", "encodedPassword", "test@example.com");
        user.setId(1L);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        // Create USER role with permissions
        Role userRole = new Role("USER", "Standard user role");
        userRole.setId(1L);

        Permission readPermission = new Permission("USER:READ", "Read user data", "user", "read");
        readPermission.setId(1L);
        
        Permission writePermission = new Permission("USER:WRITE", "Write user data", "user", "write");
        writePermission.setId(2L);

        userRole.addPermission(readPermission);
        userRole.addPermission(writePermission);

        // Create ADMIN role with permissions
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