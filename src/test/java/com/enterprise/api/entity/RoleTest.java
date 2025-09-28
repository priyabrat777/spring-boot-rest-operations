package com.enterprise.api.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Role entity validation and relationships.
 * Tests validation constraints, entity relationships, and business logic.
 */
@DisplayName("Role Entity Tests")
class RoleTest {

    private Validator validator;
    private Role role;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        role = new Role();
        role.setName("ADMIN");
        role.setDescription("Administrator role");
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid role data")
        void shouldPassValidationWithValidData() {
            Set<ConstraintViolation<Role>> violations = validator.validate(role);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when name is blank")
        void shouldFailValidationWhenNameIsBlank() {
            role.setName("");
            
            Set<ConstraintViolation<Role>> violations = validator.validate(role);
            
            assertThat(violations).hasSize(3);
            assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                    "Role name is required",
                    "Role name must be between 2 and 50 characters",
                    "Role name must contain only uppercase letters and underscores"
                );
        }

        @Test
        @DisplayName("Should fail validation when name is too short")
        void shouldFailValidationWhenNameIsTooShort() {
            role.setName("A");
            
            Set<ConstraintViolation<Role>> violations = validator.validate(role);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Role name must be between 2 and 50 characters");
        }

        @Test
        @DisplayName("Should fail validation when name is too long")
        void shouldFailValidationWhenNameIsTooLong() {
            role.setName("A".repeat(51));
            
            Set<ConstraintViolation<Role>> violations = validator.validate(role);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Role name must be between 2 and 50 characters");
        }

        @Test
        @DisplayName("Should fail validation when name contains invalid characters")
        void shouldFailValidationWhenNameContainsInvalidCharacters() {
            role.setName("admin-role");
            
            Set<ConstraintViolation<Role>> violations = validator.validate(role);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Role name must contain only uppercase letters and underscores");
        }

