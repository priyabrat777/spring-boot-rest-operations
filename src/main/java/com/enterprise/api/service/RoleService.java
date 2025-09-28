package com.enterprise.api.service;

import com.enterprise.api.dto.request.CreateRoleRequest;
import com.enterprise.api.dto.request.UpdateRoleRequest;
import com.enterprise.api.dto.response.RoleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for role management operations.
 * Handles CRUD operations for roles with RBAC-based access control.
 * 
 * Requirements addressed:
 * - 4.6: Role-based access control for role management
 * - 4.7: Role creation and permission assignment
 */
public interface RoleService {

    /**
     * Creates a new role.
     * 
     * @param createRoleRequest the role creation request
     * @param authentication the current authentication for authorization
     * @return the created role response
     */
    RoleResponse createRole(CreateRoleRequest createRoleRequest, Authentication authentication);

    /**
     * Updates an existing role.
     * 
     * @param roleId the role ID to update
     * @param updateRoleRequest the role update request
     * @param authentication the current authentication for authorization
     * @return the updated role response
     */
    RoleResponse updateRole(Long roleId, UpdateRoleRequest updateRoleRequest, Authentication authentication);

    /**
     * Partially updates a role with only provided fields.
     * 
     * @param roleId the role ID to update
     * @param partialUpdateData the partial update data
     * @param authentication the current authentication for authorization
     * @return the updated role response
     */
    RoleResponse partialUpdateRole(Long roleId, Map<String, Object> partialUpdateData, Authentication authentication);

    /**
     * Updates role status (enabled/disabled).
     * 
     * @param roleId the role ID
     * @param enabled the new enabled status
     * @param authentication the current authentication for authorization
     * @return true if updated successfully
     */
    boolean updateRoleStatus(Long roleId, boolean enabled, Authentication authentication);

    /**
     * Gets a role by ID.
     * 
     * @param roleId the role ID
     * @param authentication the current authentication for authorization
     * @return the role response if authorized to view
     */
    Optional<RoleResponse> getRoleById(Long roleId, Authentication authentication);

    /**
     * Gets a role by name.
     * 
     * @param roleName the role name
     * @param authentication the current authentication for authorization
     * @return the role response if authorized to view
     */
    Optional<RoleResponse> getRoleByName(String roleName, Authentication authentication);

    /**
     * Gets all roles with pagination.
     * 
     * @param authentication the current authentication for authorization
     * @param pageable pagination information
     * @return page of roles the current user is authorized to view
     */
    Page<RoleResponse> getAllRoles(Authentication authentication, Pageable pageable);

    /**
     * Searches roles by name or description.
     * 
     * @param searchTerm the search term
     * @param authentication the current authentication for authorization
     * @param pageable pagination information
     * @return page of matching roles
     */
    Page<RoleResponse> searchRoles(String searchTerm, Authentication authentication, Pageable pageable);

    /**
     * Gets system roles.
     * 
     * @param authentication the current authentication for authorization
     * @param pageable pagination information
     * @return page of system roles
     */
    Page<RoleResponse> getSystemRoles(Authentication authentication, Pageable pageable);

    /**
     * Gets non-system roles.
     * 
     * @param authentication the current authentication for authorization
     * @param pageable pagination information
     * @return page of non-system roles
     */
    Page<RoleResponse> getNonSystemRoles(Authentication authentication, Pageable pageable);

    /**
     * Soft deletes a role.
     * 
     * @param roleId the role ID to delete
     * @param authentication the current authentication for authorization
     * @return true if deleted successfully
     */
    boolean deleteRole(Long roleId, Authentication authentication);

    /**
     * Assigns permissions to a role.
     * 
     * @param roleId the role ID
     * @param permissionIds the permission IDs to assign
     * @param authentication the current authentication for authorization
     * @return true if permissions assigned successfully
     */
    boolean assignPermissions(Long roleId, List<Long> permissionIds, Authentication authentication);

    /**
     * Removes permissions from a role.
     * 
     * @param roleId the role ID
     * @param permissionIds the permission IDs to remove
     * @param authentication the current authentication for authorization
     * @return true if permissions removed successfully
     */
    boolean removePermissions(Long roleId, List<Long> permissionIds, Authentication authentication);

    /**
     * Checks if the current user can view the specified role.
     * 
     * @param roleId the role ID
     * @param authentication the current authentication
     * @return true if authorized to view
     */
    boolean canViewRole(Long roleId, Authentication authentication);

    /**
     * Checks if the current user can modify the specified role.
     * 
     * @param roleId the role ID
     * @param authentication the current authentication
     * @return true if authorized to modify
     */
    boolean canModifyRole(Long roleId, Authentication authentication);
}