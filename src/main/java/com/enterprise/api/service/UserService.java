package com.enterprise.api.service;

import com.enterprise.api.dto.request.CreateUserRequest;
import com.enterprise.api.dto.request.UpdateUserRequest;
import com.enterprise.api.dto.request.ChangePasswordRequest;
import com.enterprise.api.dto.response.UserResponse;
import com.enterprise.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for user management operations.
 * Handles CRUD operations with RBAC-based data filtering.
 * 
 * Requirements addressed:
 * - 4.6: Role-based data filtering for user operations
 * - 4.7: User account management and validation
 */
public interface UserService {

    /**
     * Creates a new user with encoded password.
     * 
     * @param createUserRequest the user creation request
     * @param authentication the current authentication for authorization
     * @return the created user response
     */
    UserResponse createUser(CreateUserRequest createUserRequest, Authentication authentication);

    /**
     * Updates an existing user.
     * 
     * @param userId the user ID to update
     * @param updateUserRequest the user update request
     * @param authentication the current authentication for authorization
     * @return the updated user response
     */
    UserResponse updateUser(Long userId, UpdateUserRequest updateUserRequest, Authentication authentication);

    /**
     * Partially updates a user with only provided fields.
     * 
     * @param userId the user ID to update
     * @param partialUpdateData the partial update data
     * @param authentication the current authentication for authorization
     * @return the updated user response
     */
    UserResponse partialUpdateUser(Long userId, Map<String, Object> partialUpdateData, Authentication authentication);

    /**
     * Gets a user by ID with RBAC filtering.
     * 
     * @param userId the user ID
     * @param authentication the current authentication for authorization
     * @return the user response if authorized to view
     */
    Optional<UserResponse> getUserById(Long userId, Authentication authentication);

    /**
     * Gets a user by username with RBAC filtering.
     * 
     * @param username the username
     * @param authentication the current authentication for authorization
     * @return the user response if authorized to view
     */
    Optional<UserResponse> getUserByUsername(String username, Authentication authentication);

    /**
     * Gets all users with RBAC filtering and pagination.
     * 
     * @param authentication the current authentication for authorization
     * @param pageable pagination information
     * @return page of users the current user is authorized to view
     */
    Page<UserResponse> getAllUsers(Authentication authentication, Pageable pageable);

    /**
     * Searches users by name or username with RBAC filtering.
     * 
     * @param searchTerm the search term
     * @param authentication the current authentication for authorization
     * @param pageable pagination information
     * @return page of matching users
     */
    Page<UserResponse> searchUsers(String searchTerm, Authentication authentication, Pageable pageable);

    /**
     * Gets users by role with RBAC filtering.
     * 
     * @param roleName the role name
     * @param authentication the current authentication for authorization
     * @param pageable pagination information
     * @return page of users with the specified role
     */
    Page<UserResponse> getUsersByRole(String roleName, Authentication authentication, Pageable pageable);

    /**
     * Soft deletes a user.
     * 
     * @param userId the user ID to delete
     * @param authentication the current authentication for authorization
     * @return true if deleted successfully
     */
    boolean deleteUser(Long userId, Authentication authentication);

    /**
     * Enables or disables a user account.
     * 
     * @param userId the user ID
     * @param enabled the new enabled status
     * @param authentication the current authentication for authorization
     * @return true if updated successfully
     */
    boolean updateUserStatus(Long userId, boolean enabled, Authentication authentication);

    /**
     * Locks or unlocks a user account.
     * 
     * @param userId the user ID
     * @param locked the new lock status (true = locked, false = unlocked)
     * @param authentication the current authentication for authorization
     * @return true if updated successfully
     */
    boolean updateUserLockStatus(Long userId, boolean locked, Authentication authentication);

    /**
     * Changes a user's password with validation.
     * 
     * @param userId the user ID
     * @param changePasswordRequest the password change request
     * @param authentication the current authentication for authorization
     * @return true if password changed successfully
     */
    boolean changePassword(Long userId, ChangePasswordRequest changePasswordRequest, Authentication authentication);

    /**
     * Assigns roles to a user.
     * 
     * @param userId the user ID
     * @param roleIds the role IDs to assign
     * @param authentication the current authentication for authorization
     * @return true if roles assigned successfully
     */
    boolean assignRoles(Long userId, List<Long> roleIds, Authentication authentication);

    /**
     * Removes roles from a user.
     * 
     * @param userId the user ID
     * @param roleIds the role IDs to remove
     * @param authentication the current authentication for authorization
     * @return true if roles removed successfully
     */
    boolean removeRoles(Long userId, List<Long> roleIds, Authentication authentication);

    /**
     * Checks if the current user can view the specified user.
     * 
     * @param targetUserId the target user ID
     * @param authentication the current authentication
     * @return true if authorized to view
     */
    boolean canViewUser(Long targetUserId, Authentication authentication);

    /**
     * Checks if the current user can modify the specified user.
     * 
     * @param targetUserId the target user ID
     * @param authentication the current authentication
     * @return true if authorized to modify
     */
    boolean canModifyUser(Long targetUserId, Authentication authentication);

    /**
     * Validates password strength and requirements.
     * 
     * @param password the password to validate
     * @return true if password meets requirements
     */
    boolean validatePassword(String password);

    /**
     * Encodes a password using the configured password encoder.
     * 
     * @param rawPassword the raw password
     * @return the encoded password
     */
    String encodePassword(String rawPassword);

    /**
     * Checks if a password matches the encoded password.
     * 
     * @param rawPassword the raw password
     * @param encodedPassword the encoded password
     * @return true if passwords match
     */
    boolean matchesPassword(String rawPassword, String encodedPassword);
}