        @Test
        @DisplayName("Should pass validation with valid role name")
        void shouldPassValidationWithValidRoleName() {
            role.setName("SUPER_ADMIN");
            
            Set<ConstraintViolation<Role>> violations = validator.validate(role);
            
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when description is too long")
        void shouldFailValidationWhenDescriptionIsTooLong() {
            role.setDescription("a".repeat(256));
            
            Set<ConstraintViolation<Role>> violations = validator.validate(role);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Description must not exceed 255 characters");
        }

        @Test
        @DisplayName("Should pass validation with null description")
        void shouldPassValidationWithNullDescription() {
            role.setDescription(null);
            
            Set<ConstraintViolation<Role>> violations = validator.validate(role);
            
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Permission Management Tests")
    class PermissionManagementTests {

        private Permission readPermission;
        private Permission writePermission;

        @BeforeEach
        void setUp() {
            readPermission = new Permission("USER_READ", "user", "read");
            writePermission = new Permission("USER_WRITE", "user", "write");
        }

        @Test
        @DisplayName("Should add permission successfully")
        void shouldAddPermissionSuccessfully() {
            role.addPermission(readPermission);
            
            assertThat(role.getPermissions()).contains(readPermission);
            assertThat(readPermission.getRoles()).contains(role);
        }

        @Test
        @DisplayName("Should remove permission successfully")
        void shouldRemovePermissionSuccessfully() {
            role.addPermission(readPermission);
            role.removePermission(readPermission);
            
            assertThat(role.getPermissions()).doesNotContain(readPermission);
            assertThat(readPermission.getRoles()).doesNotContain(role);
        }

        @Test
        @DisplayName("Should check if role has permission by name")
        void shouldCheckIfRoleHasPermissionByName() {
            role.addPermission(readPermission);
            
            assertThat(role.hasPermission("USER_READ")).isTrue();
            assertThat(role.hasPermission("USER_WRITE")).isFalse();
        }

        @Test
        @DisplayName("Should check if role has permission by resource and action")
        void shouldCheckIfRoleHasPermissionByResourceAndAction() {
            role.addPermission(readPermission);
            
            assertThat(role.hasPermission("user", "read")).isTrue();
            assertThat(role.hasPermission("user", "write")).isFalse();
        }

        @Test
        @DisplayName("Should handle null permission gracefully")
        void shouldHandleNullPermissionGracefully() {
            role.addPermission(null);
            
            assertThat(role.getPermissions()).isEmpty();
        }

        @Test
        @DisplayName("Should handle multiple permissions")
        void shouldHandleMultiplePermissions() {
            role.addPermission(readPermission);
            role.addPermission(writePermission);
            
            assertThat(role.getPermissions()).hasSize(2);
            assertThat(role.hasPermission("USER_READ")).isTrue();
            assertThat(role.hasPermission("USER_WRITE")).isTrue();
        }
    }

    @Nested
    @DisplayName("User Management Tests")
    class UserManagementTests {

        private User user1;
        private User user2;

        @BeforeEach
        void setUp() {
            user1 = new User("user1", "password123", "user1@example.com");
            user2 = new User("user2", "password123", "user2@example.com");
        }

        @Test
        @DisplayName("Should add user successfully")
        void shouldAddUserSuccessfully() {
            role.addUser(user1);
            
            assertThat(role.getUsers()).contains(user1);
            assertThat(user1.getRoles()).contains(role);
        }

        @Test
        @DisplayName("Should remove user successfully")
        void shouldRemoveUserSuccessfully() {
            role.addUser(user1);
            role.removeUser(user1);
            
            assertThat(role.getUsers()).doesNotContain(user1);
            assertThat(user1.getRoles()).doesNotContain(role);
        }

        @Test
        @DisplayName("Should handle null user gracefully")
        void shouldHandleNullUserGracefully() {
            role.addUser(null);
            
            assertThat(role.getUsers()).isEmpty();
        }

        @Test
        @DisplayName("Should handle multiple users")
        void shouldHandleMultipleUsers() {
            role.addUser(user1);
            role.addUser(user2);
            
            assertThat(role.getUsers()).hasSize(2);
            assertThat(role.getUsers()).contains(user1, user2);
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should have default boolean values")
        void shouldHaveDefaultBooleanValues() {
            Role newRole = new Role();
            
            assertThat(newRole.isSystemRole()).isFalse();
            assertThat(newRole.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("Should set system role flag")
        void shouldSetSystemRoleFlag() {
            role.setSystemRole(true);
            
            assertThat(role.isSystemRole()).isTrue();
        }
    }

    @Nested
    @DisplayName("Equality and Hash Code Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when IDs are the same")
        void shouldBeEqualWhenIdsAreTheSame() {
            Role role1 = new Role();
            role1.setId(1L);
            role1.setName("ROLE1");
            
            Role role2 = new Role();
            role2.setId(1L);
            role2.setName("ROLE2");
            
            assertThat(role1).isEqualTo(role2);
            assertThat(role1.hashCode()).isEqualTo(role2.hashCode());
        }

        @Test
        @DisplayName("Should be equal when names are the same and IDs are null")
        void shouldBeEqualWhenNamesAreTheSameAndIdsAreNull() {
            Role role1 = new Role();
            role1.setName("ADMIN");
            
            Role role2 = new Role();
            role2.setName("ADMIN");
            
            assertThat(role1).isEqualTo(role2);
            assertThat(role1.hashCode()).isEqualTo(role2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when names are different")
        void shouldNotBeEqualWhenNamesAreDifferent() {
            Role role1 = new Role();
            role1.setName("ADMIN");
            
            Role role2 = new Role();
            role2.setName("USER");
            
            assertThat(role1).isNotEqualTo(role2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            assertThat(role).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different class")
        void shouldNotBeEqualToDifferentClass() {
            assertThat(role).isNotEqualTo("string");
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create role with default constructor")
        void shouldCreateRoleWithDefaultConstructor() {
            Role newRole = new Role();
            
            assertThat(newRole.getName()).isNull();
            assertThat(newRole.getDescription()).isNull();
            assertThat(newRole.getUsers()).isNotNull().isEmpty();
            assertThat(newRole.getPermissions()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should create role with name constructor")
        void shouldCreateRoleWithNameConstructor() {
            Role newRole = new Role("ADMIN");
            
            assertThat(newRole.getName()).isEqualTo("ADMIN");
            assertThat(newRole.getDescription()).isNull();
            assertThat(newRole.getUsers()).isNotNull().isEmpty();
            assertThat(newRole.getPermissions()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should create role with name and description constructor")
        void shouldCreateRoleWithNameAndDescriptionConstructor() {
            Role newRole = new Role("ADMIN", "Administrator role");
            
            assertThat(newRole.getName()).isEqualTo("ADMIN");
            assertThat(newRole.getDescription()).isEqualTo("Administrator role");
            assertThat(newRole.getUsers()).isNotNull().isEmpty();
            assertThat(newRole.getPermissions()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should generate meaningful toString")
        void shouldGenerateMeaningfulToString() {
            role.setId(1L);
            role.setSystemRole(true);
            
            String toString = role.toString();
            
            assertThat(toString).contains("Role{");
            assertThat(toString).contains("id=1");
            assertThat(toString).contains("name='ADMIN'");
            assertThat(toString).contains("description='Administrator role'");
            assertThat(toString).contains("systemRole=true");
        }
    }
}