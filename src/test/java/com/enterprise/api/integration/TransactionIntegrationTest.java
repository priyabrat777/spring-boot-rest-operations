package com.enterprise.api.integration;

import com.enterprise.api.entity.Permission;
import com.enterprise.api.entity.Role;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.PermissionRepository;
import com.enterprise.api.repository.RoleRepository;
import com.enterprise.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Transaction integration tests with H2 database.
 * Tests transaction boundaries, rollback scenarios, and isolation levels.
 * 
 * Requirements addressed:
 * - 10.4: Database integration tests with transactions
 */
@SpringBootTest
@ActiveProfiles("test")
class TransactionIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();
    }

    @Test
    @Transactional
    void transactionRollback_OnException_ShouldRollbackAllChanges() {
        // Get initial counts
        long initialUserCount = userRepository.count();
        long initialRoleCount = roleRepository.count();

        try {
            // This should fail and rollback all changes
            transactionTemplate.execute(status -> {
                // Create a permission
                Permission permission = new Permission();
                permission.setName("TEST_PERMISSION");
                permission.setResource("TEST");
                permission.setAction("READ");
                permissionRepository.save(permission);

                // Create a role
                Role role = new Role();
                role.setName("TEST_ROLE");
                role.setDescription("Test role");
                role.setPermissions(Set.of(permission));
                roleRepository.save(role);

                // Create a user
                User user = new User();
                user.setUsername("testuser");
                user.setEmail("test@example.com");
                user.setPassword("password");
                user.setFirstName("Test");
                user.setLastName("User");
                user.setEnabled(true);
                user.setRoles(Set.of(role));
                userRepository.save(user);

                // Force an exception to trigger rollback
                throw new RuntimeException("Simulated transaction failure");
            });
        } catch (RuntimeException e) {
            // Expected exception
        }

        // Verify rollback occurred - counts should be unchanged
        assertThat(userRepository.count()).isEqualTo(initialUserCount);
        assertThat(roleRepository.count()).isEqualTo(initialRoleCount);
        assertThat(permissionRepository.count()).isEqualTo(0);
    }

    @Test
    @Transactional
    void transactionCommit_OnSuccess_ShouldPersistAllChanges() {
        // Get initial counts
        long initialUserCount = userRepository.count();
        long initialRoleCount = roleRepository.count();

        // Execute successful transaction
        transactionTemplate.execute(status -> {
            // Create a permission
            Permission permission = new Permission();
            permission.setName("COMMIT_PERMISSION");
            permission.setResource("COMMIT");
            permission.setAction("READ");
            permissionRepository.save(permission);

            // Create a role
            Role role = new Role();
            role.setName("COMMIT_ROLE");
            role.setDescription("Commit test role");
            role.setPermissions(Set.of(permission));
            roleRepository.save(role);

            // Create a user
            User user = new User();
            user.setUsername("commituser");
            user.setEmail("commit@example.com");
            user.setPassword("password");
            user.setFirstName("Commit");
            user.setLastName("User");
            user.setEnabled(true);
            user.setRoles(Set.of(role));
            userRepository.save(user);

            return null;
        });

        // Verify commit occurred - counts should be increased
        assertThat(userRepository.count()).isEqualTo(initialUserCount + 1);
        assertThat(roleRepository.count()).isEqualTo(initialRoleCount + 1);
        assertThat(permissionRepository.count()).isEqualTo(1);

        // Verify data integrity
        User savedUser = userRepository.findByUsernameActive("commituser").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getRoles()).hasSize(1);
        assertThat(savedUser.getRoles().iterator().next().getName()).isEqualTo("COMMIT_ROLE");
    }

    @Test
    void nestedTransactions_InnerRollback_ShouldRollbackAll() {
        long initialCount = userRepository.count();

        try {
            transactionTemplate.execute(outerStatus -> {
                // Create user in outer transaction
                User outerUser = new User();
                outerUser.setUsername("outeruser");
                outerUser.setEmail("outer@example.com");
                outerUser.setPassword("password");
                outerUser.setFirstName("Outer");
                outerUser.setLastName("User");
                outerUser.setEnabled(true);
                userRepository.save(outerUser);

                try {
                    transactionTemplate.execute(innerStatus -> {
                        // Create user in inner transaction
                        User innerUser = new User();
                        innerUser.setUsername("inneruser");
                        innerUser.setEmail("inner@example.com");
                        innerUser.setPassword("password");
                        innerUser.setFirstName("Inner");
                        innerUser.setLastName("User");
                        innerUser.setEnabled(true);
                        userRepository.save(innerUser);

                        // Force rollback in inner transaction
                        throw new RuntimeException("Inner transaction failure");
                    });
                } catch (RuntimeException e) {
                    // Inner transaction failed, this should cause outer to rollback too
                    throw e;
                }

                return null;
            });
        } catch (RuntimeException e) {
            // Expected exception
        }

        // Both transactions should be rolled back
        assertThat(userRepository.count()).isEqualTo(initialCount);
    }

    @Test
    @Transactional
    void optimisticLocking_ConcurrentModification_ShouldThrowException() {
        // Create a user
        User user = new User();
        user.setUsername("lockinguser");
        user.setEmail("locking@example.com");
        user.setPassword("password");
        user.setFirstName("Locking");
        user.setLastName("User");
        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        // Simulate concurrent modification by loading the same entity twice
        User user1 = userRepository.findById(savedUser.getId()).orElseThrow();
        User user2 = userRepository.findById(savedUser.getId()).orElseThrow();

        // Modify and save first instance
        user1.setEmail("modified1@example.com");
        userRepository.save(user1);

        // Try to modify and save second instance (should fail due to version mismatch)
        user2.setEmail("modified2@example.com");
        
        assertThatThrownBy(() -> userRepository.save(user2))
            .hasMessageContaining("version");
    }

    @Test
    @Transactional
    void cascadeOperations_WithTransaction_ShouldMaintainConsistency() {
        // Create permission
        Permission permission = new Permission();
        permission.setName("CASCADE_PERMISSION");
        permission.setResource("CASCADE");
        permission.setAction("READ");
        permission = permissionRepository.save(permission);

        // Create role with permission
        Role role = new Role();
        role.setName("CASCADE_ROLE");
        role.setDescription("Cascade test role");
        role.setPermissions(Set.of(permission));
        role = roleRepository.save(role);

        // Create user with role
        User user = new User();
        user.setUsername("cascadeuser");
        user.setEmail("cascade@example.com");
        user.setPassword("password");
        user.setFirstName("Cascade");
        user.setLastName("User");
        user.setEnabled(true);
        user.setRoles(Set.of(role));
        user = userRepository.save(user);

        // Verify all entities are saved
        assertThat(userRepository.findById(user.getId())).isPresent();
        assertThat(roleRepository.findById(role.getId())).isPresent();
        assertThat(permissionRepository.findById(permission.getId())).isPresent();

        // Verify relationships are maintained
        User foundUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(foundUser.getRoles()).hasSize(1);
        
        Role foundRole = foundUser.getRoles().iterator().next();
        assertThat(foundRole.getPermissions()).hasSize(1);
        assertThat(foundRole.getPermissions().iterator().next().getName()).isEqualTo("CASCADE_PERMISSION");
    }

    @Test
    void readOnlyTransaction_ModificationAttempt_ShouldBeIgnored() {
        // Create initial data
        User user = new User();
        user.setUsername("readonlyuser");
        user.setEmail("readonly@example.com");
        user.setPassword("password");
        user.setFirstName("ReadOnly");
        user.setLastName("User");
        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        String originalEmail = savedUser.getEmail();

        // Try to modify in read-only transaction
        transactionTemplate.setReadOnly(true);
        try {
            transactionTemplate.execute(status -> {
                User userToModify = userRepository.findById(savedUser.getId()).orElseThrow();
                userToModify.setEmail("modified@example.com");
                userRepository.save(userToModify);
                return null;
            });
        } finally {
            transactionTemplate.setReadOnly(false);
        }

        // Verify modification was not persisted
        User unchangedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(unchangedUser.getEmail()).isEqualTo(originalEmail);
    }

    @Test
    void transactionPropagation_RequiresNew_ShouldCreateNewTransaction() {
        long initialCount = userRepository.count();

        try {
            transactionTemplate.execute(outerStatus -> {
                // Create user in outer transaction
                User outerUser = new User();
                outerUser.setUsername("propagationouter");
                outerUser.setEmail("outer@example.com");
                outerUser.setPassword("password");
                outerUser.setFirstName("Outer");
                outerUser.setLastName("User");
                outerUser.setEnabled(true);
                userRepository.save(outerUser);

                // Create new transaction template for inner transaction
                TransactionTemplate innerTemplate = new TransactionTemplate(transactionTemplate.getTransactionManager());
                innerTemplate.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);

                try {
                    innerTemplate.execute(innerStatus -> {
                        // Create user in new transaction
                        User innerUser = new User();
                        innerUser.setUsername("propagationinner");
                        innerUser.setEmail("inner@example.com");
                        innerUser.setPassword("password");
                        innerUser.setFirstName("Inner");
                        innerUser.setLastName("User");
                        innerUser.setEnabled(true);
                        userRepository.save(innerUser);

                        // This new transaction should commit independently
                        return null;
                    });
                } catch (Exception e) {
                    // Inner transaction completed successfully
                }

                // Force outer transaction to rollback
                throw new RuntimeException("Outer transaction failure");
            });
        } catch (RuntimeException e) {
            // Expected exception
        }

        // Inner transaction should have committed (REQUIRES_NEW)
        // Outer transaction should have rolled back
        // So we should have 1 more user than initially (the inner transaction user)
        assertThat(userRepository.count()).isEqualTo(initialCount + 1);
        assertThat(userRepository.findByUsernameActive("propagationinner")).isPresent();
        assertThat(userRepository.findByUsernameActive("propagationouter")).isEmpty();
    }

    @Test
    @Transactional
    void batchOperations_WithTransaction_ShouldMaintainConsistency() {
        // Create multiple users in a single transaction
        for (int i = 1; i <= 5; i++) {
            User user = new User();
            user.setUsername("batchuser" + i);
            user.setEmail("batch" + i + "@example.com");
            user.setPassword("password" + i);
            user.setFirstName("Batch" + i);
            user.setLastName("User");
            user.setEnabled(true);
            userRepository.save(user);
        }

        // Verify all users are saved
        assertThat(userRepository.count()).isEqualTo(5);

        // Verify we can query all batch users
        var batchUsers = userRepository.findAll().stream()
            .filter(user -> user.getUsername().startsWith("batchuser"))
            .toList();
        assertThat(batchUsers).hasSize(5);
    }
}