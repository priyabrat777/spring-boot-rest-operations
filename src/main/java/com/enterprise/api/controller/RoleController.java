package com.enterprise.api.controller;

import com.enterprise.api.dto.request.CreateRoleRequest;
import com.enterprise.api.dto.request.UpdateRoleRequest;
import com.enterprise.api.dto.response.RoleResponse;
import com.enterprise.api.service.RoleService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for role management operations.
 * Handles CRUD operations for roles with RBAC-based access control.
 * 
 * Requirements addressed:
 * - 1.1: GET operations for role retrieval
 * - 1.2: POST operations for role creation
 * - 1.3: PUT operations for role updates
 * - 1.4: DELETE operations for role removal
 * - 1.5: PATCH operations for partial updates
 * - 1.6: Proper HTTP status codes and response handling
 * - 4.4: Role-based access control
 * - 4.5: Authentication and authorization error handling
 * - 4.6: Role-based data filtering
 */
@RestController
@RequestMapping("/api/v1/roles")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RoleController {

    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * Creates a new role.
     * 
     * @param createRoleRequest the role creation request
     * @param authentication the current authentication
     * @return the created role response
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_CREATE')")
    public ResponseEntity<RoleResponse> createRole(
            @Valid @RequestBody CreateRoleRequest createRoleRequest,
            Authentication authentication) {
        
        logger.info("Creating role with name: {} by user: {}", 
                createRoleRequest.getName(), authentication.getName());
        
        try {
            RoleResponse roleResponse = roleService.createRole(createRoleRequest, authentication);
            logger.info("Role created successfully with ID: {} by user: {}", 
                    roleResponse.getId(), authentication.getName());
            
            URI location = URI.create("/api/v1/roles/" + roleResponse.getId());
            return ResponseEntity.created(location).body(roleResponse);
        } catch (Exception e) {
            logger.error("Failed to create role with name: {} by user: {}", 
                    createRoleRequest.getName(), authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Gets all roles with pagination and role-based filtering.
     * 
     * @param authentication the current authentication
     * @param pageable pagination information
     * @return page of roles
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('ROLE_READ')")
    public ResponseEntity<Page<RoleResponse>> getAllRoles(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("Getting all roles for user: {} with pagination: {}", 
                authentication.getName(), pageable);
        
        try {
            Page<RoleResponse> roles = roleService.getAllRoles(authentication, pageable);
            logger.debug("Retrieved {} roles for user: {}", roles.getTotalElements(), authentication.getName());
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            logger.error("Failed to get all roles for user: {}", authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Gets a role by ID.
     * 
     * @param roleId the role ID
     * @param authentication the current authentication
     * @return the role response
     */
    @GetMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('ROLE_READ')")
    public ResponseEntity<RoleResponse> getRoleById(
            @PathVariable Long roleId,
            Authentication authentication) {
        
        logger.debug("Getting role with ID: {} for user: {}", roleId, authentication.getName());
        
        try {
            Optional<RoleResponse> roleResponse = roleService.getRoleById(roleId, authentication);
            if (roleResponse.isPresent()) {
                logger.debug("Role found with ID: {} for user: {}", roleId, authentication.getName());
                return ResponseEntity.ok(roleResponse.get());
            } else {
                logger.warn("Role not found with ID: {} for user: {}", roleId, authentication.getName());
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to get role with ID: {} for user: {}", roleId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Gets a role by name.
     * 
     * @param roleName the role name
     * @param authentication the current authentication
     * @return the role response
     */
    @GetMapping("/name/{roleName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('ROLE_READ')")
    public ResponseEntity<RoleResponse> getRoleByName(
            @PathVariable String roleName,
            Authentication authentication) {
        
        logger.debug("Getting role with name: {} for user: {}", roleName, authentication.getName());
        
        try {
            Optional<RoleResponse> roleResponse = roleService.getRoleByName(roleName, authentication);
            if (roleResponse.isPresent()) {
                logger.debug("Role found with name: {} for user: {}", roleName, authentication.getName());
                return ResponseEntity.ok(roleResponse.get());
            } else {
                logger.warn("Role not found with name: {} for user: {}", roleName, authentication.getName());
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to get role with name: {} for user: {}", roleName, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Searches roles by name or description.
     * 
     * @param searchTerm the search term
     * @param authentication the current authentication
     * @param pageable pagination information
     * @return page of matching roles
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('ROLE_READ')")
    public ResponseEntity<Page<RoleResponse>> searchRoles(
            @RequestParam String searchTerm,
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("Searching roles with term: {} for user: {}", searchTerm, authentication.getName());
        
        try {
            Page<RoleResponse> roles = roleService.searchRoles(searchTerm, authentication, pageable);
            logger.debug("Found {} roles matching term: {} for user: {}", 
                    roles.getTotalElements(), searchTerm, authentication.getName());
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            logger.error("Failed to search roles with term: {} for user: {}", 
                    searchTerm, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Gets system roles.
     * 
     * @param authentication the current authentication
     * @param pageable pagination information
     * @return page of system roles
     */
    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_READ')")
    public ResponseEntity<Page<RoleResponse>> getSystemRoles(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("Getting system roles for user: {}", authentication.getName());
        
        try {
            Page<RoleResponse> roles = roleService.getSystemRoles(authentication, pageable);
            logger.debug("Retrieved {} system roles for user: {}", roles.getTotalElements(), authentication.getName());
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            logger.error("Failed to get system roles for user: {}", authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Gets non-system roles.
     * 
     * @param authentication the current authentication
     * @param pageable pagination information
     * @return page of non-system roles
     */
    @GetMapping("/custom")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('ROLE_READ')")
    public ResponseEntity<Page<RoleResponse>> getNonSystemRoles(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("Getting custom roles for user: {}", authentication.getName());
        
        try {
            Page<RoleResponse> roles = roleService.getNonSystemRoles(authentication, pageable);
            logger.debug("Retrieved {} custom roles for user: {}", roles.getTotalElements(), authentication.getName());
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            logger.error("Failed to get custom roles for user: {}", authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Updates a role completely.
     * 
     * @param roleId the role ID to update
     * @param updateRoleRequest the role update request
     * @param authentication the current authentication
     * @return the updated role response
     */
    @PutMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateRoleRequest updateRoleRequest,
            Authentication authentication) {
        
        logger.info("Updating role with ID: {} by user: {}", roleId, authentication.getName());
        
        try {
            RoleResponse roleResponse = roleService.updateRole(roleId, updateRoleRequest, authentication);
            logger.info("Role updated successfully with ID: {} by user: {}", roleId, authentication.getName());
            return ResponseEntity.ok(roleResponse);
        } catch (Exception e) {
            logger.error("Failed to update role with ID: {} by user: {}", roleId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Assigns permissions to a role.
     * 
     * @param roleId the role ID
     * @param permissionRequest the permission assignment request
     * @param authentication the current authentication
     * @return success message
     */
    @PatchMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<Map<String, Object>> assignPermissions(
            @PathVariable Long roleId,
            @RequestBody Map<String, List<Long>> permissionRequest,
            Authentication authentication) {
        
        List<Long> permissionIds = permissionRequest.get("permissionIds");
        logger.info("Assigning permissions {} to role ID: {} by user: {}", 
                permissionIds, roleId, authentication.getName());
        
        try {
            boolean success = roleService.assignPermissions(roleId, permissionIds, authentication);
            if (success) {
                logger.info("Permissions assigned successfully to role ID: {} by user: {}", 
                        roleId, authentication.getName());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Permissions assigned successfully",
                    "roleId", roleId,
                    "permissionIds", permissionIds
                ));
            } else {
                logger.warn("Failed to assign permissions to role ID: {} by user: {}", 
                        roleId, authentication.getName());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to assign permissions"
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to assign permissions to role ID: {} by user: {}", 
                    roleId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Removes permissions from a role.
     * 
     * @param roleId the role ID
     * @param permissionRequest the permission removal request
     * @param authentication the current authentication
     * @return success message
     */
    @DeleteMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<Map<String, Object>> removePermissions(
            @PathVariable Long roleId,
            @RequestBody Map<String, List<Long>> permissionRequest,
            Authentication authentication) {
        
        List<Long> permissionIds = permissionRequest.get("permissionIds");
        logger.info("Removing permissions {} from role ID: {} by user: {}", 
                permissionIds, roleId, authentication.getName());
        
        try {
            boolean success = roleService.removePermissions(roleId, permissionIds, authentication);
            if (success) {
                logger.info("Permissions removed successfully from role ID: {} by user: {}", 
                        roleId, authentication.getName());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Permissions removed successfully",
                    "roleId", roleId,
                    "permissionIds", permissionIds
                ));
            } else {
                logger.warn("Failed to remove permissions from role ID: {} by user: {}", 
                        roleId, authentication.getName());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to remove permissions"
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to remove permissions from role ID: {} by user: {}", 
                    roleId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Soft deletes a role.
     * 
     * @param roleId the role ID to delete
     * @param authentication the current authentication
     * @return success message
     */
    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_DELETE')")
    public ResponseEntity<Map<String, Object>> deleteRole(
            @PathVariable Long roleId,
            Authentication authentication) {
        
        logger.info("Deleting role with ID: {} by user: {}", roleId, authentication.getName());
        
        try {
            boolean success = roleService.deleteRole(roleId, authentication);
            if (success) {
                logger.info("Role deleted successfully with ID: {} by user: {}", 
                        roleId, authentication.getName());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Role deleted successfully",
                    "roleId", roleId
                ));
            } else {
                logger.warn("Failed to delete role with ID: {} by user: {}", 
                        roleId, authentication.getName());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to delete role"
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to delete role with ID: {} by user: {}", 
                    roleId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Handles OPTIONS requests for CORS preflight.
     * 
     * @return allowed methods
     */
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok()
                .header("Allow", "GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD")
                .build();
    }

    /**
     * Handles HEAD requests for endpoint availability.
     * 
     * @return response headers without body
     */
    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<Void> handleHead() {
        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .build();
    }
}