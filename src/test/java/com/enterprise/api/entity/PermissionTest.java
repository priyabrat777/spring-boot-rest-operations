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
 * Unit tests for Permission entity validation and relationships.
 * Tests validation constraints, entity relationships, and business logic.
 */
@DisplayName("Permission Entity Tests")
class PermissionTest {

    private Validator validator;
    private Permission permission;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        permission = new Permission();
        permission.setName("USER_READ");
        permission.setDescription("Read user data");
        permission.setResource("user");
        permission.setAction("read");
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid permission data")
        void shouldPassValidationWithValidData() {
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when name is blank")
        void shouldFailValidationWhenNameIsBlank() {
            permission.setName("");
            
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            
            assertThat(violations).hasSize(3);
            assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                    "Permission name is required",
                    "Permission name must be between 2 and 100 characters",
                    "Permission name must contain only uppercase letters, underscores, and colons"
                );
        }

        @Test
        @DisplayName("Should fail validation when name is too short")
        void shouldFailValidationWhenNameIsTooShort() {
            permission.setName("A");
            
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Permission name must be between 2 and 100 characters");
        }

        @Test
        @DisplayName("Should fail validation when name is too long")
        void shouldFailValidationWhenNameIsTooLong() {
            permission.setName("A".repeat(101));
            
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Permission name must be between 2 and 100 characters");
        }

        @Test
        @DisplayName("Should fail validation when name contains invalid characters")
        void shouldFailValidationWhenNameContainsInvalidCharacters() {
            permission.setName("user-read");
            
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Permission name must contain only uppercase letters, underscores, and colons");
        }

        @Test
        @DisplayName("Should pass validation with valid permission name")
        void shouldPassValidationWithValidPermissionName() {
            permission.setName("USER_PROFILE:READ");
            
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when resource is blank")
        void shouldFailValidationWhenResourceIsBlank() {
            permission.setResource("");
            
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            
            assertThat(violations).hasSize(3);
            assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                    "Resource is required",
                    "Resource must be between 2 and 50 characters",
                    "Resource must contain only lowercase letters and underscores"
                );
        }

        @Test
        @DisplayName("Should fail validation when resource contains invalid characters")
        void shouldFailValidationWhenResourceContainsInvalidCharacters() {
            permission.setResource("User-Profile");
            
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Resource must contain only lowercase letters and underscores");
        }

        @Test
        @DisplayName("Should pass validation with valid resource")
        void shouldPassValidationWithValidResource() {
            permission.setResource("user_profile");
            
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when action is blank")
        void shouldFailValidationWhenActionIsBlank() {
            permission.setAction("");
            
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            
            assertThat(violations).hasSize(3);
            assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                    "Action is required",
                    "Action must be between 2 and 50 characters",
                    "Action must contain only lowercase letters and underscores"
                );
        }

        @Test
        @DisplayName("Should fail validation when action contains invalid characters")
        void shouldFailValidationWhenActionContainsInvalidCharacters() {
            permission.setAction("Read-Write");
            
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Action must contain only lowercase letters and underscores");
        }

