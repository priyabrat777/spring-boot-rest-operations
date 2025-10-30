package com.enterprise.api.repository;

import com.enterprise.api.entity.Permission;
import com.enterprise.api.entity.Role;
import com.enterprise.api.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PermissionRepository.
 * Tests permission-specific repository operations including role and user relationships.
 * 
 * Requirements addressed:
 * - 5.1: CRUD operations, custom queries, and specifications
 * - 5.2: @Query annotations and method name derivation
 * - 5.3: Pageable and Sort parameters
 * - 5.4: Batch operations
 * - 3.6: Soft delete and active record filtering
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional
@Rollback
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PermissionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PermissionRepository permissionRepository;

    private Permission readUserPermission;
    private Permission writeUserPermission;
    private Permission readFilePermission;
    private Permission systemPermission;
    private Permission deletedPermission;
    private Role adminRole;
    private Role userRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create permissions first and persist them
        readUserPermission = new Permission("READ_USER", "Permission to read user data", "user", "read");
        readUserPermission.setSystemPermission(false);

        writeUserPermission = new Permission("WRITE_USER", "Permission to write user data", "user", "write");
        writeUserPermission.setSystemPermission(false);

        readFilePermission = new Permission("READ_FILE", "Permission to read file data", "file", "read");
        readFilePermission.setSystemPermission(false);

        systemPermission = new Permission("SYSTEM_ACCESS", "System access permission", "system", "access");
        systemPermission.setSystemPermission(true);

        deletedPermission = new Permission("DELETED_PERMISSION", "Deleted permission", "deleted", "action");
        deletedPermission.setSystemPermission(false);
        deletedPermission.setDeleted(true);

        // Persist permissions first
        entityManager.persistAndFlush(readUserPermission);
        entityManager.persistAndFlush(writeUserPermission);
        entityManager.persistAndFlush(readFilePermission);
        entityManager.persistAndFlush(systemPermission);
        entityManager.persistAndFlush(deletedPermission);

        // Create roles and establish relationships
        adminRole = new Role("ADMIN", "Administrator role");
        adminRole.addPermission(readUserPermission);
        adminRole.addPermission(writeUserPermission);
        adminRole.addPermission(systemPermission);

        userRole = new Role("USER", "Regular user role");
        userRole.addPermission(readUserPermission);
        userRole.addPermission(readFilePermission);

        // Persist roles
        entityManager.persistAndFlush(adminRole);
        entityManager.persistAndFlush(userRole);

        // Create user and establish relationships
        testUser = new User("testuser", "password123", "test@example.com");
        testUser.setEnabled(true);
        testUser.addRole(adminRole);
        testUser.addRole(userRole);

        // Persist user
        entityManager.persistAndFlush(testUser);
        entityManager.clear();
    }

    @Test
    void findByNameActive_ShouldReturnActivePermission() {
        // When
        Optional<Permission> foundPermission = permissionRepository.findByNameActive("READ_USER");

        // Then
        assertThat(foundPermission).isPresent();
        assertThat(foundPermission.get().getName()).isEqualTo("READ_USER");
        assertThat(foundPermission.get().isDeleted()).isFalse();
    }

    @Test
    void findByNameActive_ShouldNotReturnDeletedPermission() {
        // When
        Optional<Permission> foundPermission = permissionRepository.findByNameActive("DELETED_PERMISSION");

        // Then
        assertThat(foundPermission).isEmpty();
    }

    @Test
    void findByResourceAndActionActive_ShouldReturnPermission() {
        // When
        Optional<Permission> foundPermission = permissionRepository.findByResourceAndActionActive("user", "read");

        // Then
        assertThat(foundPermission).isPresent();
        assertThat(foundPermission.get().getName()).isEqualTo("READ_USER");
        assertThat(foundPermission.get().getResource()).isEqualTo("user");
        assertThat(foundPermission.get().getAction()).isEqualTo("read");
    }

    @Test
    void existsByNameActive_ShouldReturnTrueForActivePermission() {
        // When
        boolean exists = permissionRepository.existsByNameActive("READ_USER");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByNameActive_ShouldReturnFalseForDeletedPermission() {
        // When
        boolean exists = permissionRepository.existsByNameActive("DELETED_PERMISSION");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void existsByResourceAndActionActive_ShouldReturnTrueForActivePermission() {
        // When
        boolean exists = permissionRepository.existsByResourceAndActionActive("user", "read");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByResourceAndActionActive_ShouldReturnFalseForDeletedPermission() {
        // When
        boolean exists = permissionRepository.existsByResourceAndActionActive("deleted", "action");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void findByResourceActive_ShouldReturnPermissionsForResource() {
        // When
        List<Permission> userPermissions = permissionRepository.findByResourceActive("user");

        // Then
        assertThat(userPermissions).hasSize(2);
        assertThat(userPermissions).extracting(Permission::getName)
                .containsExactlyInAnyOrder("READ_USER", "WRITE_USER");
    }

    @Test
    void findByActionActive_ShouldReturnPermissionsForAction() {
        // When
        List<Permission> readPermissions = permissionRepository.findByActionActive("read");

        // Then
        assertThat(readPermissions).hasSize(2);
        assertThat(readPermissions).extracting(Permission::getName)
                .containsExactlyInAnyOrder("READ_USER", "READ_FILE");
    }

    @Test
    void findSystemPermissionsActive_ShouldReturnOnlySystemPermissions() {
        // When
        List<Permission> systemPermissions = permissionRepository.findSystemPermissionsActive();

        // Then
        assertThat(systemPermissions).hasSize(1);
        assertThat(systemPermissions.get(0).getName()).isEqualTo("SYSTEM_ACCESS");
        assertThat(systemPermissions.get(0).isSystemPermission()).isTrue();
    }

    @Test
    void findNonSystemPermissionsActive_ShouldReturnOnlyNonSystemPermissions() {
        // When
        List<Permission> nonSystemPermissions = permissionRepository.findNonSystemPermissionsActive();

        // Then
        assertThat(nonSystemPermissions).hasSize(3);
        assertThat(nonSystemPermissions).extracting(Permission::getName)
                .containsExactlyInAnyOrder("READ_USER", "WRITE_USER", "READ_FILE");
        assertThat(nonSystemPermissions).allMatch(permission -> !permission.isSystemPermission());
    }

    @Test
    void findByNameContainingIgnoreCaseActive_ShouldReturnMatchingPermissions() {
        // When
        List<Permission> matchingPermissions = permissionRepository.findByNameContainingIgnoreCaseActive("read");

        // Then
        assertThat(matchingPermissions).hasSize(2);
        assertThat(matchingPermissions).extracting(Permission::getName)
                .containsExactlyInAnyOrder("READ_USER", "READ_FILE");
    }

    @Test
    void findByDescriptionContainingIgnoreCaseActive_ShouldReturnMatchingPermissions() {
        // When
        List<Permission> matchingPermissions = permissionRepository.findByDescriptionContainingIgnoreCaseActive("user data");

        // Then
        assertThat(matchingPermissions).hasSize(2);
        assertThat(matchingPermissions).extracting(Permission::getName)
                .containsExactlyInAnyOrder("READ_USER", "WRITE_USER");
    }

    @Test
    void findByRoleIdActive_ShouldReturnPermissionsForRole() {
        // When
        List<Permission> adminPermissions = permissionRepository.findByRoleIdActive(adminRole.getId());
        List<Permission> userPermissions = permissionRepository.findByRoleIdActive(userRole.getId());

        // Then
        assertThat(adminPermissions).hasSize(3);
        assertThat(adminPermissions).extracting(Permission::getName)
                .containsExactlyInAnyOrder("READ_USER", "WRITE_USER", "SYSTEM_ACCESS");

        assertThat(userPermissions).hasSize(2);
        assertThat(userPermissions).extracting(Permission::getName)
                .containsExactlyInAnyOrder("READ_USER", "READ_FILE");
    }

    @Test
    void findByRoleNameActive_ShouldReturnPermissionsForRole() {
        // When
        List<Permission> adminPermissions = permissionRepository.findByRoleNameActive("ADMIN");
        List<Permission> userPermissions = permissionRepository.findByRoleNameActive("USER");

        // Then
        assertThat(adminPermissions).hasSize(3);
        assertThat(adminPermissions).extracting(Permission::getName)
                .containsExactlyInAnyOrder("READ_USER", "WRITE_USER", "SYSTEM_ACCESS");

        assertThat(userPermissions).hasSize(2);
        assertThat(userPermissions).extracting(Permission::getName)
                .containsExactlyInAnyOrder("READ_USER", "READ_FILE");
    }

    @Test
    void findByUserIdActive_ShouldReturnPermissionsForUser() {
        // When
        List<Permission> userPermissions = permissionRepository.findByUserIdActive(testUser.getId());

        // Then
        assertThat(userPermissions).hasSize(4); // All permissions from both roles (READ_USER appears in both but should be distinct)
        assertThat(userPermissions).extracting(Permission::getName)
                .containsExactlyInAnyOrder("READ_USER", "WRITE_USER", "SYSTEM_ACCESS", "READ_FILE");
    }

    @Test
    void findByUsernameActive_ShouldReturnPermissionsForUser() {
        // When
        List<Permission> userPermissions = permissionRepository.findByUsernameActive("testuser");

        // Then
        assertThat(userPermissions).hasSize(4);
        assertThat(userPermissions).extracting(Permission::getName)
                .containsExactlyInAnyOrder("READ_USER", "WRITE_USER", "SYSTEM_ACCESS", "READ_FILE");
    }

    @Test
    void findByCreatedDateAfterActive_ShouldReturnRecentPermissions() {
        // Given
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        // When
        List<Permission> recentPermissions = permissionRepository.findByCreatedDateAfterActive(yesterday);

        // Then
        assertThat(recentPermissions).hasSize(4); // All active permissions created today
        assertThat(recentPermissions).extracting(Permission::getName)
                .containsExactlyInAnyOrder("READ_USER", "WRITE_USER", "READ_FILE", "SYSTEM_ACCESS");
    }

    @Test
    void findPermissionsWithoutRoles_ShouldReturnUnassignedPermissions() {
        // Given - Create a permission without roles
        Permission unassignedPermission = new Permission("UNASSIGNED", "Unassigned permission", "test", "action");
        unassignedPermission.setSystemPermission(false);
        entityManager.persistAndFlush(unassignedPermission);
        entityManager.clear();

        // When
        List<Permission> unassignedPermissions = permissionRepository.findPermissionsWithoutRoles();

        // Then
        assertThat(unassignedPermissions).hasSize(1);
        assertThat(unassignedPermissions.get(0).getName()).isEqualTo("UNASSIGNED");
    }

    @Test
    void findPermissionsWithManyRoles_ShouldReturnPermissionsWithMultipleRoles() {
        // When
        List<Permission> permissionsWithManyRoles = permissionRepository.findPermissionsWithManyRoles(2);

        // Then
        assertThat(permissionsWithManyRoles).hasSize(1); // Only READ_USER is assigned to both roles
        assertThat(permissionsWithManyRoles.get(0).getName()).isEqualTo("READ_USER");
    }

    @Test
    void findDistinctResourcesActive_ShouldReturnUniqueResources() {
        // When
        List<String> resources = permissionRepository.findDistinctResourcesActive();

        // Then
        assertThat(resources).hasSize(3);
        assertThat(resources).containsExactlyInAnyOrder("file", "system", "user");
    }

    @Test
    void findDistinctActionsActive_ShouldReturnUniqueActions() {
        // When
        List<String> actions = permissionRepository.findDistinctActionsActive();

        // Then
        assertThat(actions).hasSize(3);
        assertThat(actions).containsExactlyInAnyOrder("access", "read", "write");
    }

    @Test
    void findDistinctActionsByResourceActive_ShouldReturnActionsForResource() {
        // When
        List<String> userActions = permissionRepository.findDistinctActionsByResourceActive("user");

        // Then
        assertThat(userActions).hasSize(2);
        assertThat(userActions).containsExactlyInAnyOrder("read", "write");
    }

    @Test
    void countBySystemPermissionActive_ShouldReturnCorrectCount() {
        // When
        long systemPermissionCount = permissionRepository.countBySystemPermissionActive(true);
        long nonSystemPermissionCount = permissionRepository.countBySystemPermissionActive(false);

        // Then
        assertThat(systemPermissionCount).isEqualTo(1);
        assertThat(nonSystemPermissionCount).isEqualTo(3);
    }

    @Test
    void countByResourceActive_ShouldReturnCorrectCount() {
        // When
        long userResourceCount = permissionRepository.countByResourceActive("user");
        long fileResourceCount = permissionRepository.countByResourceActive("file");

        // Then
        assertThat(userResourceCount).isEqualTo(2);
        assertThat(fileResourceCount).isEqualTo(1);
    }

    @Test
    void countByRoleIdActive_ShouldReturnCorrectCount() {
        // When
        long adminPermissionCount = permissionRepository.countByRoleIdActive(adminRole.getId());
        long userPermissionCount = permissionRepository.countByRoleIdActive(userRole.getId());

        // Then
        assertThat(adminPermissionCount).isEqualTo(3);
        assertThat(userPermissionCount).isEqualTo(2);
    }

    @Test
    void updateSystemPermissionFlag_ShouldUpdateFlag() {
        // When
        int affectedRows = permissionRepository.updateSystemPermissionFlag(readUserPermission.getId(), true);

        // Then
        assertThat(affectedRows).isEqualTo(1);
        
        entityManager.clear();
        Permission updatedPermission = entityManager.find(Permission.class, readUserPermission.getId());
        assertThat(updatedPermission.isSystemPermission()).isTrue();
        assertThat(updatedPermission.getLastModifiedDate()).isNotNull();
    }

    @Test
    void updateDescription_ShouldUpdateDescription() {
        // Given
        String newDescription = "Updated permission description";

        // When
        int affectedRows = permissionRepository.updateDescription(readUserPermission.getId(), newDescription);

        // Then
        assertThat(affectedRows).isEqualTo(1);
        
        entityManager.clear();
        Permission updatedPermission = entityManager.find(Permission.class, readUserPermission.getId());
        assertThat(updatedPermission.getDescription()).isEqualTo(newDescription);
    }

    @Test
    void batchUpdateSystemPermissionFlag_ShouldUpdateMultiplePermissions() {
        // Given
        List<Long> permissionIds = Arrays.asList(readUserPermission.getId(), writeUserPermission.getId());

        // When
        int affectedRows = permissionRepository.batchUpdateSystemPermissionFlag(permissionIds, true);

        // Then
        assertThat(affectedRows).isEqualTo(2);
        
        entityManager.clear();
        Permission updatedReadPermission = entityManager.find(Permission.class, readUserPermission.getId());
        Permission updatedWritePermission = entityManager.find(Permission.class, writeUserPermission.getId());
        
        assertThat(updatedReadPermission.isSystemPermission()).isTrue();
        assertThat(updatedWritePermission.isSystemPermission()).isTrue();
    }

    @Test
    void findUnusedNonSystemPermissions_ShouldReturnUnusedPermissions() {
        // Given - Create a permission without roles
        Permission unusedPermission = new Permission("UNUSED", "Unused permission", "unused", "action");
        unusedPermission.setSystemPermission(false);
        entityManager.persistAndFlush(unusedPermission);
        entityManager.clear();

        // When
        List<Permission> unusedPermissions = permissionRepository.findUnusedNonSystemPermissions();

        // Then
        assertThat(unusedPermissions).hasSize(1);
        assertThat(unusedPermissions.get(0).getName()).isEqualTo("UNUSED");
    }

    @Test
    void findAllActiveOrderByResourceAndAction_ShouldReturnOrderedPermissions() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Permission> orderedPermissions = permissionRepository.findAllActiveOrderByResourceAndAction(pageable);

        // Then
        assertThat(orderedPermissions.getTotalElements()).isEqualTo(4);
        // Should be ordered by resource, then action
        List<Permission> content = orderedPermissions.getContent();
        assertThat(content.get(0).getResource()).isEqualTo("file");
        assertThat(content.get(1).getResource()).isEqualTo("system");
        assertThat(content.get(2).getResource()).isEqualTo("user");
        assertThat(content.get(3).getResource()).isEqualTo("user");
    }

    @Test
    void findBySystemPermissionActiveWithPagination_ShouldReturnPagedPermissions() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Permission> systemPermissionsPage = permissionRepository.findBySystemPermissionActive(true, pageable);
        Page<Permission> nonSystemPermissionsPage = permissionRepository.findBySystemPermissionActive(false, pageable);

        // Then
        assertThat(systemPermissionsPage.getTotalElements()).isEqualTo(1);
        assertThat(systemPermissionsPage.getContent().get(0).getName()).isEqualTo("SYSTEM_ACCESS");

        assertThat(nonSystemPermissionsPage.getTotalElements()).isEqualTo(3);
        assertThat(nonSystemPermissionsPage.getContent()).extracting(Permission::getName)
                .containsExactlyInAnyOrder("READ_USER", "WRITE_USER", "READ_FILE");
    }

    @Test
    void findByResourceActiveWithPagination_ShouldReturnPagedPermissions() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Permission> userResourcePage = permissionRepository.findByResourceActive("user", pageable);

        // Then
        assertThat(userResourcePage.getTotalElements()).isEqualTo(2);
        assertThat(userResourcePage.getContent()).extracting(Permission::getName)
                .containsExactlyInAnyOrder("READ_USER", "WRITE_USER");
    }
}