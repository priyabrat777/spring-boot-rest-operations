package com.enterprise.api.integration;

import com.enterprise.api.entity.*;
import com.enterprise.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Database integration tests with H2.
 * Tests complete database operations, entity relationships, and data integrity.
 * 
 * Requirements addressed:
 * - 10.2: Database integration tests with H2
 * - 10.4: Database integration tests with transactions
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private OtpRepository otpRepository;

    private User testUser;
    private Role testRole;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        auditLogRepository.deleteAll();
        otpRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // Create test permission
        testPermission = new Permission();
        testPermission.setName("READ_USERS");
        testPermission.setResource("USER");
        testPermission.setAction("READ");
        testPermission = permissionRepository.save(testPermission);

        // Create test role
        testRole = new Role();
        testRole.setName("TEST_ROLE");
        testRole.setDescription("Test role for integration testing");
        testRole.setPermissions(Set.of(testPermission));
        testRole = roleRepository.save(testRole);

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedpassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setRoles(Set.of(testRole));
        testUser = userRepository.save(testUser);
    }

    @Test
    void userRepository_CompleteUserLifecycle_ShouldWorkCorrectly() {
        // Test user creation with audit fields
        assertThat(testUser.getId()).isNotNull();
        assertThat(testUser.getCreatedDate()).isNotNull();
        assertThat(testUser.getVersion()).isEqualTo(0L);
        assertThat(testUser.isDeleted()).isFalse();

        // Test user retrieval
        var foundUser = userRepository.findByUsernameActive("testuser");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
        assertThat(foundUser.get().getRoles()).hasSize(1);

        // Test user update
        testUser.setEmail("updated@example.com");
        User updatedUser = userRepository.save(testUser);
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getLastModifiedDate()).isNotNull();
        assertThat(updatedUser.getVersion()).isEqualTo(1L);

        // Test soft delete
        testUser.markAsDeleted();
        userRepository.save(testUser);
        
        var activeUsers = userRepository.findAllActive();
        assertThat(activeUsers).isEmpty();

        var deletedUser = userRepository.findById(testUser.getId());
        assertThat(deletedUser).isPresent();
        assertThat(deletedUser.get().isDeleted()).isTrue();
    }

    @Test
    void roleRepository_RolePermissionRelationships_ShouldWorkCorrectly() {
        // Test role creation with permissions
        assertThat(testRole.getId()).isNotNull();
        assertThat(testRole.getPermissions()).hasSize(1);
        assertThat(testRole.getPermissions().iterator().next().getName()).isEqualTo("READ_USERS");

        // Test adding additional permission
        Permission writePermission = new Permission();
        writePermission.setName("WRITE_USERS");
        writePermission.setResource("USER");
        writePermission.setAction("WRITE");
        writePermission = permissionRepository.save(writePermission);

        testRole.getPermissions().add(writePermission);
        Role updatedRole = roleRepository.save(testRole);
        
        assertThat(updatedRole.getPermissions()).hasSize(2);
        assertThat(updatedRole.getPermissions())
            .extracting(Permission::getName)
            .containsExactlyInAnyOrder("READ_USERS", "WRITE_USERS");
    }

    @Test
    void auditLogRepository_AuditTrailOperations_ShouldWorkCorrectly() {
        // Create audit logs for different operations
        AuditLog createLog = new AuditLog("User", testUser.getId().toString(), 
            AuditLog.AuditOperation.CREATE, "system");
        createLog.setNewValues("{\"username\":\"testuser\",\"email\":\"test@example.com\"}");
        createLog.setIpAddress("192.168.1.100");
        createLog.setUserAgent("Test Agent");
        auditLogRepository.save(createLog);

        AuditLog updateLog = new AuditLog("User", testUser.getId().toString(), 
            AuditLog.AuditOperation.UPDATE, "testuser");
        updateLog.setOldValues("{\"email\":\"test@example.com\"}");
        updateLog.setNewValues("{\"email\":\"updated@example.com\"}");
        updateLog.setIpAddress("192.168.1.101");
        updateLog.setUserAgent("Test Agent");
        auditLogRepository.save(updateLog);

        // Test audit log retrieval
        var userAuditLogs = auditLogRepository.findAll().stream()
            .filter(log -> "User".equals(log.getEntityName()) && testUser.getId().toString().equals(log.getEntityId()))
            .toList();
        assertThat(userAuditLogs).hasSize(2);

        var createLogs = auditLogRepository.findAll().stream()
            .filter(log -> AuditLog.AuditOperation.CREATE.equals(log.getOperation()))
            .toList();
        assertThat(createLogs).hasSize(1);
        assertThat(createLogs.get(0).getNewValues()).contains("testuser");

        var systemLogs = auditLogRepository.findAll().stream()
            .filter(log -> "system".equals(log.getPerformedBy()))
            .toList();
        assertThat(systemLogs).hasSize(1);
        assertThat(systemLogs.get(0).getOperation()).isEqualTo(AuditLog.AuditOperation.CREATE);
    }

    @Test
    void otpRepository_OtpOperations_ShouldWorkCorrectly() {
        // Create OTP
        Otp otp = new Otp();
        otp.setIdentifier("test@example.com");
        otp.setCode("123456");
        otp.setType(OtpType.EMAIL);
        otp.setPurpose(OtpPurpose.PASSWORD_RESET);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setUsed(false);
        
        Otp savedOtp = otpRepository.save(otp);
        
        assertThat(savedOtp.getId()).isNotNull();
        assertThat(savedOtp.getIdentifier()).isEqualTo("test@example.com");

        // Test OTP retrieval
        var foundOtps = otpRepository.findAll().stream()
            .filter(o -> "test@example.com".equals(o.getIdentifier()) && "123456".equals(o.getCode()) && !o.isUsed())
            .toList();
        assertThat(foundOtps).hasSize(1);
        assertThat(foundOtps.get(0).getPurpose()).isEqualTo(OtpPurpose.PASSWORD_RESET);

        // Test OTP usage
        otp.setUsed(true);
        otpRepository.save(otp);
        
        var usedOtps = otpRepository.findAll().stream()
            .filter(o -> "test@example.com".equals(o.getIdentifier()) && "123456".equals(o.getCode()) && !o.isUsed())
            .toList();
        assertThat(usedOtps).isEmpty();
    }

    @Test
    void cascadeOperations_UserDeletion_ShouldHandleCascades() {
        // Create OTP for user
        Otp otp = new Otp();
        otp.setIdentifier(testUser.getEmail());
        otp.setCode("654321");
        otp.setType(OtpType.SMS);
        otp.setPurpose(OtpPurpose.LOGIN);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otpRepository.save(otp);

        // Verify entities exist
        assertThat(otpRepository.findAll()).hasSize(1);

        // Soft delete user
        testUser.markAsDeleted();
        userRepository.save(testUser);

        // Verify user is soft deleted but related entities still exist
        assertThat(userRepository.findAllActive()).isEmpty();
        assertThat(otpRepository.findAll()).hasSize(1);
    }

    @Test
    void complexQuery_UserWithRolesAndPermissions_ShouldWorkCorrectly() {
        // Test complex query with joins
        var usersWithRole = userRepository.findByRoleNameActive("TEST_ROLE");
        assertThat(usersWithRole).hasSize(1);
        assertThat(usersWithRole.get(0).getUsername()).isEqualTo("testuser");

        // Test user permissions through roles
        var userWithRoles = userRepository.findByIdWithRoles(testUser.getId()).orElse(null);
        assertThat(userWithRoles).isNotNull();
        assertThat(userWithRoles.getRoles()).hasSize(1);
        
        Role role = userWithRoles.getRoles().iterator().next();
        assertThat(role.getPermissions()).hasSize(1);
        assertThat(role.getPermissions().iterator().next().getName()).isEqualTo("READ_USERS");
    }

    @Test
    void auditableEntityVersioning_OptimisticLocking_ShouldWorkCorrectly() {
        // Get user in two different contexts
        User user1 = userRepository.findById(testUser.getId()).orElseThrow();
        User user2 = userRepository.findById(testUser.getId()).orElseThrow();

        // Modify and save first instance
        user1.setEmail("first@example.com");
        userRepository.save(user1);

        // Try to modify and save second instance (should fail due to version mismatch)
        user2.setEmail("second@example.com");
        
        try {
            userRepository.save(user2);
            // If we reach here, optimistic locking is not working
            assertThat(false).as("Expected optimistic locking exception").isTrue();
        } catch (Exception e) {
            // Expected - optimistic locking exception
            assertThat(e.getMessage()).contains("version");
        }
    }

    @Test
    void databaseConstraints_UniqueConstraints_ShouldBeEnforced() {
        // Try to create user with duplicate username
        User duplicateUser = new User();
        duplicateUser.setUsername("testuser"); // Same as testUser
        duplicateUser.setEmail("different@example.com");
        duplicateUser.setPassword("password");
        duplicateUser.setFirstName("Duplicate");
        duplicateUser.setLastName("User");
        duplicateUser.setEnabled(true);

        try {
            userRepository.save(duplicateUser);
            userRepository.flush(); // Force constraint check
            // If we reach here, unique constraint is not working
            assertThat(false).as("Expected unique constraint violation").isTrue();
        } catch (Exception e) {
            // Expected - unique constraint violation
            assertThat(e.getMessage().toLowerCase()).contains("unique");
        }
    }

    @Test
    void batchOperations_WithDatabase_ShouldMaintainConsistency() {
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
        assertThat(userRepository.count()).isEqualTo(6); // 5 batch users + 1 test user

        // Verify we can query all batch users
        var batchUsers = userRepository.findAll().stream()
            .filter(user -> user.getUsername().startsWith("batchuser"))
            .toList();
        assertThat(batchUsers).hasSize(5);
    }
}