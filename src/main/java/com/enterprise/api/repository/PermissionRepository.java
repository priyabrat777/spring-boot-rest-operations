package com.enterprise.api.repository;

import com.enterprise.api.entity.Permission;
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
 * Repository interface for Permission entity operations.
 * Extends BaseRepository to inherit common operations and adds permission-specific queries.
 * 
 * Requirements addressed:
 * - 5.1: CRUD operations, custom queries, and specifications
 * - 5.2: @Query annotations and method name derivation
 * - 5.3: Pageable and Sort parameters
 * - 5.4: Batch operations
 * - 3.6: Soft delete and active record filtering
 */
@Repository
public interface PermissionRepository extends BaseRepository<Permission, Long> {

    /**
     * Find an active permission by name.
     * 
     * @param name the permission name to search for
     * @return optional containing the permission if found and active
     */
    @Query("SELECT p FROM Permission p WHERE p.name = :name AND p.deleted = false")
    Optional<Permission> findByNameActive(@Param("name") String name);

    /**
     * Find an active permission by resource and action.
     * 
     * @param resource the resource name
     * @param action the action name
     * @return optional containing the permission if found and active
     */
    @Query("SELECT p FROM Permission p WHERE p.resource = :resource AND p.action = :action AND p.deleted = false")
    Optional<Permission> findByResourceAndActionActive(@Param("resource") String resource, @Param("action") String action);

    /**
     * Check if permission name exists among active permissions.
     * 
     * @param name the permission name to check
     * @return true if permission name exists
     */
    @Query("SELECT COUNT(p) > 0 FROM Permission p WHERE p.name = :name AND p.deleted = false")
    boolean existsByNameActive(@Param("name") String name);

    /**
     * Check if permission exists by resource and action among active permissions.
     * 
     * @param resource the resource name
     * @param action the action name
     * @return true if permission exists
     */
    @Query("SELECT COUNT(p) > 0 FROM Permission p WHERE p.resource = :resource AND p.action = :action AND p.deleted = false")
    boolean existsByResourceAndActionActive(@Param("resource") String resource, @Param("action") String action);

    /**
     * Find active permissions by resource.
     * 
     * @param resource the resource name
     * @return list of permissions for the resource
     */
    @Query("SELECT p FROM Permission p WHERE p.resource = :resource AND p.deleted = false")
    List<Permission> findByResourceActive(@Param("resource") String resource);

    /**
     * Find active permissions by action.
     * 
     * @param action the action name
     * @return list of permissions for the action
     */
    @Query("SELECT p FROM Permission p WHERE p.action = :action AND p.deleted = false")
    List<Permission> findByActionActive(@Param("action") String action);

    /**
     * Find active system permissions.
     * 
     * @return list of active system permissions
     */
    @Query("SELECT p FROM Permission p WHERE p.systemPermission = true AND p.deleted = false")
    List<Permission> findSystemPermissionsActive();

    /**
     * Find active non-system permissions.
     * 
     * @return list of active non-system permissions
     */
    @Query("SELECT p FROM Permission p WHERE p.systemPermission = false AND p.deleted = false")
    List<Permission> findNonSystemPermissionsActive();

    /**
     * Find active permissions by partial name match (case-insensitive).
     * 
     * @param name partial permission name to search for
     * @return list of matching permissions
     */
    @Query("SELECT p FROM Permission p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.deleted = false")
    List<Permission> findByNameContainingIgnoreCaseActive(@Param("name") String name);

    /**
     * Find active permissions by partial description match (case-insensitive).
     * 
     * @param description partial description to search for
     * @return list of matching permissions
     */
    @Query("SELECT p FROM Permission p WHERE LOWER(p.description) LIKE LOWER(CONCAT('%', :description, '%')) AND p.deleted = false")
    List<Permission> findByDescriptionContainingIgnoreCaseActive(@Param("description") String description);

    /**
     * Find active permissions assigned to a specific role.
     * 
     * @param roleId the role ID
     * @return list of permissions assigned to the role
     */
    @Query("SELECT DISTINCT p FROM Permission p JOIN p.roles r WHERE r.id = :roleId AND p.deleted = false AND r.deleted = false")
    List<Permission> findByRoleIdActive(@Param("roleId") Long roleId);

    /**
     * Find active permissions assigned to a specific role by role name.
     * 
     * @param roleName the role name
     * @return list of permissions assigned to the role
     */
    @Query("SELECT DISTINCT p FROM Permission p JOIN p.roles r WHERE r.name = :roleName AND p.deleted = false AND r.deleted = false")
    List<Permission> findByRoleNameActive(@Param("roleName") String roleName);

    /**
     * Find active permissions for a specific user (through roles).
     * 
     * @param userId the user ID
     * @return list of permissions available to the user
     */
    @Query("SELECT DISTINCT p FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.id = :userId AND p.deleted = false AND r.deleted = false AND u.deleted = false")
    List<Permission> findByUserIdActive(@Param("userId") Long userId);

    /**
     * Find active permissions for a specific user by username (through roles).
     * 
     * @param username the username
     * @return list of permissions available to the user
     */
    @Query("SELECT DISTINCT p FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.username = :username AND p.deleted = false AND r.deleted = false AND u.deleted = false")
    List<Permission> findByUsernameActive(@Param("username") String username);

    /**
     * Find active permissions created after a specific date.
     * 
     * @param date the date to search after
     * @return list of permissions created after the date
     */
    @Query("SELECT p FROM Permission p WHERE p.createdDate > :date AND p.deleted = false")
    List<Permission> findByCreatedDateAfterActive(@Param("date") LocalDateTime date);

