package com.enterprise.api.repository;

import com.enterprise.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations.
 * Extends BaseRepository to inherit common operations and adds user-specific queries.
 * 
 * Requirements addressed:
 * - 5.1: CRUD operations, custom queries, and specifications
 * - 5.2: @Query annotations and method name derivation
 * - 5.3: Pageable and Sort parameters
 * - 5.4: Batch operations
 * - 3.6: Soft delete and active record filtering
 */
@Repository
public interface UserRepository extends BaseRepository<User, Long> {

    /**
     * Find an active user by username.
     * 
     * @param username the username to search for
     * @return optional containing the user if found and active
     */
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.deleted = false")
    Optional<User> findByUsernameActive(@Param("username") String username);

    /**
     * Find an active user by email.
     * 
     * @param email the email to search for
     * @return optional containing the user if found and active
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deleted = false")
    Optional<User> findByEmailActive(@Param("email") String email);

    /**
     * Find an active user by username or email.
     * 
     * @param usernameOrEmail the username or email to search for
     * @return optional containing the user if found and active
     */
    @Query("SELECT u FROM User u WHERE (u.username = :usernameOrEmail OR u.email = :usernameOrEmail) AND u.deleted = false")
    Optional<User> findByUsernameOrEmailActive(@Param("usernameOrEmail") String usernameOrEmail);

    /**
     * Check if username exists among active users.
     * 
     * @param username the username to check
     * @return true if username exists
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username AND u.deleted = false")
    boolean existsByUsernameActive(@Param("username") String username);

    /**
     * Check if email exists among active users.
     * 
     * @param email the email to check
     * @return true if email exists
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.deleted = false")
    boolean existsByEmailActive(@Param("email") String email);

    /**
     * Find active users by role name.
     * 
     * @param roleName the role name to search for
     * @return list of users with the specified role
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.deleted = false AND r.deleted = false")
    List<User> findByRoleNameActive(@Param("roleName") String roleName);

    /**
     * Find active users by role name with pagination.
     * 
     * @param roleName the role name to search for
     * @param pageable pagination information
     * @return page of users with the specified role
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.deleted = false AND r.deleted = false")
    Page<User> findByRoleNameActive(@Param("roleName") String roleName, Pageable pageable);

    /**
     * Find active enabled users.
     * 
     * @return list of active enabled users
     */
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.deleted = false")
    List<User> findByEnabledTrueActive();

    /**
     * Find active enabled users with pagination.
     * 
     * @param pageable pagination information
     * @return page of active enabled users
     */
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.deleted = false")
    Page<User> findByEnabledTrueActive(Pageable pageable);

    /**
     * Find active users by partial username match (case-insensitive).
     * 
     * @param username partial username to search for
     * @return list of matching users
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')) AND u.deleted = false")
    List<User> findByUsernameContainingIgnoreCaseActive(@Param("username") String username);

    /**
     * Find active users by partial email match (case-insensitive).
     * 
     * @param email partial email to search for
     * @return list of matching users
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')) AND u.deleted = false")
    List<User> findByEmailContainingIgnoreCaseActive(@Param("email") String email);

    /**
     * Find active users by full name search (case-insensitive).
     * 
     * @param name partial name to search for
     * @return list of matching users
     */
    @Query("SELECT u FROM User u WHERE (LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', :name, '%'))) AND u.deleted = false")
    List<User> findByFullNameContainingIgnoreCaseActive(@Param("name") String name);

    /**
     * Find active users created after a specific date.
     * 
     * @param date the date to search after
     * @return list of users created after the date
     */
    @Query("SELECT u FROM User u WHERE u.createdDate > :date AND u.deleted = false")
    List<User> findByCreatedDateAfterActive(@Param("date") LocalDateTime date);

    /**
     * Find active users last modified after a specific date.
     * 
     * @param date the date to search after
     * @return list of users modified after the date
     */
    @Query("SELECT u FROM User u WHERE u.lastModifiedDate > :date AND u.deleted = false")
    List<User> findByLastModifiedDateAfterActive(@Param("date") LocalDateTime date);

