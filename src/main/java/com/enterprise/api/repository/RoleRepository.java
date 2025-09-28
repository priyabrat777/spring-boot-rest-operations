package com.enterprise.api.repository;

import com.enterprise.api.entity.Role;
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
 * Repository interface for Role entity operations.
 * Extends BaseRepository to inherit common operations and adds role-specific queries.
 * 
 * Requirements addressed:
 * - 5.1: CRUD operations, custom queries, and specifications
 * - 5.2: @Query annotations and method name derivation
 * - 5.3: Pageable and Sort parameters
 * - 5.4: Batch operations
 * - 3.6: Soft delete and active record filtering
 */
@Repository
public interface RoleRepository extends BaseRepository<Role, Long> {

    /**
     * Find an active role by name.
     * 
     * @param name the role name to search for
     * @return optional containing the role if found and active
     */
    @Query("SELECT r FROM Role r WHERE r.name = :name AND r.deleted = false")
    Optional<Role> findByNameActive(@Param("name") String name);

    /**
     * Check if role name exists among active roles.
     * 
     * @param name the role name to check
     * @return true if role name exists
     */
    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE r.name = :name AND r.deleted = false")
    boolean existsByNameActive(@Param("name") String name);

    /**
     * Find active system roles.
     * 
     * @return list of active system roles
     */
    @Query("SELECT r FROM Role r WHERE r.systemRole = true AND r.deleted = false")
    List<Role> findSystemRolesActive();

    /**
     * Find active non-system roles.
     * 
     * @return list of active non-system roles
     */
    @Query("SELECT r FROM Role r WHERE r.systemRole = false AND r.deleted = false")
    List<Role> findNonSystemRolesActive();

    /**
     * Find active roles by partial name match (case-insensitive).
     * 
     * @param name partial role name to search for
     * @return list of matching roles
     */
    @Query("SELECT r FROM Role r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')) AND r.deleted = false")
    List<Role> findByNameContainingIgnoreCaseActive(@Param("name") String name);

    /**
     * Find active roles by partial description match (case-insensitive).
     * 
     * @param description partial description to search for
     * @return list of matching roles
     */
    @Query("SELECT r FROM Role r WHERE LOWER(r.description) LIKE LOWER(CONCAT('%', :description, '%')) AND r.deleted = false")
    List<Role> findByDescriptionContainingIgnoreCaseActive(@Param("description") String description);

    /**
     * Find active roles assigned to a specific user.
     * 
     * @param userId the user ID
     * @return list of roles assigned to the user
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN r.users u WHERE u.id = :userId AND r.deleted = false AND u.deleted = false")
    List<Role> findByUserIdActive(@Param("userId") Long userId);

    /**
     * Find active roles assigned to a specific user by username.
     * 
     * @param username the username
     * @return list of roles assigned to the user
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN r.users u WHERE u.username = :username AND r.deleted = false AND u.deleted = false")
    List<Role> findByUsernameActive(@Param("username") String username);

    /**
     * Find active roles with a specific permission.
     * 
     * @param permissionName the permission name
     * @return list of roles with the permission
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN r.permissions p WHERE p.name = :permissionName AND r.deleted = false AND p.deleted = false")
    List<Role> findByPermissionNameActive(@Param("permissionName") String permissionName);

    /**
     * Find active roles with permission for specific resource and action.
     * 
     * @param resource the resource name
     * @param action the action name
     * @return list of roles with the permission
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN r.permissions p WHERE p.resource = :resource AND p.action = :action AND r.deleted = false AND p.deleted = false")
    List<Role> findByResourceAndActionActive(@Param("resource") String resource, @Param("action") String action);

    /**
     * Find active roles created after a specific date.
     * 
     * @param date the date to search after
     * @return list of roles created after the date
     */
    @Query("SELECT r FROM Role r WHERE r.createdDate > :date AND r.deleted = false")
    List<Role> findByCreatedDateAfterActive(@Param("date") LocalDateTime date);

    /**
     * Find active roles with user count greater than specified value.
     * 
     * @param minUserCount minimum number of users
     * @return list of roles with many users
     */
    @Query("SELECT r FROM Role r WHERE SIZE(r.users) >= :minUserCount AND r.deleted = false")
    List<Role> findRolesWithManyUsers(@Param("minUserCount") int minUserCount);

