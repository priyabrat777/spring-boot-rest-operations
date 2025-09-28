package com.enterprise.api.security;

import com.enterprise.api.entity.User;
import com.enterprise.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation for loading user-specific data
 * during authentication. Integrates with the User entity and repository.
 * 
 * Requirements addressed:
 * - 4.1: User authentication with custom UserDetailsService
 * - 4.2: User data loading for JWT token validation
 * - 4.3: Role and permission loading for RBAC
 * - 4.7: User account status validation
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user by username for authentication.
     * Fetches user with roles and permissions for complete RBAC support.
     * 
     * @param username the username identifying the user whose data is required
     * @return a fully populated UserDetails object
     * @throws UsernameNotFoundException if the user could not be found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);
        
        User user = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> {
                    logger.warn("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        // Check if user is soft deleted
        if (user.isDeleted()) {
            logger.warn("Attempt to authenticate deleted user: {}", username);
            throw new UsernameNotFoundException("User account is no longer active: " + username);
        }

        logger.debug("User loaded successfully: {} with {} roles", 
                username, user.getRoles().size());
        
        return new CustomUserPrincipal(user);
    }

    /**
     * Loads user by user ID for JWT token validation.
     * Used when validating JWT tokens that contain user ID claims.
     * 
     * @param userId the user ID
     * @return a fully populated UserDetails object
     * @throws UsernameNotFoundException if the user could not be found
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        logger.debug("Loading user by ID: {}", userId);
        
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> {
                    logger.warn("User not found with ID: {}", userId);
                    return new UsernameNotFoundException("User not found with ID: " + userId);
                });

        // Check if user is soft deleted
        if (user.isDeleted()) {
            logger.warn("Attempt to authenticate deleted user with ID: {}", userId);
            throw new UsernameNotFoundException("User account is no longer active with ID: " + userId);
        }

        logger.debug("User loaded successfully by ID: {} (username: {}) with {} roles", 
                userId, user.getUsername(), user.getRoles().size());
        
        return new CustomUserPrincipal(user);
    }

    /**
     * Loads user by email for authentication.
     * Alternative authentication method using email instead of username.
     * 
     * @param email the email identifying the user whose data is required
     * @return a fully populated UserDetails object
     * @throws UsernameNotFoundException if the user could not be found
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        logger.debug("Loading user by email: {}", email);
        
        User user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> {
                    logger.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        // Check if user is soft deleted
        if (user.isDeleted()) {
            logger.warn("Attempt to authenticate deleted user with email: {}", email);
            throw new UsernameNotFoundException("User account is no longer active with email: " + email);
        }

        logger.debug("User loaded successfully by email: {} (username: {}) with {} roles", 
                email, user.getUsername(), user.getRoles().size());
        
        return new CustomUserPrincipal(user);
    }

    /**
     * Checks if a user exists by username.
     * 
     * @param username the username to check
     * @return true if the user exists and is not deleted, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsernameAndDeletedFalse(username);
    }

    /**
     * Checks if a user exists by email.
     * 
     * @param email the email to check
     * @return true if the user exists and is not deleted, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailAndDeletedFalse(email);
    }

    /**
     * Validates if a user account is active and can be authenticated.
     * 
     * @param userDetails the user details to validate
     * @return true if the account is valid for authentication, false otherwise
     */
    public boolean isAccountValid(UserDetails userDetails) {
        return userDetails.isEnabled() 
                && userDetails.isAccountNonExpired() 
                && userDetails.isAccountNonLocked() 
                && userDetails.isCredentialsNonExpired();
    }
}