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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RoleRepository.
 * Tests role-specific repository operations including user and permission relationships.
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RoleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoleRepository roleRepository;



    private Role adminRole;
    private Role userRole;
    private Role systemRole;
    private Role deletedRole;
    private User testUser;
    private Permission readPermission;
    private Permission writePermission;

    @BeforeEach
    void setUp() {
        // Create permissions with unique names and resource/action combinations to avoid constraint violations
        // Use only uppercase letters and underscores to match validation pattern
        long timestamp = System.currentTimeMillis();
        long threadId = Thread.currentThread().getId();
        int random = (int) (Math.random() * 10000);
        String uniqueSuffix = "T" + threadId + "R" + random + "TS" + (timestamp % 100000);
        // Remove any numbers and keep only letters to match validation pattern
        uniqueSuffix = uniqueSuffix.replaceAll("[0-9]", "A");
        if (uniqueSuffix.length() > 10) {
            uniqueSuffix = uniqueSuffix.substring(0, 10);
        }
        
        readPermission = new Permission("READ_USER_" + uniqueSuffix, "user_" + uniqueSuffix.toLowerCase(), "read");
        readPermission.setDescription("Permission to read user data");
        
        writePermission = new Permission("WRITE_USER_" + uniqueSuffix, "user_" + uniqueSuffix.toLowerCase(), "write");
        writePermission.setDescription("Permission to write user data");
        
        entityManager.persistAndFlush(readPermission);
        entityManager.persistAndFlush(writePermission);

        // Create roles with unique names
        adminRole = new Role("ADMIN_" + uniqueSuffix, "Administrator role");
        adminRole.setSystemRole(false);
        adminRole.addPermission(readPermission);
        adminRole.addPermission(writePermission);

        userRole = new Role("USER_" + uniqueSuffix, "Regular user role");
        userRole.setSystemRole(false);
        userRole.addPermission(readPermission);

        systemRole = new Role("SYSTEM_" + uniqueSuffix, "System role");
        systemRole.setSystemRole(true);
        systemRole.addPermission(readPermission);
        systemRole.addPermission(writePermission);

        deletedRole = new Role("DELETED_ROLE_" + uniqueSuffix, "Deleted role");
        deletedRole.setSystemRole(false);
        deletedRole.setDeleted(true);

        // Create user with unique name
        testUser = new User("testuser_" + uniqueSuffix.toLowerCase(), "password123", "test_" + uniqueSuffix.toLowerCase() + "@example.com");
        testUser.setEnabled(true);
        testUser.addRole(adminRole);
        testUser.addRole(userRole);

        // Persist entities
        entityManager.persistAndFlush(adminRole);
        entityManager.persistAndFlush(userRole);
        entityManager.persistAndFlush(systemRole);
        entityManager.persistAndFlush(deletedRole);
        entityManager.persistAndFlush(testUser);
        entityManager.clear();
    }

    @Test
    void findByNameActive_ShouldReturnActiveRole() {
        // When
        Optional<Role> foundRole = roleRepository.findByNameActive(adminRole.getName());

        // Then
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getName()).isEqualTo(adminRole.getName());
        assertThat(foundRole.get().isDeleted()).isFalse();
    }

    @Test
    void findByNameActive_ShouldNotReturnDeletedRole() {
        // When
        Optional<Role> foundRole = roleRepository.findByNameActive(deletedRole.getName());

        // Then
        assertThat(foundRole).isEmpty();
    }

    @Test
    void existsByNameActive_ShouldReturnTrueForActiveRole() {
        // When
        boolean exists = roleRepository.existsByNameActive(adminRole.getName());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByNameActive_ShouldReturnFalseForDeletedRole() {
        // When
        boolean exists = roleRepository.existsByNameActive(deletedRole.getName());

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void findSystemRolesActive_ShouldReturnOnlySystemRoles() {
        // When
        List<Role> systemRoles = roleRepository.findSystemRolesActive();

        // Then
        assertThat(systemRoles).hasSize(1);
        assertThat(systemRoles.get(0).getName()).isEqualTo(systemRole.getName());
        assertThat(systemRoles.get(0).isSystemRole()).isTrue();
    }

    @Test
    void findNonSystemRolesActive_ShouldReturnOnlyNonSystemRoles() {
        // When
        List<Role> nonSystemRoles = roleRepository.findNonSystemRolesActive();

        // Then
        assertThat(nonSystemRoles).hasSize(2);
        assertThat(nonSystemRoles).extracting(Role::getName)
                .containsExactlyInAnyOrder(adminRole.getName(), userRole.getName());
        assertThat(nonSystemRoles).allMatch(role -> !role.isSystemRole());
    }

    @Test
    void findByNameContainingIgnoreCaseActive_ShouldReturnMatchingRoles() {
        // When
        List<Role> matchingRoles = roleRepository.findByNameContainingIgnoreCaseActive("admin");

        // Then
        assertThat(matchingRoles).hasSize(1);
        assertThat(matchingRoles.get(0).getName()).isEqualTo(adminRole.getName());
    }

    @Test
    void findByDescriptionContainingIgnoreCaseActive_ShouldReturnMatchingRoles() {
        // When
        List<Role> matchingRoles = roleRepository.findByDescriptionContainingIgnoreCaseActive("administrator");

        // Then
        assertThat(matchingRoles).hasSize(1);
        assertThat(matchingRoles.get(0).getName()).isEqualTo(adminRole.getName());
    }

    @Test
    void findByUserIdActive_ShouldReturnUserRoles() {
        // When
        List<Role> userRoles = roleRepository.findByUserIdActive(testUser.getId());

        // Then
        assertThat(userRoles).hasSize(2);
        assertThat(userRoles).extracting(Role::getName)
                .containsExactlyInAnyOrder(adminRole.getName(), userRole.getName());
    }

    @Test
    void findByUsernameActive_ShouldReturnUserRoles() {
        // When
        List<Role> userRoles = roleRepository.findByUsernameActive(testUser.getUsername());

        // Then
        assertThat(userRoles).hasSize(2);
        assertThat(userRoles).extracting(Role::getName)
                .containsExactlyInAnyOrder(adminRole.getName(), userRole.getName());
    }

    @Test
    void findByPermissionNameActive_ShouldReturnRolesWithPermission() {
        // When
        List<Role> rolesWithReadPermission = roleRepository.findByPermissionNameActive(readPermission.getName());
        List<Role> rolesWithWritePermission = roleRepository.findByPermissionNameActive(writePermission.getName());

        // Then
        assertThat(rolesWithReadPermission).hasSize(3); // ADMIN, USER, SYSTEM
        assertThat(rolesWithReadPermission).extracting(Role::getName)
                .containsExactlyInAnyOrder(adminRole.getName(), userRole.getName(), systemRole.getName());

        assertThat(rolesWithWritePermission).hasSize(2); // ADMIN, SYSTEM
        assertThat(rolesWithWritePermission).extracting(Role::getName)
                .containsExactlyInAnyOrder(adminRole.getName(), systemRole.getName());
    }

    @Test
    void findByResourceAndActionActive_ShouldReturnRolesWithResourceAction() {
        // When
        List<Role> rolesWithUserRead = roleRepository.findByResourceAndActionActive(readPermission.getResource(), readPermission.getAction());
        List<Role> rolesWithUserWrite = roleRepository.findByResourceAndActionActive(writePermission.getResource(), writePermission.getAction());

        // Then
        assertThat(rolesWithUserRead).hasSize(3);
        assertThat(rolesWithUserRead).extracting(Role::getName)
                .containsExactlyInAnyOrder(adminRole.getName(), userRole.getName(), systemRole.getName());

        assertThat(rolesWithUserWrite).hasSize(2);
        assertThat(rolesWithUserWrite).extracting(Role::getName)
                .containsExactlyInAnyOrder(adminRole.getName(), systemRole.getName());
    }

    @Test
    void findByCreatedDateAfterActive_ShouldReturnRecentRoles() {
        // Given
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        // When
        List<Role> recentRoles = roleRepository.findByCreatedDateAfterActive(yesterday);

        // Then
        assertThat(recentRoles).hasSize(3); // All active roles created today
        assertThat(recentRoles).extracting(Role::getName)
                .containsExactlyInAnyOrder(adminRole.getName(), userRole.getName(), systemRole.getName());
    }

    @Test
    void findRolesWithManyUsers_ShouldReturnRolesWithMultipleUsers() {
        // Given - Create another user with both admin and user roles
        String uniqueSuffix2 = "USER2";
        User anotherUser = new User("anotheruser_" + uniqueSuffix2.toLowerCase(), "password", "another_" + uniqueSuffix2.toLowerCase() + "@example.com");
        anotherUser.setEnabled(true);
        
        // Merge the roles to get managed entities
        Role managedAdminRole = entityManager.merge(adminRole);
        Role managedUserRole = entityManager.merge(userRole);
        anotherUser.addRole(managedAdminRole);
        anotherUser.addRole(managedUserRole);
        
        entityManager.persistAndFlush(anotherUser);
        entityManager.clear();

        // When
        List<Role> rolesWithManyUsers = roleRepository.findRolesWithManyUsers(2);

        // Then
        assertThat(rolesWithManyUsers).hasSize(2); // ADMIN and USER roles should have 2 users each
        assertThat(rolesWithManyUsers).extracting(Role::getName)
                .containsExactlyInAnyOrder(adminRole.getName(), userRole.getName());
    }

    @Test
    void findRolesWithoutUsers_ShouldReturnRolesWithNoUsers() {
        // When
        List<Role> rolesWithoutUsers = roleRepository.findRolesWithoutUsers();

        // Then
        assertThat(rolesWithoutUsers).hasSize(1); // Only SYSTEM role has no users
        assertThat(rolesWithoutUsers.get(0).getName()).isEqualTo(systemRole.getName());
    }

    @Test
    void findRolesWithoutPermissions_ShouldReturnRolesWithNoPermissions() {
        // Given - Create a role without permissions
        long timestamp = System.currentTimeMillis();
        String uniqueSuffix2 = Long.toString(timestamp, 36).toUpperCase().replaceAll("[0-9]", "");
        if (uniqueSuffix2.length() > 8) {
            uniqueSuffix2 = uniqueSuffix2.substring(0, 8);
        }
        Role emptyRole = new Role("EMPTY_ROLE_" + uniqueSuffix2, "Role without permissions");
        emptyRole.setSystemRole(false);
        entityManager.persistAndFlush(emptyRole);
        entityManager.clear();

        // When
        List<Role> rolesWithoutPermissions = roleRepository.findRolesWithoutPermissions();

        // Then
        assertThat(rolesWithoutPermissions).hasSize(1);
        assertThat(rolesWithoutPermissions.get(0).getName()).isEqualTo(emptyRole.getName());
    }

    @Test
    void findRolesWithManyPermissions_ShouldReturnRolesWithMultiplePermissions() {
        // When
        List<Role> rolesWithManyPermissions = roleRepository.findRolesWithManyPermissions(2);

        // Then
        assertThat(rolesWithManyPermissions).hasSize(2); // ADMIN and SYSTEM roles have 2 permissions each
        assertThat(rolesWithManyPermissions).extracting(Role::getName)
                .containsExactlyInAnyOrder(adminRole.getName(), systemRole.getName());
    }

    @Test
    void countBySystemRoleActive_ShouldReturnCorrectCount() {
        // When
        long systemRoleCount = roleRepository.countBySystemRoleActive(true);
        long nonSystemRoleCount = roleRepository.countBySystemRoleActive(false);

        // Then
        assertThat(systemRoleCount).isEqualTo(1);
        assertThat(nonSystemRoleCount).isEqualTo(2);
    }

    @Test
    void countByUserIdActive_ShouldReturnCorrectCount() {
        // When
        long userRoleCount = roleRepository.countByUserIdActive(testUser.getId());

        // Then
        assertThat(userRoleCount).isEqualTo(2);
    }

    @Test
    void countByPermissionNameActive_ShouldReturnCorrectCount() {
        // When
        long readPermissionCount = roleRepository.countByPermissionNameActive(readPermission.getName());
        long writePermissionCount = roleRepository.countByPermissionNameActive(writePermission.getName());

        // Then
        assertThat(readPermissionCount).isEqualTo(3);
        assertThat(writePermissionCount).isEqualTo(2);
    }

    @Test
    void updateSystemRoleFlag_ShouldUpdateFlag() {
        // When
        int affectedRows = roleRepository.updateSystemRoleFlag(adminRole.getId(), true);

        // Then
        assertThat(affectedRows).isEqualTo(1);
        
        entityManager.clear();
        Role updatedRole = entityManager.find(Role.class, adminRole.getId());
        assertThat(updatedRole.isSystemRole()).isTrue();
        assertThat(updatedRole.getLastModifiedDate()).isNotNull();
    }

    @Test
    void updateDescription_ShouldUpdateDescription() {
        // Given
        String newDescription = "Updated administrator role";

        // When
        int affectedRows = roleRepository.updateDescription(adminRole.getId(), newDescription);

        // Then
        assertThat(affectedRows).isEqualTo(1);
        
        entityManager.clear();
        Role updatedRole = entityManager.find(Role.class, adminRole.getId());
        assertThat(updatedRole.getDescription()).isEqualTo(newDescription);
    }

    @Test
    void batchUpdateSystemRoleFlag_ShouldUpdateMultipleRoles() {
        // Given
        List<Long> roleIds = Arrays.asList(adminRole.getId(), userRole.getId());

        // When
        int affectedRows = roleRepository.batchUpdateSystemRoleFlag(roleIds, true);

        // Then
        assertThat(affectedRows).isEqualTo(2);
        
        entityManager.clear();
        Role updatedAdminRole = entityManager.find(Role.class, adminRole.getId());
        Role updatedUserRole = entityManager.find(Role.class, userRole.getId());
        
        assertThat(updatedAdminRole.isSystemRole()).isTrue();
        assertThat(updatedUserRole.isSystemRole()).isTrue();
    }

    @Test
    void findUnusedNonSystemRoles_ShouldReturnUnusedRoles() {
        // Given - Create a role without users
        long timestamp = System.currentTimeMillis();
        String uniqueSuffix2 = Long.toString(timestamp, 36).toUpperCase().replaceAll("[0-9]", "");
        if (uniqueSuffix2.length() > 8) {
            uniqueSuffix2 = uniqueSuffix2.substring(0, 8);
        }
        Role unusedRole = new Role("UNUSED_ROLE_" + uniqueSuffix2, "Unused role");
        unusedRole.setSystemRole(false);
        entityManager.persistAndFlush(unusedRole);
        entityManager.clear();

        // When
        List<Role> unusedRoles = roleRepository.findUnusedNonSystemRoles();

        // Then
        assertThat(unusedRoles).hasSize(1);
        assertThat(unusedRoles.get(0).getName()).isEqualTo(unusedRole.getName());
    }

    @Test
    void findAllActiveOrderByName_ShouldReturnOrderedRoles() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Role> orderedRoles = roleRepository.findAllActiveOrderByName(pageable);

        // Then
        assertThat(orderedRoles.getTotalElements()).isEqualTo(3);
        // Check that roles are ordered alphabetically by name
        List<String> expectedOrder = List.of(adminRole.getName(), systemRole.getName(), userRole.getName())
                .stream().sorted().toList();
        assertThat(orderedRoles.getContent()).extracting(Role::getName)
                .containsExactlyElementsOf(expectedOrder);
    }

    @Test
    void findBySystemRoleActiveWithPagination_ShouldReturnPagedRoles() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Role> systemRolesPage = roleRepository.findBySystemRoleActive(true, pageable);
        Page<Role> nonSystemRolesPage = roleRepository.findBySystemRoleActive(false, pageable);

        // Then
        assertThat(systemRolesPage.getTotalElements()).isEqualTo(1);
        assertThat(systemRolesPage.getContent().get(0).getName()).isEqualTo(systemRole.getName());

        assertThat(nonSystemRolesPage.getTotalElements()).isEqualTo(2);
        assertThat(nonSystemRolesPage.getContent()).extracting(Role::getName)
                .containsExactlyInAnyOrder(adminRole.getName(), userRole.getName());
    }
}