    /**
     * Find active roles without any users.
     * 
     * @return list of roles without users
     */
    @Query("SELECT r FROM Role r WHERE SIZE(r.users) = 0 AND r.deleted = false")
    List<Role> findRolesWithoutUsers();

    /**
     * Find active roles without any permissions.
     * 
     * @return list of roles without permissions
     */
    @Query("SELECT r FROM Role r WHERE SIZE(r.permissions) = 0 AND r.deleted = false")
    List<Role> findRolesWithoutPermissions();

    /**
     * Find active roles with permission count greater than specified value.
     * 
     * @param minPermissionCount minimum number of permissions
     * @return list of roles with many permissions
     */
    @Query("SELECT r FROM Role r WHERE SIZE(r.permissions) >= :minPermissionCount AND r.deleted = false")
    List<Role> findRolesWithManyPermissions(@Param("minPermissionCount") int minPermissionCount);

    /**
     * Count active roles by system role flag.
     * 
     * @param systemRole the system role flag
     * @return count of roles
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.systemRole = :systemRole AND r.deleted = false")
    long countBySystemRoleActive(@Param("systemRole") boolean systemRole);

    /**
     * Count active roles assigned to a specific user.
     * 
     * @param userId the user ID
     * @return count of roles assigned to the user
     */
    @Query("SELECT COUNT(DISTINCT r) FROM Role r JOIN r.users u WHERE u.id = :userId AND r.deleted = false AND u.deleted = false")
    long countByUserIdActive(@Param("userId") Long userId);

    /**
     * Count active roles with a specific permission.
     * 
     * @param permissionName the permission name
     * @return count of roles with the permission
     */
    @Query("SELECT COUNT(DISTINCT r) FROM Role r JOIN r.permissions p WHERE p.name = :permissionName AND r.deleted = false AND p.deleted = false")
    long countByPermissionNameActive(@Param("permissionName") String permissionName);

    /**
     * Update role's system role flag.
     * 
     * @param id the role ID
     * @param systemRole the new system role flag
     * @return number of affected rows
     */
    @Modifying
    @Query("UPDATE Role r SET r.systemRole = :systemRole, r.lastModifiedDate = CURRENT_TIMESTAMP WHERE r.id = :id AND r.deleted = false")
    int updateSystemRoleFlag(@Param("id") Long id, @Param("systemRole") boolean systemRole);

    /**
     * Update role's description.
     * 
     * @param id the role ID
     * @param description the new description
     * @return number of affected rows
     */
    @Modifying
    @Query("UPDATE Role r SET r.description = :description, r.lastModifiedDate = CURRENT_TIMESTAMP WHERE r.id = :id AND r.deleted = false")
    int updateDescription(@Param("id") Long id, @Param("description") String description);

    /**
     * Batch update system role flag for multiple roles.
     * 
     * @param ids the role IDs
     * @param systemRole the new system role flag
     * @return number of affected rows
     */
    @Modifying
    @Query("UPDATE Role r SET r.systemRole = :systemRole, r.lastModifiedDate = CURRENT_TIMESTAMP WHERE r.id IN :ids AND r.deleted = false")
    int batchUpdateSystemRoleFlag(@Param("ids") List<Long> ids, @Param("systemRole") boolean systemRole);

    /**
     * Find roles that are not assigned to any user and are not system roles.
     * These might be candidates for cleanup.
     * 
     * @return list of unused non-system roles
     */
    @Query("SELECT r FROM Role r WHERE SIZE(r.users) = 0 AND r.systemRole = false AND r.deleted = false")
    List<Role> findUnusedNonSystemRoles();

    /**
     * Find active roles with pagination, ordered by name.
     * 
     * @param pageable pagination information
     * @return page of roles ordered by name
     */
    @Query("SELECT r FROM Role r WHERE r.deleted = false ORDER BY r.name")
    Page<Role> findAllActiveOrderByName(Pageable pageable);

    /**
     * Find active roles by system role flag with pagination.
     * 
     * @param systemRole the system role flag
     * @param pageable pagination information
     * @return page of roles
     */
    @Query("SELECT r FROM Role r WHERE r.systemRole = :systemRole AND r.deleted = false")
    Page<Role> findBySystemRoleActive(@Param("systemRole") boolean systemRole, Pageable pageable);
}