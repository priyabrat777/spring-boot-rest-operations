package com.enterprise.api.repository;

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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UserRepository.
 * Tests user-specific repository operations including role-based queries.
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User disabledUser;
    private User deletedUser;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // Create roles
        adminRole = new Role("ADMIN", "Administrator role");
        userRole = new Role("USER", "Regular user role");
        
        entityManager.persistAndFlush(adminRole);
        entityManager.persistAndFlush(userRole);

        // Create users
        user1 = new User("testuser1", "password123", "test1@example.com");
        user1.setFirstName("Test");
        user1.setLastName("User One");
        user1.setEnabled(true);
        user1.addRole(adminRole);
        user1.addRole(userRole);

        user2 = new User("testuser2", "password456", "test2@example.com");
        user2.setFirstName("Test");
        user2.setLastName("User Two");
        user2.setEnabled(true);
        user2.addRole(userRole);

        disabledUser = new User("disableduser", "password789", "disabled@example.com");
        disabledUser.setFirstName("Disabled");
        disabledUser.setLastName("User");
        disabledUser.setEnabled(false);
        disabledUser.addRole(userRole);

        deletedUser = new User("deleteduser", "passwordabc", "deleted@example.com");
        deletedUser.setFirstName("Deleted");
        deletedUser.setLastName("User");
        deletedUser.setEnabled(true);
        deletedUser.setDeleted(true);
        deletedUser.addRole(userRole);

        // Persist entities
        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);
        entityManager.persistAndFlush(disabledUser);
        entityManager.persistAndFlush(deletedUser);
        entityManager.clear();
    }

    @Test
    void findByUsernameActive_ShouldReturnActiveUser() {
        // When
        Optional<User> foundUser = userRepository.findByUsernameActive("testuser1");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser1");
        assertThat(foundUser.get().isDeleted()).isFalse();
    }

    @Test
    void findByUsernameActive_ShouldNotReturnDeletedUser() {
        // When
        Optional<User> foundUser = userRepository.findByUsernameActive("deleteduser");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    void findByEmailActive_ShouldReturnActiveUser() {
        // When
        Optional<User> foundUser = userRepository.findByEmailActive("test1@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test1@example.com");
        assertThat(foundUser.get().isDeleted()).isFalse();
    }

    @Test
    void findByUsernameOrEmailActive_ShouldReturnUserByUsername() {
        // When
        Optional<User> foundUser = userRepository.findByUsernameOrEmailActive("testuser1");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser1");
    }

    @Test
    void findByUsernameOrEmailActive_ShouldReturnUserByEmail() {
        // When
        Optional<User> foundUser = userRepository.findByUsernameOrEmailActive("test2@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test2@example.com");
    }

    @Test
    void existsByUsernameActive_ShouldReturnTrueForActiveUser() {
        // When
        boolean exists = userRepository.existsByUsernameActive("testuser1");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUsernameActive_ShouldReturnFalseForDeletedUser() {
        // When
        boolean exists = userRepository.existsByUsernameActive("deleteduser");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmailActive_ShouldReturnTrueForActiveUser() {
        // When
        boolean exists = userRepository.existsByEmailActive("test1@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmailActive_ShouldReturnFalseForDeletedUser() {
        // When
        boolean exists = userRepository.existsByEmailActive("deleted@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void findByRoleNameActive_ShouldReturnUsersWithRole() {
        // When
        List<User> adminUsers = userRepository.findByRoleNameActive("ADMIN");
        List<User> regularUsers = userRepository.findByRoleNameActive("USER");

        // Then
        assertThat(adminUsers).hasSize(1);
        assertThat(adminUsers.get(0).getUsername()).isEqualTo("testuser1");
        
        assertThat(regularUsers).hasSize(3); // user1, user2, disabledUser (deletedUser is excluded)
        assertThat(regularUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("testuser1", "testuser2", "disableduser");
    }

    @Test
    void findByRoleNameActiveWithPagination_ShouldReturnPagedUsersWithRole() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<User> userPage = userRepository.findByRoleNameActive("USER", pageable);

        // Then
        assertThat(userPage.getTotalElements()).isEqualTo(3);
        assertThat(userPage.getContent()).hasSize(2);
    }

    @Test
    void findByEnabledTrueActive_ShouldReturnOnlyEnabledUsers() {
        // When
        List<User> enabledUsers = userRepository.findByEnabledTrueActive();

        // Then
        assertThat(enabledUsers).hasSize(2);
        assertThat(enabledUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("testuser1", "testuser2");
        assertThat(enabledUsers).allMatch(User::isEnabled);
    }

    @Test
    void findByEnabledTrueActiveWithPagination_ShouldReturnPagedEnabledUsers() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<User> enabledPage = userRepository.findByEnabledTrueActive(pageable);

        // Then
        assertThat(enabledPage.getTotalElements()).isEqualTo(2);
        assertThat(enabledPage.getContent()).hasSize(1);
        assertThat(enabledPage.getContent().get(0).isEnabled()).isTrue();
    }

    @Test
    void findByUsernameContainingIgnoreCaseActive_ShouldReturnMatchingUsers() {
        // When
        List<User> matchingUsers = userRepository.findByUsernameContainingIgnoreCaseActive("testuser");

        // Then
        assertThat(matchingUsers).hasSize(2);
        assertThat(matchingUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("testuser1", "testuser2");
    }

    @Test
    void findByEmailContainingIgnoreCaseActive_ShouldReturnMatchingUsers() {
        // When
        List<User> matchingUsers = userRepository.findByEmailContainingIgnoreCaseActive("test");

        // Then
        assertThat(matchingUsers).hasSize(2);
        assertThat(matchingUsers).extracting(User::getEmail)
                .containsExactlyInAnyOrder("test1@example.com", "test2@example.com");
    }

    @Test
    void findByFullNameContainingIgnoreCaseActive_ShouldReturnMatchingUsers() {
        // When
        List<User> matchingUsers = userRepository.findByFullNameContainingIgnoreCaseActive("Test User");

        // Then
        assertThat(matchingUsers).hasSize(2);
        assertThat(matchingUsers).extracting(User::getFirstName)
                .containsOnly("Test");
    }

    @Test
    void findByCreatedDateAfterActive_ShouldReturnRecentUsers() {
        // Given
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        // When
        List<User> recentUsers = userRepository.findByCreatedDateAfterActive(yesterday);

        // Then
        assertThat(recentUsers).hasSize(3); // All active users created today
        assertThat(recentUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("testuser1", "testuser2", "disableduser");
    }

    @Test
    void updateEnabledStatus_ShouldUpdateUserStatus() {
        // When
        int affectedRows = userRepository.updateEnabledStatus(user1.getId(), false);

        // Then
        assertThat(affectedRows).isEqualTo(1);
        
        entityManager.clear();
        User updatedUser = entityManager.find(User.class, user1.getId());
        assertThat(updatedUser.isEnabled()).isFalse();
        assertThat(updatedUser.getLastModifiedDate()).isNotNull();
    }

    @Test
    void updateAccountLockStatus_ShouldUpdateLockStatus() {
        // When
        int affectedRows = userRepository.updateAccountLockStatus(user1.getId(), false);

        // Then
        assertThat(affectedRows).isEqualTo(1);
        
        entityManager.clear();
        User updatedUser = entityManager.find(User.class, user1.getId());
        assertThat(updatedUser.isAccountNonLocked()).isFalse();
    }

    @Test
    void updateCredentialsExpirationStatus_ShouldUpdateCredentialsStatus() {
        // When
        int affectedRows = userRepository.updateCredentialsExpirationStatus(user1.getId(), false);

        // Then
        assertThat(affectedRows).isEqualTo(1);
        
        entityManager.clear();
        User updatedUser = entityManager.find(User.class, user1.getId());
        assertThat(updatedUser.isCredentialsNonExpired()).isFalse();
    }

    @Test
    void updatePassword_ShouldUpdateUserPassword() {
        // Given
        String newPassword = "newpassword123";

        // When
        int affectedRows = userRepository.updatePassword(user1.getId(), newPassword);

        // Then
        assertThat(affectedRows).isEqualTo(1);
        
        entityManager.clear();
        User updatedUser = entityManager.find(User.class, user1.getId());
        assertThat(updatedUser.getPassword()).isEqualTo(newPassword);
    }

    @Test
    void batchUpdateEnabledStatus_ShouldUpdateMultipleUsers() {
        // Given
        List<Long> userIds = Arrays.asList(user1.getId(), user2.getId());

        // When
        int affectedRows = userRepository.batchUpdateEnabledStatus(userIds, false);

        // Then
        assertThat(affectedRows).isEqualTo(2);
        
        entityManager.clear();
        User updatedUser1 = entityManager.find(User.class, user1.getId());
        User updatedUser2 = entityManager.find(User.class, user2.getId());
        
        assertThat(updatedUser1.isEnabled()).isFalse();
        assertThat(updatedUser2.isEnabled()).isFalse();
    }

    @Test
    void countByRoleNameActive_ShouldReturnCorrectCount() {
        // When
        long adminCount = userRepository.countByRoleNameActive("ADMIN");
        long userCount = userRepository.countByRoleNameActive("USER");

        // Then
        assertThat(adminCount).isEqualTo(1);
        assertThat(userCount).isEqualTo(3); // user1, user2, disabledUser
    }

    @Test
    void countByEnabledTrueActive_ShouldReturnCorrectCount() {
        // When
        long enabledCount = userRepository.countByEnabledTrueActive();

        // Then
        assertThat(enabledCount).isEqualTo(2);
    }

    @Test
    void countByEnabledFalseActive_ShouldReturnCorrectCount() {
        // When
        long disabledCount = userRepository.countByEnabledFalseActive();

        // Then
        assertThat(disabledCount).isEqualTo(1);
    }

    @Test
    void findUsersWithMultipleRoles_ShouldReturnUsersWithManyRoles() {
        // When
        List<User> usersWithMultipleRoles = userRepository.findUsersWithMultipleRoles(2);

        // Then
        assertThat(usersWithMultipleRoles).hasSize(1);
        assertThat(usersWithMultipleRoles.get(0).getUsername()).isEqualTo("testuser1");
    }

    @Test
    void findUsersWithoutRoles_ShouldReturnUsersWithNoRoles() {
        // Given - Create a user without roles
        User userWithoutRoles = new User("noroleuser", "password", "norole@example.com");
        userWithoutRoles.setEnabled(true);
        entityManager.persistAndFlush(userWithoutRoles);
        entityManager.clear();

        // When
        List<User> usersWithoutRoles = userRepository.findUsersWithoutRoles();

        // Then
        assertThat(usersWithoutRoles).hasSize(1);
        assertThat(usersWithoutRoles.get(0).getUsername()).isEqualTo("noroleuser");
    }

    @Test
    void findByAccountStatus_ShouldReturnUsersWithSpecificStatus() {
        // When
        List<User> enabledUsers = userRepository.findByAccountStatus(true, true, true, true);

        // Then
        assertThat(enabledUsers).hasSize(2);
        assertThat(enabledUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("testuser1", "testuser2");
    }

    @Test
    void findByAccountStatus_ShouldReturnDisabledUsers() {
        // When
        List<User> disabledUsers = userRepository.findByAccountStatus(false, true, true, true);

        // Then
        assertThat(disabledUsers).hasSize(1);
        assertThat(disabledUsers.get(0).getUsername()).isEqualTo("disableduser");
    }
}