    /**
     * Update user's enabled status.
     * 
     * @param id the user ID
     * @param enabled the new enabled status
     * @return number of affected rows
     */
    @Modifying
    @Query("UPDATE User u SET u.enabled = :enabled, u.lastModifiedDate = CURRENT_TIMESTAMP WHERE u.id = :id AND u.deleted = false")
    int updateEnabledStatus(@Param("id") Long id, @Param("enabled") boolean enabled);

    /**
     * Update user's account lock status.
     * 
     * @param id the user ID
     * @param locked the new lock status (true = locked, false = unlocked)
     * @return number of affected rows
     */
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = :unlocked, u.lastModifiedDate = CURRENT_TIMESTAMP WHERE u.id = :id AND u.deleted = false")
    int updateAccountLockStatus(@Param("id") Long id, @Param("unlocked") boolean unlocked);

    /**
     * Update user's credentials expiration status.
     * 
     * @param id the user ID
     * @param credentialsNonExpired the new credentials expiration status
     * @return number of affected rows
     */
    @Modifying
    @Query("UPDATE User u SET u.credentialsNonExpired = :credentialsNonExpired, u.lastModifiedDate = CURRENT_TIMESTAMP WHERE u.id = :id AND u.deleted = false")
    int updateCredentialsExpirationStatus(@Param("id") Long id, @Param("credentialsNonExpired") boolean credentialsNonExpired);

    /**
     * Update user's password.
     * 
     * @param id the user ID
     * @param password the new encoded password
     * @return number of affected rows
     */
    @Modifying
    @Query("UPDATE User u SET u.password = :password, u.lastModifiedDate = CURRENT_TIMESTAMP WHERE u.id = :id AND u.deleted = false")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /**
     * Batch enable/disable users by IDs.
     * 
     * @param ids the user IDs
     * @param enabled the new enabled status
     * @return number of affected rows
     */
    @Modifying
    @Query("UPDATE User u SET u.enabled = :enabled, u.lastModifiedDate = CURRENT_TIMESTAMP WHERE u.id IN :ids AND u.deleted = false")
    int batchUpdateEnabledStatus(@Param("ids") List<Long> ids, @Param("enabled") boolean enabled);

    /**
     * Count active users by role name.
     * 
     * @param roleName the role name
     * @return count of users with the role
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.deleted = false AND r.deleted = false")
    long countByRoleNameActive(@Param("roleName") String roleName);

    /**
     * Count active enabled users.
     * 
     * @return count of active enabled users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true AND u.deleted = false")
    long countByEnabledTrueActive();

    /**
     * Count active disabled users.
     * 
     * @return count of active disabled users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = false AND u.deleted = false")
    long countByEnabledFalseActive();

    /**
     * Find users with multiple roles (more than specified count).
     * 
     * @param minRoleCount minimum number of roles
     * @return list of users with multiple roles
     */
    @Query("SELECT u FROM User u WHERE SIZE(u.roles) >= :minRoleCount AND u.deleted = false")
    List<User> findUsersWithMultipleRoles(@Param("minRoleCount") int minRoleCount);

    /**
     * Find users without any roles.
     * 
     * @return list of users without roles
     */
    @Query("SELECT u FROM User u WHERE SIZE(u.roles) = 0 AND u.deleted = false")
    List<User> findUsersWithoutRoles();

    /**
     * Find active users with specific account status combination.
     * 
     * @param enabled account enabled status
     * @param accountNonExpired account non-expired status
     * @param accountNonLocked account non-locked status
     * @param credentialsNonExpired credentials non-expired status
     * @return list of users matching the criteria
     */
    @Query("SELECT u FROM User u WHERE u.enabled = :enabled AND u.accountNonExpired = :accountNonExpired AND u.accountNonLocked = :accountNonLocked AND u.credentialsNonExpired = :credentialsNonExpired AND u.deleted = false")
    List<User> findByAccountStatus(@Param("enabled") boolean enabled, 
                                   @Param("accountNonExpired") boolean accountNonExpired,
                                   @Param("accountNonLocked") boolean accountNonLocked,
                                   @Param("credentialsNonExpired") boolean credentialsNonExpired);
}