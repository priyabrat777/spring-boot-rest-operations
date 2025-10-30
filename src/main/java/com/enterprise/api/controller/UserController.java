package com.enterprise.api.controller;

import com.enterprise.api.dto.request.ChangePasswordRequest;
import com.enterprise.api.dto.request.CreateUserRequest;
import com.enterprise.api.dto.request.UpdateUserRequest;
import com.enterprise.api.dto.response.ErrorResponse;
import com.enterprise.api.dto.response.UserResponse;
import com.enterprise.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for user management operations.
 * Handles CRUD operations with role-based access control.
 * 
 * Requirements addressed:
 * - 1.1: GET operations for user retrieval
 * - 1.2: POST operations for user creation
 * - 1.3: PUT operations for user updates
 * - 1.4: DELETE operations for user removal
 * - 1.5: PATCH operations for partial updates
 * - 1.6: Proper HTTP status codes and response handling
 * - 4.4: Role-based access control
 * - 4.5: Authentication and authorization error handling
 * - 4.6: Role-based data filtering
 */
@Tag(name = "User Management", description = "User CRUD operations with role-based access control and data filtering")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Creates a new user.
     * 
     * @param createUserRequest the user creation request
     * @param authentication the current authentication
     * @return the created user response
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_CREATE')")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest createUserRequest,
            Authentication authentication) {
        
        logger.info("Creating user with username: {} by user: {}", 
                createUserRequest.getUsername(), authentication.getName());
        
        try {
            UserResponse userResponse = userService.createUser(createUserRequest, authentication);
            logger.info("User created successfully with ID: {} by user: {}", 
                    userResponse.getId(), authentication.getName());
            
            URI location = URI.create("/api/v1/users/" + userResponse.getId());
            return ResponseEntity.created(location).body(userResponse);
        } catch (Exception e) {
            logger.error("Failed to create user with username: {} by user: {}", 
                    createUserRequest.getUsername(), authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Gets all users with pagination and role-based filtering.
     * 
     * @param authentication the current authentication
     * @param pageable pagination information
     * @return page of users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('USER_READ')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("Getting all users for user: {} with pagination: {}", 
                authentication.getName(), pageable);
        
        try {
            Page<UserResponse> users = userService.getAllUsers(authentication, pageable);
            logger.debug("Retrieved {} users for user: {}", users.getTotalElements(), authentication.getName());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Failed to get all users for user: {}", authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Gets a user by ID.
     * 
     * @param userId the user ID
     * @param authentication the current authentication
     * @return the user response
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('USER_READ') or #userId == authentication.principal.id")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long userId,
            Authentication authentication) {
        
        logger.debug("Getting user with ID: {} for user: {}", userId, authentication.getName());
        
        try {
            Optional<UserResponse> userResponse = userService.getUserById(userId, authentication);
            if (userResponse.isPresent()) {
                logger.debug("User found with ID: {} for user: {}", userId, authentication.getName());
                return ResponseEntity.ok(userResponse.get());
            } else {
                logger.warn("User not found with ID: {} for user: {}", userId, authentication.getName());
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to get user with ID: {} for user: {}", userId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Gets a user by username.
     * 
     * @param username the username
     * @param authentication the current authentication
     * @return the user response
     */
    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('USER_READ')")
    public ResponseEntity<UserResponse> getUserByUsername(
            @PathVariable String username,
            Authentication authentication) {
        
        logger.debug("Getting user with username: {} for user: {}", username, authentication.getName());
        
        try {
            Optional<UserResponse> userResponse = userService.getUserByUsername(username, authentication);
            if (userResponse.isPresent()) {
                logger.debug("User found with username: {} for user: {}", username, authentication.getName());
                return ResponseEntity.ok(userResponse.get());
            } else {
                logger.warn("User not found with username: {} for user: {}", username, authentication.getName());
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to get user with username: {} for user: {}", username, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Searches users by name or username.
     * 
     * @param searchTerm the search term
     * @param authentication the current authentication
     * @param pageable pagination information
     * @return page of matching users
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('USER_READ')")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam String searchTerm,
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("Searching users with term: {} for user: {}", searchTerm, authentication.getName());
        
        try {
            Page<UserResponse> users = userService.searchUsers(searchTerm, authentication, pageable);
            logger.debug("Found {} users matching term: {} for user: {}", 
                    users.getTotalElements(), searchTerm, authentication.getName());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Failed to search users with term: {} for user: {}", 
                    searchTerm, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Gets users by role.
     * 
     * @param roleName the role name
     * @param authentication the current authentication
     * @param pageable pagination information
     * @return page of users with the specified role
     */
    @GetMapping("/role/{roleName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('USER_READ')")
    public ResponseEntity<Page<UserResponse>> getUsersByRole(
            @PathVariable String roleName,
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("Getting users with role: {} for user: {}", roleName, authentication.getName());
        
        try {
            Page<UserResponse> users = userService.getUsersByRole(roleName, authentication, pageable);
            logger.debug("Found {} users with role: {} for user: {}", 
                    users.getTotalElements(), roleName, authentication.getName());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Failed to get users with role: {} for user: {}", 
                    roleName, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Updates a user completely.
     * 
     * @param userId the user ID to update
     * @param updateUserRequest the user update request
     * @param authentication the current authentication
     * @return the updated user response
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('USER_UPDATE') or #userId == authentication.principal.id")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest updateUserRequest,
            Authentication authentication) {
        
        logger.info("Updating user with ID: {} by user: {}", userId, authentication.getName());
        
        try {
            UserResponse userResponse = userService.updateUser(userId, updateUserRequest, authentication);
            logger.info("User updated successfully with ID: {} by user: {}", userId, authentication.getName());
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            logger.error("Failed to update user with ID: {} by user: {}", userId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Updates user status (enabled/disabled).
     * 
     * @param userId the user ID
     * @param statusRequest the status update request
     * @param authentication the current authentication
     * @return success message
     */
    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('USER_UPDATE')")
    public ResponseEntity<Map<String, Object>> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> statusRequest,
            Authentication authentication) {
        
        boolean enabled = statusRequest.getOrDefault("enabled", true);
        logger.info("Updating user status for ID: {} to enabled: {} by user: {}", 
                userId, enabled, authentication.getName());
        
        try {
            boolean success = userService.updateUserStatus(userId, enabled, authentication);
            if (success) {
                logger.info("User status updated successfully for ID: {} by user: {}", 
                        userId, authentication.getName());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User status updated successfully",
                    "userId", userId,
                    "enabled", enabled
                ));
            } else {
                logger.warn("Failed to update user status for ID: {} by user: {}", 
                        userId, authentication.getName());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to update user status"
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to update user status for ID: {} by user: {}", 
                    userId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Updates user lock status.
     * 
     * @param userId the user ID
     * @param lockRequest the lock status request
     * @param authentication the current authentication
     * @return success message
     */
    @PatchMapping("/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('USER_UPDATE')")
    public ResponseEntity<Map<String, Object>> updateUserLockStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> lockRequest,
            Authentication authentication) {
        
        boolean locked = lockRequest.getOrDefault("locked", false);
        logger.info("Updating user lock status for ID: {} to locked: {} by user: {}", 
                userId, locked, authentication.getName());
        
        try {
            boolean success = userService.updateUserLockStatus(userId, locked, authentication);
            if (success) {
                logger.info("User lock status updated successfully for ID: {} by user: {}", 
                        userId, authentication.getName());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User lock status updated successfully",
                    "userId", userId,
                    "locked", locked
                ));
            } else {
                logger.warn("Failed to update user lock status for ID: {} by user: {}", 
                        userId, authentication.getName());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to update user lock status"
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to update user lock status for ID: {} by user: {}", 
                    userId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Changes a user's password.
     * 
     * @param userId the user ID
     * @param changePasswordRequest the password change request
     * @param authentication the current authentication
     * @return success message
     */
    @PatchMapping("/{userId}/password")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('USER_UPDATE') or #userId == authentication.principal.id")
    public ResponseEntity<Map<String, Object>> changePassword(
            @PathVariable Long userId,
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest,
            Authentication authentication) {
        
        logger.info("Changing password for user ID: {} by user: {}", userId, authentication.getName());
        
        try {
            boolean success = userService.changePassword(userId, changePasswordRequest, authentication);
            if (success) {
                logger.info("Password changed successfully for user ID: {} by user: {}", 
                        userId, authentication.getName());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password changed successfully"
                ));
            } else {
                logger.warn("Failed to change password for user ID: {} by user: {}", 
                        userId, authentication.getName());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to change password"
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to change password for user ID: {} by user: {}", 
                    userId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Assigns roles to a user.
     * 
     * @param userId the user ID
     * @param roleRequest the role assignment request
     * @param authentication the current authentication
     * @return success message
     */
    @PatchMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('USER_UPDATE')")
    public ResponseEntity<Map<String, Object>> assignRoles(
            @PathVariable Long userId,
            @RequestBody Map<String, List<Long>> roleRequest,
            Authentication authentication) {
        
        List<Long> roleIds = roleRequest.get("roleIds");
        logger.info("Assigning roles {} to user ID: {} by user: {}", 
                roleIds, userId, authentication.getName());
        
        try {
            boolean success = userService.assignRoles(userId, roleIds, authentication);
            if (success) {
                logger.info("Roles assigned successfully to user ID: {} by user: {}", 
                        userId, authentication.getName());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Roles assigned successfully",
                    "userId", userId,
                    "roleIds", roleIds
                ));
            } else {
                logger.warn("Failed to assign roles to user ID: {} by user: {}", 
                        userId, authentication.getName());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to assign roles"
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to assign roles to user ID: {} by user: {}", 
                    userId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Removes roles from a user.
     * 
     * @param userId the user ID
     * @param roleRequest the role removal request
     * @param authentication the current authentication
     * @return success message
     */
    @DeleteMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('USER_UPDATE')")
    public ResponseEntity<Map<String, Object>> removeRoles(
            @PathVariable Long userId,
            @RequestBody Map<String, List<Long>> roleRequest,
            Authentication authentication) {
        
        List<Long> roleIds = roleRequest.get("roleIds");
        logger.info("Removing roles {} from user ID: {} by user: {}", 
                roleIds, userId, authentication.getName());
        
        try {
            boolean success = userService.removeRoles(userId, roleIds, authentication);
            if (success) {
                logger.info("Roles removed successfully from user ID: {} by user: {}", 
                        userId, authentication.getName());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Roles removed successfully",
                    "userId", userId,
                    "roleIds", roleIds
                ));
            } else {
                logger.warn("Failed to remove roles from user ID: {} by user: {}", 
                        userId, authentication.getName());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to remove roles"
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to remove roles from user ID: {} by user: {}", 
                    userId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Soft deletes a user.
     * 
     * @param userId the user ID to delete
     * @param authentication the current authentication
     * @return success message
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('USER_DELETE')")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @PathVariable Long userId,
            Authentication authentication) {
        
        logger.info("Deleting user with ID: {} by user: {}", userId, authentication.getName());
        
        try {
            boolean success = userService.deleteUser(userId, authentication);
            if (success) {
                logger.info("User deleted successfully with ID: {} by user: {}", 
                        userId, authentication.getName());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User deleted successfully",
                    "userId", userId
                ));
            } else {
                logger.warn("Failed to delete user with ID: {} by user: {}", 
                        userId, authentication.getName());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to delete user"
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to delete user with ID: {} by user: {}", 
                    userId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Partially updates a user with only provided fields.
     * 
     * @param userId the user ID to update
     * @param partialUpdateRequest the partial update request
     * @param authentication the current authentication
     * @return the updated user response
     */
    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or hasAuthority('USER_UPDATE') or #userId == authentication.principal.id")
    public ResponseEntity<UserResponse> partialUpdateUser(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> partialUpdateRequest,
            Authentication authentication) {
        
        logger.info("Partially updating user with ID: {} by user: {}", userId, authentication.getName());
        
        try {
            UserResponse userResponse = userService.partialUpdateUser(userId, partialUpdateRequest, authentication);
            logger.info("User partially updated successfully with ID: {} by user: {}", userId, authentication.getName());
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            logger.error("Failed to partially update user with ID: {} by user: {}", userId, authentication.getName(), e);
            throw e;
        }
    }

    /**
     * Handles OPTIONS requests for CORS preflight and method discovery.
     * 
     * @param request the HTTP request
     * @return allowed methods and CORS headers
     */
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        // Determine allowed methods based on endpoint pattern
        String allowedMethods;
        if (requestURI.matches(".*/users/\\d+/status") || 
            requestURI.matches(".*/users/\\d+/lock") || 
            requestURI.matches(".*/users/\\d+/password") || 
            requestURI.matches(".*/users/\\d+/roles")) {
            allowedMethods = "PATCH, DELETE, OPTIONS, HEAD";
        } else if (requestURI.matches(".*/users/\\d+")) {
            allowedMethods = "GET, PUT, PATCH, DELETE, OPTIONS, HEAD";
        } else if (requestURI.endsWith("/users/search") || 
                   requestURI.matches(".*/users/role/.*") || 
                   requestURI.matches(".*/users/username/.*")) {
            allowedMethods = "GET, OPTIONS, HEAD";
        } else if (requestURI.endsWith("/users")) {
            allowedMethods = "GET, POST, OPTIONS, HEAD";
        } else {
            allowedMethods = "GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD";
        }
        
        return ResponseEntity.ok()
                .header("Allow", allowedMethods)
                .header("Access-Control-Allow-Methods", allowedMethods)
                .header("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With, Accept, Origin")
                .header("Access-Control-Max-Age", "3600")
                .build();
    }

    /**
     * Handles HEAD requests for endpoint availability and metadata.
     * 
     * @param request the HTTP request
     * @return response headers without body
     */
    @RequestMapping(method = RequestMethod.HEAD)
    public ResponseEntity<Void> handleHead(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0");
        
        // Add endpoint-specific headers
        if (requestURI.matches(".*/users/\\d+")) {
            response.header("X-Resource-Type", "user")
                   .header("X-Supports-Partial-Update", "true");
        } else if (requestURI.endsWith("/users")) {
            response.header("X-Resource-Type", "user-collection")
                   .header("X-Supports-Pagination", "true");
        } else if (requestURI.endsWith("/search")) {
            response.header("X-Resource-Type", "user-search")
                   .header("X-Supports-Pagination", "true");
        }
        
        return response.build();
    }
}