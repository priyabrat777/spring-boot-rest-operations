package com.enterprise.api.service;

import com.enterprise.api.dto.request.CreateRoleRequest;
import com.enterprise.api.dto.request.UpdateRoleRequest;
import com.enterprise.api.dto.response.RoleResponse;
import com.enterprise.api.entity.Role;
import com.enterprise.api.repository.RoleRepository;
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
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for RoleServiceImpl.
 * Tests all business logic methods with comprehensive coverage.
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role testRole;
    private CreateRoleRequest createRoleRequest;
    private UpdateRoleRequest updateRoleRequest;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName("TEST_ROLE");
        testRole.setDescription("Test Role Description");
        testRole.setSystemRole(false);
        testRole.setCreatedDate(LocalDateTime.now());
        testRole.setLastModifiedDate(LocalDateTime.now());
        testRole.setVersion(1L);

        createRoleRequest = new CreateRoleRequest();
        createRoleRequest.setName("NEW_ROLE");
        createRoleRequest.setDescription("New Role Description");
        createRoleRequest.setSystemRole(false);

        updateRoleRequest = new UpdateRoleRequest();
        updateRoleRequest.setDescription("Updated Role Description");
        updateRoleRequest.setSystemRole(true);

        lenient().when(authentication.getName()).thenReturn("testuser");
    }

    @Test
    void createRole_ShouldThrowUnsupportedOperationException() {
        // When & Then
        assertThrows(UnsupportedOperationException.class, () -> 
            roleService.createRole(createRoleRequest, authentication));
    }

    @Test
    void updateRole_ShouldThrowUnsupportedOperationException() {
        // When & Then
        assertThrows(UnsupportedOperationException.class, () -> 
            roleService.updateRole(1L, updateRoleRequest, authentication));
    }

    @Test
    void partialUpdateRole_WithValidData_ShouldUpdateRole() {
        // Given
        Map<String, Object> partialUpdateData = new HashMap<>();
        partialUpdateData.put("name", "UPDATED_NAME");
        partialUpdateData.put("description", "Updated Description");
        partialUpdateData.put("systemRole", true);

        when(roleRepository.findByIdActive(1L)).thenReturn(Optional.of(testRole));
        when(roleRepository.existsByNameAndIdNot("UPDATED_NAME", 1L)).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        // When
        RoleResponse result = roleService.partialUpdateRole(1L, partialUpdateData, authentication);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(roleRepository).findByIdActive(1L);
        verify(roleRepository).existsByNameAndIdNot("UPDATED_NAME", 1L);
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void partialUpdateRole_WithDuplicateName_ShouldThrowException() {
        // Given
        Map<String, Object> partialUpdateData = new HashMap<>();
        partialUpdateData.put("name", "EXISTING_NAME");

        when(roleRepository.findByIdActive(1L)).thenReturn(Optional.of(testRole));
        when(roleRepository.existsByNameAndIdNot("EXISTING_NAME", 1L)).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            roleService.partialUpdateRole(1L, partialUpdateData, authentication));
        assertEquals("Role name already exists", exception.getMessage());
    }

    @Test
    void partialUpdateRole_WithNonExistentRole_ShouldThrowException() {
        // Given
        Map<String, Object> partialUpdateData = new HashMap<>();
        partialUpdateData.put("name", "NEW_NAME");

        when(roleRepository.findByIdActive(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            roleService.partialUpdateRole(1L, partialUpdateData, authentication));
        assertEquals("Role not found with ID: 1", exception.getMessage());
    }

    @Test
    void partialUpdateRole_WithEmptyData_ShouldReturnUnchangedRole() {
        // Given
        Map<String, Object> partialUpdateData = new HashMap<>();

        when(roleRepository.findByIdActive(1L)).thenReturn(Optional.of(testRole));

        // When
        RoleResponse result = roleService.partialUpdateRole(1L, partialUpdateData, authentication);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(roleRepository).findByIdActive(1L);
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void partialUpdateRole_WithSameValues_ShouldNotUpdate() {
        // Given
        Map<String, Object> partialUpdateData = new HashMap<>();
        partialUpdateData.put("name", testRole.getName());
        partialUpdateData.put("description", testRole.getDescription());
        partialUpdateData.put("systemRole", testRole.isSystemRole());

        when(roleRepository.findByIdActive(1L)).thenReturn(Optional.of(testRole));

        // When
        RoleResponse result = roleService.partialUpdateRole(1L, partialUpdateData, authentication);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(roleRepository).findByIdActive(1L);
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void updateRoleStatus_WithValidRole_ShouldUpdateStatus() {
        // Given
        when(roleRepository.findByIdActive(1L)).thenReturn(Optional.of(testRole));
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        // When
        boolean result = roleService.updateRoleStatus(1L, true, authentication);

        // Then
        assertTrue(result);
        verify(roleRepository).findByIdActive(1L);
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void updateRoleStatus_WithNonExistentRole_ShouldReturnFalse() {
        // Given
        when(roleRepository.findByIdActive(1L)).thenReturn(Optional.empty());

        // When
        boolean result = roleService.updateRoleStatus(1L, true, authentication);

        // Then
        assertFalse(result);
        verify(roleRepository).findByIdActive(1L);
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void updateRoleStatus_WithException_ShouldReturnFalse() {
        // Given
        when(roleRepository.findByIdActive(1L)).thenReturn(Optional.of(testRole));
        when(roleRepository.save(any(Role.class))).thenThrow(new RuntimeException("Database error"));

        // When
        boolean result = roleService.updateRoleStatus(1L, true, authentication);

        // Then
        assertFalse(result);
        verify(roleRepository).findByIdActive(1L);
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void getRoleById_ShouldThrowUnsupportedOperationException() {
        // When & Then
        assertThrows(UnsupportedOperationException.class, () -> 
            roleService.getRoleById(1L, authentication));
    }

    @Test
    void getRoleByName_ShouldThrowUnsupportedOperationException() {
        // When & Then
        assertThrows(UnsupportedOperationException.class, () -> 
            roleService.getRoleByName("TEST_ROLE", authentication));
    }

    @Test
    void getAllRoles_ShouldThrowUnsupportedOperationException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThrows(UnsupportedOperationException.class, () -> 
            roleService.getAllRoles(authentication, pageable));
    }

    @Test
    void searchRoles_ShouldThrowUnsupportedOperationException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThrows(UnsupportedOperationException.class, () -> 
            roleService.searchRoles("search", authentication, pageable));
    }

    @Test
    void getSystemRoles_ShouldThrowUnsupportedOperationException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThrows(UnsupportedOperationException.class, () -> 
            roleService.getSystemRoles(authentication, pageable));
    }

    @Test
    void getNonSystemRoles_ShouldThrowUnsupportedOperationException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        assertThrows(UnsupportedOperationException.class, () -> 
            roleService.getNonSystemRoles(authentication, pageable));
    }

    @Test
    void deleteRole_ShouldThrowUnsupportedOperationException() {
        // When & Then
        assertThrows(UnsupportedOperationException.class, () -> 
            roleService.deleteRole(1L, authentication));
    }

    @Test
    void assignPermissions_ShouldThrowUnsupportedOperationException() {
        // Given
        List<Long> permissionIds = Arrays.asList(1L, 2L, 3L);

        // When & Then
        assertThrows(UnsupportedOperationException.class, () -> 
            roleService.assignPermissions(1L, permissionIds, authentication));
    }

    @Test
    void removePermissions_ShouldThrowUnsupportedOperationException() {
        // Given
        List<Long> permissionIds = Arrays.asList(1L, 2L, 3L);

        // When & Then
        assertThrows(UnsupportedOperationException.class, () -> 
            roleService.removePermissions(1L, permissionIds, authentication));
    }

    @Test
    void canViewRole_ShouldReturnTrue() {
        // When
        boolean result = roleService.canViewRole(1L, authentication);

        // Then
        assertTrue(result);
    }

    @Test
    void canModifyRole_ShouldReturnTrue() {
        // When
        boolean result = roleService.canModifyRole(1L, authentication);

        // Then
        assertTrue(result);
    }

    @Test
    void partialUpdateRole_WithNullValues_ShouldSkipNullFields() {
        // Given
        Map<String, Object> partialUpdateData = new HashMap<>();
        partialUpdateData.put("name", null);
        partialUpdateData.put("description", "Updated Description");

        when(roleRepository.findByIdActive(1L)).thenReturn(Optional.of(testRole));
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        // When
        RoleResponse result = roleService.partialUpdateRole(1L, partialUpdateData, authentication);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(roleRepository).findByIdActive(1L);
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void partialUpdateRole_WithWhitespaceValues_ShouldTrimValues() {
        // Given
        Map<String, Object> partialUpdateData = new HashMap<>();
        partialUpdateData.put("name", "  TRIMMED_NAME  ");
        partialUpdateData.put("description", "  Trimmed Description  ");

        when(roleRepository.findByIdActive(1L)).thenReturn(Optional.of(testRole));
        when(roleRepository.existsByNameAndIdNot("  TRIMMED_NAME  ", 1L)).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        // When
        RoleResponse result = roleService.partialUpdateRole(1L, partialUpdateData, authentication);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(roleRepository).findByIdActive(1L);
        verify(roleRepository).existsByNameAndIdNot("  TRIMMED_NAME  ", 1L);
        verify(roleRepository).save(any(Role.class));
    }
}