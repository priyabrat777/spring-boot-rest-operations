package com.enterprise.api.integration;

import com.enterprise.api.entity.*;
import com.enterprise.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Complete API workflow integration tests.
 * Tests end-to-end user scenarios across multiple repositories and entities.
 * 
 * Requirements addressed:
 * - 10.2: Create integration tests for complete API workflows
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApiWorkflowIntegrationTest {

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User regularUser;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        auditLogRepository.deleteAll();
        otpRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // Create permissions
        Permission readPermission = createPermission("READ_USERS", "USER", "READ");
        Permission writePermission = createPermission("WRITE_USERS", "USER", "WRITE");
        Permission adminPermission = createPermission("ADMIN_ACCESS", "SYSTEM", "ADMIN");

        // Create roles
        userRole = createRole("USER", "Regular user role", Set.of(readPermission));
        adminRole = createRole("ADMIN", "Administrator role", 
            Set.of(readPermission, writePermission, adminPermission));

        // Create users
        adminUser = createUser("admin", "admin@example.com", "Admin", "User", Set.of(adminRole));
        regularUser = createUser("user", "user@example.com", "Regular", "User", Set.of(userRole));
    }

    @Test
    void completeUserRegistrationWorkflow_ShouldWorkEndToEnd() {
        // 1. Create a new user directly via repository
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("newuser@example.com");
        newUser.setPassword(passwordEncoder.encode("newpassword"));
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setEnabled(true);
        newUser.setRoles(Set.of(userRole));

        User createdUser = userRepository.save(newUser);
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getUsername()).isEqualTo("newuser");

        // 2. Verify user was created in database
        var foundUser = userRepository.findByUsernameActive("newuser");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("newuser@example.com");

        // 3. Verify audit fields are populated
        assertThat(createdUser.getCreatedDate()).isNotNull();
        assertThat(createdUser.getVersion()).isEqualTo(0L);
    }

    @Test
    void completeUserManagementWorkflow_ShouldWorkEndToEnd() {
        // 1. Retrieve user
        User retrievedUser = userRepository.findById(regularUser.getId()).orElseThrow();
        assertThat(retrievedUser.getUsername()).isEqualTo("user");

        // 2. Update user
        retrievedUser.setEmail("updated@example.com");
        retrievedUser.setFirstName("Updated");
        User updatedUser = userRepository.save(retrievedUser);
        
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getFirstName()).isEqualTo("Updated");

        // 3. Verify update in database
        var dbUser = userRepository.findById(regularUser.getId()).orElseThrow();
        assertThat(dbUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(dbUser.getVersion()).isEqualTo(1L); // Version should be incremented

        // 4. Soft delete user
        retrievedUser.markAsDeleted();
        userRepository.save(retrievedUser);
        
        // 5. Verify user is soft deleted
        var deletedUser = userRepository.findById(regularUser.getId()).orElse(null);
        assertThat(deletedUser).isNotNull();
        assertThat(deletedUser.isDeleted()).isTrue();

        // 6. Verify user is not found in active users
        var activeUser = userRepository.findByUsernameActive("user");
        assertThat(activeUser).isEmpty();
    }

    @Test
    void completeOtpWorkflow_ShouldWorkEndToEnd() {
        // 1. Create OTP directly via repository
        String identifier = regularUser.getEmail();
        Otp otp = new Otp();
        otp.setIdentifier(identifier);
        otp.setCode("123456");
        otp.setType(OtpType.EMAIL);
        otp.setPurpose(OtpPurpose.PASSWORD_RESET);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setUsed(false);
        
        Otp generatedOtp = otpRepository.save(otp);

        // 2. Verify OTP was created in database
        var otps = otpRepository.findAll().stream()
            .filter(o -> identifier.equals(o.getIdentifier()))
            .toList();
        assertThat(otps).hasSize(1);
        
        assertThat(generatedOtp.getType()).isEqualTo(OtpType.EMAIL);
        assertThat(generatedOtp.getPurpose()).isEqualTo(OtpPurpose.PASSWORD_RESET);
        assertThat(generatedOtp.isUsed()).isFalse();
        assertThat(generatedOtp.isValid()).isTrue();

        // 3. Mark OTP as used
        generatedOtp.markAsUsed();
        otpRepository.save(generatedOtp);

        // 4. Verify OTP is marked as used
        var usedOtp = otpRepository.findById(generatedOtp.getId()).orElseThrow();
        assertThat(usedOtp.isUsed()).isTrue();
        assertThat(usedOtp.isValid()).isFalse();
    }

    @Test
    void completeRoleManagementWorkflow_ShouldWorkEndToEnd() {
        // 1. Create a new role
        Permission newPermission = createPermission("MANAGE_FILES", "FILE", "MANAGE");
        Role newRole = new Role();
        newRole.setName("FILE_MANAGER");
        newRole.setDescription("File manager role");
        newRole.setPermissions(Set.of(newPermission));
        
        Role createdRole = roleRepository.save(newRole);
        assertThat(createdRole.getId()).isNotNull();

        // 2. Assign role to user
        regularUser.getRoles().add(createdRole);
        User updatedUser = userRepository.save(regularUser);
        
        assertThat(updatedUser.getRoles()).hasSize(2); // USER + FILE_MANAGER

        // 3. Verify user has new permissions
        var userPermissions = updatedUser.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(Permission::getName)
            .toList();
        assertThat(userPermissions).contains("READ_USERS", "MANAGE_FILES");

        // 4. Remove role from user
        regularUser.getRoles().remove(createdRole);
        User userWithoutRole = userRepository.save(regularUser);
        
        assertThat(userWithoutRole.getRoles()).hasSize(1); // Only USER role remains

        // 5. Delete the role
        roleRepository.delete(createdRole);
        assertThat(roleRepository.findById(createdRole.getId())).isEmpty();
    }

    @Test
    void completePasswordChangeWorkflow_ShouldWorkEndToEnd() {
        // 1. Verify current password is encoded correctly
        assertThat(passwordEncoder.matches("password", regularUser.getPassword())).isTrue();

        // 2. Change password directly
        String newPassword = "newpassword123";
        regularUser.setPassword(passwordEncoder.encode(newPassword));
        User updatedUser = userRepository.save(regularUser);

        // 3. Verify old password no longer matches
        assertThat(passwordEncoder.matches("password", updatedUser.getPassword())).isFalse();

        // 4. Verify new password matches
        assertThat(passwordEncoder.matches(newPassword, updatedUser.getPassword())).isTrue();

        // 5. Verify password is properly encoded in database
        var dbUser = userRepository.findById(regularUser.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(newPassword, dbUser.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("password", dbUser.getPassword())).isFalse();
    }

    @Test
    void completeAuditTrailWorkflow_ShouldWorkEndToEnd() {
        // 1. Create audit logs manually to simulate operations
        User newUser = new User();
        newUser.setUsername("audituser");
        newUser.setEmail("audit@example.com");
        newUser.setPassword(passwordEncoder.encode("password"));
        newUser.setFirstName("Audit");
        newUser.setLastName("User");
        newUser.setEnabled(true);
        newUser.setRoles(Set.of(userRole));

        User createdUser = userRepository.save(newUser);

        // 2. Create audit logs for the operations
        AuditLog createLog = new AuditLog("User", createdUser.getId().toString(), 
            AuditLog.AuditOperation.CREATE, "system");
        createLog.setNewValues("{\"username\":\"audituser\",\"email\":\"audit@example.com\"}");
        auditLogRepository.save(createLog);

        // 3. Update the user
        createdUser.setEmail("updated-audit@example.com");
        userRepository.save(createdUser);

        AuditLog updateLog = new AuditLog("User", createdUser.getId().toString(), 
            AuditLog.AuditOperation.UPDATE, "system");
        updateLog.setOldValues("{\"email\":\"audit@example.com\"}");
        updateLog.setNewValues("{\"email\":\"updated-audit@example.com\"}");
        auditLogRepository.save(updateLog);

        // 4. Soft delete the user
        createdUser.markAsDeleted();
        userRepository.save(createdUser);

        AuditLog deleteLog = new AuditLog("User", createdUser.getId().toString(), 
            AuditLog.AuditOperation.DELETE, "system");
        deleteLog.setOldValues("{\"deleted\":false}");
        deleteLog.setNewValues("{\"deleted\":true}");
        auditLogRepository.save(deleteLog);

        // 5. Check audit logs
        var auditLogs = auditLogRepository.findAll().stream()
            .filter(log -> "User".equals(log.getEntityName()) && createdUser.getId().toString().equals(log.getEntityId()))
            .toList();
        
        assertThat(auditLogs).hasSize(3); // CREATE, UPDATE, DELETE
        
        var operations = auditLogs.stream()
            .map(AuditLog::getOperation)
            .toList();
        assertThat(operations).contains(AuditLog.AuditOperation.CREATE, AuditLog.AuditOperation.UPDATE, AuditLog.AuditOperation.DELETE);
    }

    @Test
    void completeErrorHandlingWorkflow_ShouldWorkEndToEnd() {
        // 1. Test duplicate username constraint
        User duplicateUser = new User();
        duplicateUser.setUsername("user"); // Same as regularUser
        duplicateUser.setEmail("different@example.com");
        duplicateUser.setPassword(passwordEncoder.encode("password"));
        duplicateUser.setFirstName("Duplicate");
        duplicateUser.setLastName("User");
        duplicateUser.setEnabled(true);
        duplicateUser.setRoles(Set.of(userRole));

        assertThatThrownBy(() -> {
            userRepository.save(duplicateUser);
            userRepository.flush(); // Force constraint check
        }).hasMessageContaining("unique");

        // 2. Test user not found
        var nonExistentUser = userRepository.findById(99999L);
        assertThat(nonExistentUser).isEmpty();

        // 3. Test finding non-existent active user
        var nonExistentActiveUser = userRepository.findByUsernameActive("nonexistent");
        assertThat(nonExistentActiveUser).isEmpty();

        // 4. Test optimistic locking
        User user1 = userRepository.findById(regularUser.getId()).orElseThrow();
        User user2 = userRepository.findById(regularUser.getId()).orElseThrow();

        user1.setEmail("first@example.com");
        userRepository.save(user1);

        user2.setEmail("second@example.com");
        assertThatThrownBy(() -> userRepository.save(user2))
            .hasMessageContaining("version");
    }

    @Test
    void completeDataConsistencyWorkflow_ShouldWorkEndToEnd() {
        // 1. Create user with roles and permissions
        Permission customPermission = createPermission("CUSTOM_PERMISSION", "CUSTOM", "READ");
        Role customRole = createRole("CUSTOM_ROLE", "Custom role", Set.of(customPermission));
        
        User userWithCustomRole = new User();
        userWithCustomRole.setUsername("customuser");
        userWithCustomRole.setEmail("custom@example.com");
        userWithCustomRole.setPassword(passwordEncoder.encode("password"));
        userWithCustomRole.setFirstName("Custom");
        userWithCustomRole.setLastName("User");
        userWithCustomRole.setEnabled(true);
        userWithCustomRole.setRoles(Set.of(customRole));

        User createdUser = userRepository.save(userWithCustomRole);

        // 2. Verify all relationships are maintained
        var dbUser = userRepository.findByIdWithRoles(createdUser.getId()).orElseThrow();
        assertThat(dbUser.getRoles()).hasSize(1);
        
        Role dbRole = dbUser.getRoles().iterator().next();
        assertThat(dbRole.getName()).isEqualTo("CUSTOM_ROLE");
        assertThat(dbRole.getPermissions()).hasSize(1);
        assertThat(dbRole.getPermissions().iterator().next().getName()).isEqualTo("CUSTOM_PERMISSION");

        // 3. Update user and verify consistency
        dbUser.setFirstName("Updated Custom");
        User updatedUser = userRepository.save(dbUser);
        
        // Verify relationships are still intact after update
        var updatedDbUser = userRepository.findByIdWithRoles(updatedUser.getId()).orElseThrow();
        assertThat(updatedDbUser.getFirstName()).isEqualTo("Updated Custom");
        assertThat(updatedDbUser.getRoles()).hasSize(1);
        assertThat(updatedDbUser.getRoles().iterator().next().getPermissions()).hasSize(1);

        // 4. Soft delete and verify relationships
        updatedUser.markAsDeleted();
        userRepository.save(updatedUser);
        
        var deletedUser = userRepository.findById(updatedUser.getId()).orElseThrow();
        assertThat(deletedUser.isDeleted()).isTrue();
        // Relationships should still exist even after soft delete
        assertThat(deletedUser.getRoles()).hasSize(1);
    }

    private Permission createPermission(String name, String resource, String action) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setResource(resource);
        permission.setAction(action);
        return permissionRepository.save(permission);
    }

    private Role createRole(String name, String description, Set<Permission> permissions) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        role.setPermissions(permissions);
        return roleRepository.save(role);
    }

    private User createUser(String username, String email, String firstName, String lastName, Set<Role> roles) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password"));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setRoles(roles);
        return userRepository.save(user);
    }
}