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
 * Unit tests for User entity validation and relationships.
 * Tests validation constraints, entity relationships, and business logic.
 */
@DisplayName("User Entity Tests")
class UserTest {

    private Validator validator;
    private User user;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        user = new User();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setEmail("test@example.com");
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid user data")
        void shouldPassValidationWithValidData() {
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when username is blank")
        void shouldFailValidationWhenUsernameIsBlank() {
            user.setUsername("");
            
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            
            assertThat(violations).hasSize(3);
            assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                    "Username is required",
                    "Username must be between 3 and 50 characters",
                    "Username can only contain letters, numbers, dots, underscores, and hyphens"
                );
        }

        @Test
        @DisplayName("Should fail validation when username is too short")
        void shouldFailValidationWhenUsernameIsTooShort() {
            user.setUsername("ab");
            
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Username must be between 3 and 50 characters");
        }

        @Test
        @DisplayName("Should fail validation when username is too long")
        void shouldFailValidationWhenUsernameIsTooLong() {
            user.setUsername("a".repeat(51));
            
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Username must be between 3 and 50 characters");
        }

        @Test
        @DisplayName("Should fail validation when username contains invalid characters")
        void shouldFailValidationWhenUsernameContainsInvalidCharacters() {
            user.setUsername("test@user");
            
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Username can only contain letters, numbers, dots, underscores, and hyphens");
        }

        @Test
        @DisplayName("Should pass validation with valid username characters")
        void shouldPassValidationWithValidUsernameCharacters() {
            user.setUsername("test.user_123-name");
            
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail validation when password is blank")
        void shouldFailValidationWhenPasswordIsBlank() {
            user.setPassword("");
            
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            
            assertThat(violations).hasSize(2);
            assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                    "Password is required",
                    "Password must be at least 8 characters long"
                );
        }

        @Test
        @DisplayName("Should fail validation when password is too short")
        void shouldFailValidationWhenPasswordIsTooShort() {
            user.setPassword("1234567");
            
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Password must be at least 8 characters long");
        }

        @Test
        @DisplayName("Should fail validation when email is blank")
        void shouldFailValidationWhenEmailIsBlank() {
            user.setEmail("");
            
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Email is required");
        }

        @Test
        @DisplayName("Should fail validation when email is invalid")
        void shouldFailValidationWhenEmailIsInvalid() {
            user.setEmail("invalid-email");
            
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Email should be valid");
        }