        @Test
        @DisplayName("Should pass validation with valid action")
        void shouldPassValidationWithValidAction() {
            permission.setAction("read_write");
            
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when description is too long")
        void shouldFailValidationWhenDescriptionIsTooLong() {
            permission.setDescription("a".repeat(256));
            
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Description must not exceed 255 characters");
        }

        @Test
        @DisplayName("Should pass validation with null description")
        void shouldPassValidationWithNullDescription() {
            permission.setDescription(null);
            
            Set<ConstraintViolation<Permission>> violations = validator.validate(permission);
            
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Role Management Tests")
    class RoleManagementTests {

        private Role adminRole;
        private Role userRole;

        @BeforeEach
        void setUp() {
            adminRole = new Role("ADMIN", "Administrator role");
            userRole = new Role("USER", "Regular user role");
        }

        @Test
        @DisplayName("Should add role successfully")
        void shouldAddRoleSuccessfully() {
            permission.addRole(adminRole);
            
            assertThat(permission.getRoles()).contains(adminRole);
            assertThat(adminRole.getPermissions()).contains(permission);
        }

        @Test
        @DisplayName("Should remove role successfully")
        void shouldRemoveRoleSuccessfully() {
            permission.addRole(adminRole);
            permission.removeRole(adminRole);
            
            assertThat(permission.getRoles()).doesNotContain(adminRole);
            assertThat(adminRole.getPermissions()).doesNotContain(permission);
        }

        @Test
        @DisplayName("Should handle null role gracefully")
        void shouldHandleNullRoleGracefully() {
            permission.addRole(null);
            
            assertThat(permission.getRoles()).isEmpty();
        }

        @Test
        @DisplayName("Should handle multiple roles")
        void shouldHandleMultipleRoles() {
            permission.addRole(adminRole);
            permission.addRole(userRole);
            
            assertThat(permission.getRoles()).hasSize(2);
            assertThat(permission.getRoles()).contains(adminRole, userRole);
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should match resource and action correctly")
        void shouldMatchResourceAndActionCorrectly() {
            assertThat(permission.matches("user", "read")).isTrue();
            assertThat(permission.matches("user", "write")).isFalse();
            assertThat(permission.matches("role", "read")).isFalse();
        }

        @Test
        @DisplayName("Should return full permission string")
        void shouldReturnFullPermissionString() {
            assertThat(permission.getFullPermission()).isEqualTo("user:read");
        }

        @Test
        @DisplayName("Should have default boolean values")
        void shouldHaveDefaultBooleanValues() {
            Permission newPermission = new Permission();
            
            assertThat(newPermission.isSystemPermission()).isFalse();
            assertThat(newPermission.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("Should set system permission flag")
        void shouldSetSystemPermissionFlag() {
            permission.setSystemPermission(true);
            
            assertThat(permission.isSystemPermission()).isTrue();
        }
    }

    @Nested
    @DisplayName("Equality and Hash Code Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when IDs are the same")
        void shouldBeEqualWhenIdsAreTheSame() {
            Permission permission1 = new Permission();
            permission1.setId(1L);
            permission1.setName("PERM1");
            
            Permission permission2 = new Permission();
            permission2.setId(1L);
            permission2.setName("PERM2");
            
            assertThat(permission1).isEqualTo(permission2);
            assertThat(permission1.hashCode()).isEqualTo(permission2.hashCode());
        }

        @Test
        @DisplayName("Should be equal when names are the same and IDs are null")
        void shouldBeEqualWhenNamesAreTheSameAndIdsAreNull() {
            Permission permission1 = new Permission();
            permission1.setName("USER_READ");
            
            Permission permission2 = new Permission();
            permission2.setName("USER_READ");
            
            assertThat(permission1).isEqualTo(permission2);
            assertThat(permission1.hashCode()).isEqualTo(permission2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when names are different")
        void shouldNotBeEqualWhenNamesAreDifferent() {
            Permission permission1 = new Permission();
            permission1.setName("USER_READ");
            
            Permission permission2 = new Permission();
            permission2.setName("USER_WRITE");
            
            assertThat(permission1).isNotEqualTo(permission2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            assertThat(permission).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different class")
        void shouldNotBeEqualToDifferentClass() {
            assertThat(permission).isNotEqualTo("string");
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create permission with default constructor")
        void shouldCreatePermissionWithDefaultConstructor() {
            Permission newPermission = new Permission();
            
            assertThat(newPermission.getName()).isNull();
            assertThat(newPermission.getDescription()).isNull();
            assertThat(newPermission.getResource()).isNull();
            assertThat(newPermission.getAction()).isNull();
            assertThat(newPermission.getRoles()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should create permission with name, resource, and action constructor")
        void shouldCreatePermissionWithNameResourceActionConstructor() {
            Permission newPermission = new Permission("USER_READ", "user", "read");
            
            assertThat(newPermission.getName()).isEqualTo("USER_READ");
            assertThat(newPermission.getResource()).isEqualTo("user");
            assertThat(newPermission.getAction()).isEqualTo("read");
            assertThat(newPermission.getDescription()).isNull();
            assertThat(newPermission.getRoles()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should create permission with full constructor")
        void shouldCreatePermissionWithFullConstructor() {
            Permission newPermission = new Permission("USER_READ", "Read user data", "user", "read");
            
            assertThat(newPermission.getName()).isEqualTo("USER_READ");
            assertThat(newPermission.getDescription()).isEqualTo("Read user data");
            assertThat(newPermission.getResource()).isEqualTo("user");
            assertThat(newPermission.getAction()).isEqualTo("read");
            assertThat(newPermission.getRoles()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should generate meaningful toString")
        void shouldGenerateMeaningfulToString() {
            permission.setId(1L);
            permission.setSystemPermission(true);
            
            String toString = permission.toString();
            
            assertThat(toString).contains("Permission{");
            assertThat(toString).contains("id=1");
            assertThat(toString).contains("name='USER_READ'");
            assertThat(toString).contains("description='Read user data'");
            assertThat(toString).contains("resource='user'");
            assertThat(toString).contains("action='read'");
            assertThat(toString).contains("systemPermission=true");
        }
    }
}