    /**
     * Find active permissions without any roles.
     * 
     * @return list of permissions without roles
     */
    @Query("SELECT p FROM Permission p WHERE SIZE(p.roles) = 0 AND p.deleted = false")
    List<Permission> findPermissionsWithoutRoles();

    /**
     * Find active permissions with role count greater than specified value.
     * 
     * @param minRoleCount minimum number of roles
     * @return list of permissions with many roles
     */
    @Query("SELECT p FROM Permission p WHERE SIZE(p.roles) >= :minRoleCount AND p.deleted = false")
    List<Permission> findPermissionsWithManyRoles(@Param("minRoleCount") int minRoleCount);

    /**
     * Find all distinct active resources.
     * 
     * @return list of distinct resource names
     */
    @Query("SELECT DISTINCT p.resource FROM Permission p WHERE p.deleted = false ORDER BY p.resource")
    List<String> findDistinctResourcesActive();

    /**
     * Find all distinct active actions.
     * 
     * @return list of distinct action names
     */
    @Query("SELECT DISTINCT p.action FROM Permission p WHERE p.deleted = false ORDER BY p.action")
    List<String> findDistinctActionsActive();

    /**
     * Find all distinct active actions for a specific resource.
     * 
     * @param resource the resource name
     * @return list of distinct action names for the resource
     */
    @Query("SELECT DISTINCT p.action FROM Permission p WHERE p.resource = :resource AND p.deleted = false ORDER BY p.action")
    List<String> findDistinctActionsByResourceActive(@Param("resource") String resource);

    /**
     * Count active permissions by system permission flag.
     * 
     * @param systemPermission the system permission flag
     * @return count of permissions
     */
    @Query("SELECT COUNT(p) FROM Permission p WHERE p.systemPermission = :systemPermission AND p.deleted = false")
    long countBySystemPermissionActive(@Param("systemPermission") boolean systemPermission);

    /**
     * Count active permissions by resource.
     * 
     * @param resource the resource name
     * @return count of permissions for the resource
     */
    @Query("SELECT COUNT(p) FROM Permission p WHERE p.resource = :resource AND p.deleted = false")
    long countByResourceActive(@Param("resource") String resource);

    /**
     * Count active permissions assigned to a specific role.
     * 
     * @param roleId the role ID
     * @return count of permissions assigned to the role
     */
    @Query("SELECT COUNT(DISTINCT p) FROM Permission p JOIN p.roles r WHERE r.id = :roleId AND p.deleted = false AND r.deleted = false")
    long countByRoleIdActive(@Param("roleId") Long roleId);

    /**
     * Update permission's system permission flag.
     * 
     * @param id the permission ID
     * @param systemPermission the new system permission flag
     * @return number of affected rows
     */
    @Modifying
    @Query("UPDATE Permission p SET p.systemPermission = :systemPermission, p.lastModifiedDate = CURRENT_TIMESTAMP WHERE p.id = :id AND p.deleted = false")
    int updateSystemPermissionFlag(@Param("id") Long id, @Param("systemPermission") boolean systemPermission);

    /**
     * Update permission's description.
     * 
     * @param id the permission ID
     * @param description the new description
     * @return number of affected rows
     */
    @Modifying
    @Query("UPDATE Permission p SET p.description = :description, p.lastModifiedDate = CURRENT_TIMESTAMP WHERE p.id = :id AND p.deleted = false")
    int updateDescription(@Param("id") Long id, @Param("description") String description);

    /**
     * Batch update system permission flag for multiple permissions.
     * 
     * @param ids the permission IDs
     * @param systemPermission the new system permission flag
     * @return number of affected rows
     */
    @Modifying
    @Query("UPDATE Permission p SET p.systemPermission = :systemPermission, p.lastModifiedDate = CURRENT_TIMESTAMP WHERE p.id IN :ids AND p.deleted = false")
    int batchUpdateSystemPermissionFlag(@Param("ids") List<Long> ids, @Param("systemPermission") boolean systemPermission);

    /**
     * Find permissions that are not assigned to any role and are not system permissions.
     * These might be candidates for cleanup.
     * 
     * @return list of unused non-system permissions
     */
    @Query("SELECT p FROM Permission p WHERE SIZE(p.roles) = 0 AND p.systemPermission = false AND p.deleted = false")
    List<Permission> findUnusedNonSystemPermissions();

    /**
     * Find active permissions with pagination, ordered by resource and action.
     * 
     * @param pageable pagination information
     * @return page of permissions ordered by resource and action
     */
    @Query("SELECT p FROM Permission p WHERE p.deleted = false ORDER BY p.resource, p.action")
    Page<Permission> findAllActiveOrderByResourceAndAction(Pageable pageable);

    /**
     * Find active permissions by system permission flag with pagination.
     * 
     * @param systemPermission the system permission flag
     * @param pageable pagination information
     * @return page of permissions
     */
    @Query("SELECT p FROM Permission p WHERE p.systemPermission = :systemPermission AND p.deleted = false")
    Page<Permission> findBySystemPermissionActive(@Param("systemPermission") boolean systemPermission, Pageable pageable);

    /**
     * Find active permissions by resource with pagination.
     * 
     * @param resource the resource name
     * @param pageable pagination information
     * @return page of permissions for the resource
     */
    @Query("SELECT p FROM Permission p WHERE p.resource = :resource AND p.deleted = false")
    Page<Permission> findByResourceActive(@Param("resource") String resource, Pageable pageable);
}