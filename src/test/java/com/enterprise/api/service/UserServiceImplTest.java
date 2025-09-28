package com.enterprise.api.service;

import com.enterprise.api.dto.request.CreateUserRequest;
import com.enterprise.api.dto.request.UpdateUserRequest;
import com.enterprise.api.dto.request.ChangePasswordRequest;
import com.enterprise.api.dto.response.UserResponse;
import com.enterprise.api.entity.Permission;
import com.enterprise.api.entity.Role;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.RoleRepository;
import com.enterprise.api.repository.UserRepository;
import com.enterprise.api.security.CustomUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserServiceImpl.
 * Tests user management operations with RBAC-based access control.
 * 
 * Requirements addressed:
 * - 4.6: Role-based data filtering testing
 * - 4.7: User account management testing
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User currentUser;
    private User targetUser;
    private Role userRole;
    private Role adminRole;
    private Permission userReadPermission;
    private Permission userUpdatePermission;
    private CustomUserPrincipal userPrincipal;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Create permissions
        userReadPermission = new Permission("USER_READ", "user", "read");
        userReadPermission.setId(1L);

        userUpdatePermission = new Permission("USER_UPDATE", "user", "update");
        userUpdatePermission.setId(2L);
        
        Permission userCreatePermission = new Permission("USER_CREATE", "user", "create");
        userCreatePermission.setId(3L);

        // Create roles
        userRole = new Role("USER");
        userRole.setId(1L);
        userRole.addPermission(userReadPermission);

        adminRole = new Role("ADMIN");
        adminRole.setId(2L);
        adminRole.addPermission(userReadPermission);
        adminRole.addPermission(userUpdatePermission);
        adminRole.addPermission(userCreatePermission);

        // Create current user (admin)
        currentUser = new User("admin", "encodedPassword", "admin@example.com");
        currentUser.setId(1L);
        currentUser.setEnabled(true);
        currentUser.addRole(adminRole);

        // Create target user
        targetUser = new User("testuser", "encodedPassword", "test@example.com");
        targetUser.setId(2L);
        targetUser.setFirstName("Test");
        targetUser.setLastName("User");
        targetUser.setEnabled(true);
        targetUser.addRole(userRole);

        // Create authentication
        userPrincipal = new CustomUserPrincipal(currentUser);
        authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, userPrincipal.getAuthorities());
    }

    @Test
    void createUser_WithValidRequest_ShouldReturnUserResponse() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest("newuser", "StrongPass123!", "new@example.com");
        request.setFirstName("New");
        request.setLastName("User");
        request.setRoleIds(Arrays.asList(1L));

        User savedUser = new User("newuser", "encodedPassword", "new@example.com");
        savedUser.setId(3L);
        savedUser.setFirstName("New");
        savedUser.setLastName("User");
        savedUser.addRole(userRole);

        when(userRepository.existsByUsernameActive("newuser")).thenReturn(false);
        when(userRepository.existsByEmailActive("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass123!")).thenReturn("encodedPassword");
        when(roleRepository.findByIdActive(1L)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse response = userService.createUser(request, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(3L, response.getId());
        assertEquals("newuser", response.getUsername());
        assertEquals("new@example.com", response.getEmail());
        assertEquals("New", response.getFirstName());
        assertEquals("User", response.getLastName());
        assertTrue(response.isEnabled());
        assertEquals(1, response.getRoles().size());

        verify(userRepository).existsByUsernameActive("newuser");
        verify(userRepository).existsByEmailActive("new@example.com");
        verify(passwordEncoder).encode("StrongPass123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WithoutPermission_ShouldThrowAccessDeniedException() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest("newuser", "StrongPass123!", "new@example.com");
        
        // Create user without admin permissions
        User regularUser = new User("user", "password", "user@example.com");
        regularUser.setId(3L);
        regularUser.addRole(userRole);
        
        CustomUserPrincipal regularUserPrincipal = new CustomUserPrincipal(regularUser);
        Authentication regularAuth = new UsernamePasswordAuthenticationToken(
                regularUserPrincipal, null, regularUserPrincipal.getAuthorities());

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> userService.createUser(request, regularAuth));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_WithExistingUsername_ShouldThrowIllegalArgumentException() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest("existinguser", "StrongPass123!", "new@example.com");

        when(userRepository.existsByUsernameActive("existinguser")).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(request, authentication));

        verify(userRepository).existsByUsernameActive("existinguser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_WithExistingEmail_ShouldThrowIllegalArgumentException() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest("newuser", "StrongPass123!", "existing@example.com");

        when(userRepository.existsByUsernameActive("newuser")).thenReturn(false);
        when(userRepository.existsByEmailActive("existing@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(request, authentication));

        verify(userRepository).existsByEmailActive("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_WithWeakPassword_ShouldThrowIllegalArgumentException() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest("newuser", "weak", "new@example.com");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(request, authentication));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_WithValidRequest_ShouldReturnUpdatedUserResponse() {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");
        request.setEmail("updated@example.com");

        User updatedUser = new User(targetUser.getUsername(), targetUser.getPassword(), "updated@example.com");
        updatedUser.setId(targetUser.getId());
        updatedUser.setFirstName("Updated");
        updatedUser.setLastName("Name");
        updatedUser.setEnabled(targetUser.isEnabled());
        updatedUser.setRoles(targetUser.getRoles());

        when(userRepository.findByIdActive(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.findByEmailActive("updated@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        UserResponse response = userService.updateUser(2L, request, authentication);

        // Assert
        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("Updated", response.getFirstName());
        assertEquals("Name", response.getLastName());
        assertEquals("updated@example.com", response.getEmail());

        verify(userRepository).findByIdActive(2L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_WithoutPermission_ShouldThrowAccessDeniedException() {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Updated");

        // Create user without admin permissions trying to update another user
        User regularUser = new User("user", "password", "user@example.com");
        regularUser.setId(3L);
        regularUser.addRole(userRole);
        
        CustomUserPrincipal regularUserPrincipal = new CustomUserPrincipal(regularUser);
        Authentication regularAuth = new UsernamePasswordAuthenticationToken(
                regularUserPrincipal, null, regularUserPrincipal.getAuthorities());

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> userService.updateUser(2L, request, regularAuth));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_NonExistentUser_ShouldThrowIllegalArgumentException() {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Updated");

        when(userRepository.findByIdActive(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(999L, request, authentication));

        verify(userRepository).findByIdActive(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_WithPermission_ShouldReturnUserResponse() {
        // Arrange
        when(userRepository.findByIdWithRolesAndPermissions(2L)).thenReturn(Optional.of(targetUser));

        // Act
        Optional<UserResponse> response = userService.getUserById(2L, authentication);

        // Assert
        assertTrue(response.isPresent());
        assertEquals(2L, response.get().getId());
        assertEquals("testuser", response.get().getUsername());

        verify(userRepository).findByIdWithRolesAndPermissions(2L);
    }

    @Test
    void getUserById_WithoutPermission_ShouldReturnEmpty() {
        // Arrange
        User regularUser = new User("user", "password", "user@example.com");
        regularUser.setId(3L);
        regularUser.addRole(userRole);
        
        CustomUserPrincipal regularUserPrincipal = new CustomUserPrincipal(regularUser);
        Authentication regularAuth = new UsernamePasswordAuthenticationToken(
                regularUserPrincipal, null, regularUserPrincipal.getAuthorities());

        // Act
        Optional<UserResponse> response = userService.getUserById(2L, regularAuth);

        // Assert
        assertFalse(response.isPresent());

        verify(userRepository, never()).findByIdWithRolesAndPermissions(any());
    }

    @Test
    void getUserById_SelfAccess_ShouldReturnUserResponse() {
        // Arrange
        when(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(Optional.of(currentUser));

        // Act
        Optional<UserResponse> response = userService.getUserById(1L, authentication);

        // Assert
        assertTrue(response.isPresent());
        assertEquals(1L, response.get().getId());
        assertEquals("admin", response.get().getUsername());

        verify(userRepository).findByIdWithRolesAndPermissions(1L);
    }

    @Test
    void getAllUsers_WithPermission_ShouldReturnPageOfUsers() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(currentUser, targetUser);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());

        when(userRepository.findAllActive(pageable)).thenReturn(userPage);

        // Act
        Page<UserResponse> response = userService.getAllUsers(authentication, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals(2, response.getTotalElements());

        verify(userRepository).findAllActive(pageable);
    }

    @Test
    void getAllUsers_WithoutPermission_ShouldThrowAccessDeniedException() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        
        User regularUser = new User("user", "password", "user@example.com");
        regularUser.setId(3L);
        regularUser.addRole(userRole);
        
        CustomUserPrincipal regularUserPrincipal = new CustomUserPrincipal(regularUser);
        Authentication regularAuth = new UsernamePasswordAuthenticationToken(
                regularUserPrincipal, null, regularUserPrincipal.getAuthorities());

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> userService.getAllUsers(regularAuth, pageable));

        verify(userRepository, never()).findAllActive(any());
    }

    @Test
    void deleteUser_WithPermission_ShouldReturnTrue() {
        // Arrange
        when(userRepository.findByIdActive(2L)).thenReturn(Optional.of(targetUser));

        // Act
        boolean result = userService.deleteUser(2L, authentication);

        // Assert
        assertTrue(result);

        verify(userRepository).findByIdActive(2L);
        verify(userRepository).softDelete(2L);
    }

    @Test
    void deleteUser_SelfDeletion_ShouldThrowIllegalArgumentException() {
        // Arrange
        when(userRepository.findByIdActive(1L)).thenReturn(Optional.of(currentUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.deleteUser(1L, authentication));

        verify(userRepository).findByIdActive(1L);
        verify(userRepository, never()).softDelete(any());
    }

    @Test
    void changePassword_SelfPasswordChange_ShouldReturnTrue() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest("currentPass", "NewStrongPass123!", "NewStrongPass123!");
        
        when(userRepository.findByIdActive(1L)).thenReturn(Optional.of(currentUser));
        when(passwordEncoder.matches("currentPass", currentUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewStrongPass123!")).thenReturn("newEncodedPassword");
        when(userRepository.updatePassword(1L, "newEncodedPassword")).thenReturn(1);

        // Act
        boolean result = userService.changePassword(1L, request, authentication);

        // Assert
        assertTrue(result);

        verify(userRepository).findByIdActive(1L);
        verify(passwordEncoder).matches("currentPass", currentUser.getPassword());
        verify(passwordEncoder).encode("NewStrongPass123!");
        verify(userRepository).updatePassword(1L, "newEncodedPassword");
    }

    @Test
    void changePassword_IncorrectCurrentPassword_ShouldThrowIllegalArgumentException() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPass", "NewStrongPass123!", "NewStrongPass123!");
        
        when(userRepository.findByIdActive(1L)).thenReturn(Optional.of(currentUser));
        when(passwordEncoder.matches("wrongPass", currentUser.getPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(1L, request, authentication));

        verify(passwordEncoder).matches("wrongPass", currentUser.getPassword());
        verify(userRepository, never()).updatePassword(any(), any());
    }

    @Test
    void changePassword_PasswordMismatch_ShouldThrowIllegalArgumentException() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest("currentPass", "NewStrongPass123!", "DifferentPass123!");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(1L, request, authentication));

        verify(userRepository, never()).updatePassword(any(), any());
    }

    @Test
    void changePassword_WeakNewPassword_ShouldThrowIllegalArgumentException() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest("currentPass", "weak", "weak");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(1L, request, authentication));

        verify(userRepository, never()).updatePassword(any(), any());
    }

    @Test
    void validatePassword_WithStrongPassword_ShouldReturnTrue() {
        // Act
        boolean result = userService.validatePassword("StrongPass123!");

        // Assert
        assertTrue(result);
    }

    @Test
    void validatePassword_WithWeakPassword_ShouldReturnFalse() {
        // Act
        boolean result = userService.validatePassword("weak");

        // Assert
        assertFalse(result);
    }

    @Test
    void validatePassword_WithNullPassword_ShouldReturnFalse() {
        // Act
        boolean result = userService.validatePassword(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void encodePassword_ShouldReturnEncodedPassword() {
        // Arrange
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        // Act
        String result = userService.encodePassword("password");

        // Assert
        assertEquals("encodedPassword", result);
        verify(passwordEncoder).encode("password");
    }

    @Test
    void matchesPassword_WithMatchingPasswords_ShouldReturnTrue() {
        // Arrange
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        // Act
        boolean result = userService.matchesPassword("password", "encodedPassword");

        // Assert
        assertTrue(result);
        verify(passwordEncoder).matches("password", "encodedPassword");
    }

    @Test
    void canViewUser_SelfAccess_ShouldReturnTrue() {
        // Act
        boolean result = userService.canViewUser(1L, authentication);

        // Assert
        assertTrue(result);
    }

    @Test
    void canModifyUser_SelfAccess_ShouldReturnTrue() {
        // Act
        boolean result = userService.canModifyUser(1L, authentication);

        // Assert
        assertTrue(result);
    }

    @Test
    void canViewUser_WithPermission_ShouldReturnTrue() {
        // Act
        boolean result = userService.canViewUser(2L, authentication);

        // Assert
        assertTrue(result);
    }

    @Test
    void canModifyUser_WithPermission_ShouldReturnTrue() {
        // Act
        boolean result = userService.canModifyUser(2L, authentication);

        // Assert
        assertTrue(result);
    }
}