        @Test
        @DisplayName("Should fail validation when email is too long")
        void shouldFailValidationWhenEmailIsTooLong() {
            user.setEmail("a".repeat(90) + "@example.com");
            
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            
            assertThat(violations).hasSize(2);
            assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                    "Email must not exceed 100 characters",
                    "Email should be valid"
                );
        }

        @Test
        @DisplayName("Should fail validation when first name is too long")
        void shouldFailValidationWhenFirstNameIsTooLong() {
            user.setFirstName("a".repeat(101));
            
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("First name must not exceed 100 characters");
        }

        @Test
        @DisplayName("Should fail validation when last name is too long")
        void shouldFailValidationWhenLastNameIsTooLong() {
            user.setLastName("a".repeat(101));
            
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Last name must not exceed 100 characters");
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
            user.addRole(adminRole);
            
            assertThat(user.getRoles()).contains(adminRole);
            assertThat(adminRole.getUsers()).contains(user);
        }

        @Test
        @DisplayName("Should remove role successfully")
        void shouldRemoveRoleSuccessfully() {
            user.addRole(adminRole);
            user.removeRole(adminRole);
            
            assertThat(user.getRoles()).doesNotContain(adminRole);
            assertThat(adminRole.getUsers()).doesNotContain(user);
        }

        @Test
        @DisplayName("Should check if user has role")
        void shouldCheckIfUserHasRole() {
            user.addRole(adminRole);
            
            assertThat(user.hasRole("ADMIN")).isTrue();
            assertThat(user.hasRole("USER")).isFalse();
        }

        @Test
        @DisplayName("Should handle null role gracefully")
        void shouldHandleNullRoleGracefully() {
            user.addRole(null);
            
            assertThat(user.getRoles()).isEmpty();
        }

        @Test
        @DisplayName("Should handle multiple roles")
        void shouldHandleMultipleRoles() {
            user.addRole(adminRole);
            user.addRole(userRole);
            
            assertThat(user.getRoles()).hasSize(2);
            assertThat(user.hasRole("ADMIN")).isTrue();
            assertThat(user.hasRole("USER")).isTrue();
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should return full name when both first and last names are set")
        void shouldReturnFullNameWhenBothNamesAreSet() {
            user.setFirstName("John");
            user.setLastName("Doe");
            
            assertThat(user.getFullName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should return first name when only first name is set")
        void shouldReturnFirstNameWhenOnlyFirstNameIsSet() {
            user.setFirstName("John");
            
            assertThat(user.getFullName()).isEqualTo("John");
        }

        @Test
        @DisplayName("Should return last name when only last name is set")
        void shouldReturnLastNameWhenOnlyLastNameIsSet() {
            user.setLastName("Doe");
            
            assertThat(user.getFullName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("Should return username when no names are set")
        void shouldReturnUsernameWhenNoNamesAreSet() {
            assertThat(user.getFullName()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should have default boolean values")
        void shouldHaveDefaultBooleanValues() {
            User newUser = new User();
            
            assertThat(newUser.isEnabled()).isTrue();
            assertThat(newUser.isAccountNonExpired()).isTrue();
            assertThat(newUser.isAccountNonLocked()).isTrue();
            assertThat(newUser.isCredentialsNonExpired()).isTrue();
            assertThat(newUser.isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("Equality and Hash Code Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when IDs are the same")
        void shouldBeEqualWhenIdsAreTheSame() {
            User user1 = new User();
            user1.setId(1L);
            user1.setUsername("user1");
            
            User user2 = new User();
            user2.setId(1L);
            user2.setUsername("user2");
            
            assertThat(user1).isEqualTo(user2);
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }

        @Test
        @DisplayName("Should be equal when usernames are the same and IDs are null")
        void shouldBeEqualWhenUsernamesAreTheSameAndIdsAreNull() {
            User user1 = new User();
            user1.setUsername("testuser");
            
            User user2 = new User();
            user2.setUsername("testuser");
            
            assertThat(user1).isEqualTo(user2);
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when usernames are different")
        void shouldNotBeEqualWhenUsernamesAreDifferent() {
            User user1 = new User();
            user1.setUsername("user1");
            
            User user2 = new User();
            user2.setUsername("user2");
            
            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            assertThat(user).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different class")
        void shouldNotBeEqualToDifferentClass() {
            assertThat(user).isNotEqualTo("string");
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create user with default constructor")
        void shouldCreateUserWithDefaultConstructor() {
            User newUser = new User();
            
            assertThat(newUser.getUsername()).isNull();
            assertThat(newUser.getPassword()).isNull();
            assertThat(newUser.getEmail()).isNull();
            assertThat(newUser.getRoles()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should create user with parameterized constructor")
        void shouldCreateUserWithParameterizedConstructor() {
            User newUser = new User("testuser", "password123", "test@example.com");
            
            assertThat(newUser.getUsername()).isEqualTo("testuser");
            assertThat(newUser.getPassword()).isEqualTo("password123");
            assertThat(newUser.getEmail()).isEqualTo("test@example.com");
            assertThat(newUser.getRoles()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should generate meaningful toString")
        void shouldGenerateMeaningfulToString() {
            user.setId(1L);
            user.setFirstName("John");
            user.setLastName("Doe");
            
            String toString = user.toString();
            
            assertThat(toString).contains("User{");
            assertThat(toString).contains("id=1");
            assertThat(toString).contains("username='testuser'");
            assertThat(toString).contains("email='test@example.com'");
            assertThat(toString).contains("firstName='John'");
            assertThat(toString).contains("lastName='Doe'");
        }
    }
}