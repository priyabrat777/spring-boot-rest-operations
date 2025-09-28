package com.enterprise.api.repository;

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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for BaseRepository functionality using User entity as test subject.
 * Tests common repository operations including soft delete and active record filtering.
 * 
 * Requirements addressed:
 * - 5.1: CRUD operations, custom queries, and specifications
 * - 5.3: Pageable and Sort parameters
 * - 5.4: Batch operations
 * - 3.6: Soft delete and active record filtering
 */
@DataJpaTest
@ActiveProfiles("test")
class BaseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository; // Using UserRepository to test BaseRepository methods

    private User activeUser1;
    private User activeUser2;
    private User deletedUser;

    @BeforeEach
    void setUp() {
        // Create test users
        activeUser1 = new User("activeuser1", "password123", "active1@example.com");
        activeUser1.setFirstName("Active");
        activeUser1.setLastName("User One");
        activeUser1.setEnabled(true);

        activeUser2 = new User("activeuser2", "password123", "active2@example.com");
        activeUser2.setFirstName("Active");
        activeUser2.setLastName("User Two");
        activeUser2.setEnabled(true);

        deletedUser = new User("deleteduser", "password123", "deleted@example.com");
        deletedUser.setFirstName("Deleted");
        deletedUser.setLastName("User");
        deletedUser.setEnabled(false);
        deletedUser.setDeleted(true);

        // Persist entities
        entityManager.persistAndFlush(activeUser1);
        entityManager.persistAndFlush(activeUser2);
        entityManager.persistAndFlush(deletedUser);
        entityManager.clear();
    }

    @Test
    void findAllActive_ShouldReturnOnlyActiveEntities() {
        // When
        List<User> activeUsers = userRepository.findAllActive();

        // Then
        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers).extracting(User::getUsername)
                .containsExactlyInAnyOrder("activeuser1", "activeuser2");
        assertThat(activeUsers).allMatch(user -> !user.isDeleted());
    }

    @Test
    void findAllActiveWithPagination_ShouldReturnPagedActiveEntities() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<User> activePage = userRepository.findAllActive(pageable);

        // Then
        assertThat(activePage.getTotalElements()).isEqualTo(2);
        assertThat(activePage.getContent()).hasSize(1);
        assertThat(activePage.getContent().get(0).isDeleted()).isFalse();
    }

    @Test
    void findByIdActive_ShouldReturnActiveEntity() {
        // When
        Optional<User> foundUser = userRepository.findByIdActive(activeUser1.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("activeuser1");
        assertThat(foundUser.get().isDeleted()).isFalse();
    }

    @Test
    void findByIdActive_ShouldNotReturnDeletedEntity() {
        // When
        Optional<User> foundUser = userRepository.findByIdActive(deletedUser.getId());

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    void existsByIdActive_ShouldReturnTrueForActiveEntity() {
        // When
        boolean exists = userRepository.existsByIdActive(activeUser1.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByIdActive_ShouldReturnFalseForDeletedEntity() {
        // When
        boolean exists = userRepository.existsByIdActive(deletedUser.getId());

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void softDelete_ShouldMarkEntityAsDeleted() {
        // When
        int affectedRows = userRepository.softDelete(activeUser1.getId());

        // Then
        assertThat(affectedRows).isEqualTo(1);
        
        entityManager.clear();
        User deletedUser = entityManager.find(User.class, activeUser1.getId());
        assertThat(deletedUser.isDeleted()).isTrue();
        assertThat(deletedUser.getLastModifiedDate()).isNotNull();
    }

    @Test
    void softDeleteByIds_ShouldMarkMultipleEntitiesAsDeleted() {
        // Given
        List<Long> idsToDelete = Arrays.asList(activeUser1.getId(), activeUser2.getId());

        // When
        int affectedRows = userRepository.softDeleteByIds(idsToDelete);

        // Then
        assertThat(affectedRows).isEqualTo(2);
        
        entityManager.clear();
        User deletedUser1 = entityManager.find(User.class, activeUser1.getId());
        User deletedUser2 = entityManager.find(User.class, activeUser2.getId());
        
        assertThat(deletedUser1.isDeleted()).isTrue();
        assertThat(deletedUser2.isDeleted()).isTrue();
    }

    @Test
    void restore_ShouldRestoreDeletedEntity() {
        // When
        int affectedRows = userRepository.restore(deletedUser.getId());

        // Then
        assertThat(affectedRows).isEqualTo(1);
        
        entityManager.clear();
        User restoredUser = entityManager.find(User.class, deletedUser.getId());
        assertThat(restoredUser.isDeleted()).isFalse();
        assertThat(restoredUser.getLastModifiedDate()).isNotNull();
    }

    @Test
    void findAllWithDeleted_ShouldReturnAllEntities() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> allUsers = userRepository.findAllWithDeleted(pageable);

        // Then
        assertThat(allUsers.getTotalElements()).isEqualTo(3);
        assertThat(allUsers.getContent()).hasSize(3);
    }

    @Test
    void findAllDeleted_ShouldReturnOnlyDeletedEntities() {
        // When
        List<User> deletedUsers = userRepository.findByDeletedTrue();

        // Then
        assertThat(deletedUsers).hasSize(1);
        assertThat(deletedUsers.get(0).getUsername()).isEqualTo("deleteduser");
        assertThat(deletedUsers.get(0).isDeleted()).isTrue();
    }

    @Test
    void findAllDeletedWithPagination_ShouldReturnPagedDeletedEntities() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> deletedPage = userRepository.findByDeletedTrue(pageable);

        // Then
        assertThat(deletedPage.getTotalElements()).isEqualTo(1);
        assertThat(deletedPage.getContent()).hasSize(1);
        assertThat(deletedPage.getContent().get(0).isDeleted()).isTrue();
    }

    @Test
    void countActive_ShouldReturnCorrectCount() {
        // When
        long activeCount = userRepository.countByDeletedFalse();

        // Then
        assertThat(activeCount).isEqualTo(2);
    }

    @Test
    void countDeleted_ShouldReturnCorrectCount() {
        // When
        long deletedCount = userRepository.countByDeletedTrue();

        // Then
        assertThat(deletedCount).isEqualTo(1);
    }

    @Test
    void permanentlyDeleteOldSoftDeleted_ShouldDeleteOldSoftDeletedEntities() {
        // Given - Find the deleted user and set its last modified date to past
        User foundDeletedUser = entityManager.find(User.class, deletedUser.getId());
        foundDeletedUser.setLastModifiedDate(LocalDateTime.now().minusDays(30));
        entityManager.merge(foundDeletedUser);
        entityManager.flush();
        entityManager.clear();

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);

        // When
        int deletedCount = userRepository.permanentlyDeleteOldSoftDeleted(cutoffDate);

        // Then
        assertThat(deletedCount).isEqualTo(1);
        
        // Verify the entity is permanently deleted
        User permanentlyDeletedUser = entityManager.find(User.class, deletedUser.getId());
        assertThat(permanentlyDeletedUser).isNull();
    }

    @Test
    void permanentlyDeleteOldSoftDeleted_ShouldNotDeleteRecentSoftDeletedEntities() {
        // Given - Find the deleted user and set its last modified date to recent
        User foundDeletedUser = entityManager.find(User.class, deletedUser.getId());
        foundDeletedUser.setLastModifiedDate(LocalDateTime.now().minusHours(1));
        entityManager.merge(foundDeletedUser);
        entityManager.flush();
        entityManager.clear();

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);

        // When
        int deletedCount = userRepository.permanentlyDeleteOldSoftDeleted(cutoffDate);

        // Then
        assertThat(deletedCount).isEqualTo(0);
        
        // Verify the entity still exists
        User stillExistingUser = entityManager.find(User.class, deletedUser.getId());
        assertThat(stillExistingUser).isNotNull();
        assertThat(stillExistingUser.isDeleted()).isTrue();
    }

    @Test
    void permanentlyDeleteOldSoftDeleted_ShouldNotDeleteActiveEntities() {
        // Given
        LocalDateTime cutoffDate = LocalDateTime.now().plusDays(1); // Future date to include all entities

        // When
        int deletedCount = userRepository.permanentlyDeleteOldSoftDeleted(cutoffDate);

        // Then
        assertThat(deletedCount).isEqualTo(1); // Only the soft-deleted entity should be affected
        
        // Verify active entities still exist
        User activeUser1Found = entityManager.find(User.class, activeUser1.getId());
        User activeUser2Found = entityManager.find(User.class, activeUser2.getId());
        
        assertThat(activeUser1Found).isNotNull();
        assertThat(activeUser2Found).isNotNull();
        assertThat(activeUser1Found.isDeleted()).isFalse();
        assertThat(activeUser2Found.isDeleted()).isFalse();
    }
}