package com.enterprise.api.service;

import com.enterprise.api.dto.request.LoginRequest;
import com.enterprise.api.dto.request.RefreshTokenRequest;
import com.enterprise.api.dto.response.AuthResponse;
import com.enterprise.api.dto.response.TokenResponse;
import com.enterprise.api.entity.Permission;
import com.enterprise.api.entity.Role;
import com.enterprise.api.entity.User;
import com.enterprise.api.repository.UserRepository;
import com.enterprise.api.security.CustomUserPrincipal;
import com.enterprise.api.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of AuthService for authentication operations.
 * Handles login, logout, token refresh, and authentication validation.
 * 
 * Requirements addressed:
 * - 4.1: JWT-based authentication with login and logout
 * - 4.2: Token validation and refresh functionality
 * - 4.6: Role-based data filtering during authentication
 * - 4.7: User account status validation
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        logger.debug("Attempting login for user: {}", loginRequest.getUsernameOrEmail());

        try {
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsernameOrEmail(),
                            loginRequest.getPassword()));

            // Get user details
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();

            // Load full user details for response building
            User user = userRepository.findByIdWithRolesAndPermissions(userPrincipal.getId())
                    .orElseThrow(
                            () -> new UsernameNotFoundException("User not found with ID: " + userPrincipal.getId()));

            // Validate account status
            validateAccountStatus(user);

            // Generate tokens
            String accessToken = jwtTokenProvider.generateToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
            long expiresIn = jwtTokenProvider.getTokenRemainingTime(accessToken);

            // Build user info for response
            AuthResponse.UserInfo userInfo = buildUserInfo(user);
            userInfo.setLastLogin(LocalDateTime.now());

            // Update user's last login time (optional - could be tracked separately)
            // This is a simple approach; in production, you might want to track this in a
            // separate table

            logger.info("User {} logged in successfully", user.getUsername());

            return new AuthResponse(accessToken, refreshToken, expiresIn, userInfo);

        } catch (BadCredentialsException e) {
            logger.warn("Invalid credentials for user: {}", loginRequest.getUsernameOrEmail());
            throw e;
        } catch (DisabledException e) {
            logger.warn("Account disabled for user: {}", loginRequest.getUsernameOrEmail());
            throw e;
        } catch (LockedException e) {
            logger.warn("Account locked for user: {}", loginRequest.getUsernameOrEmail());
            throw e;
        } catch (Exception e) {
            logger.error("Authentication failed for user: {}", loginRequest.getUsernameOrEmail(), e);
            throw new BadCredentialsException("Authentication failed");
        }
    }

    @Override
    public String logout(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            logger.info("User {} logged out successfully", username);

            // In a more sophisticated implementation, you might:
            // 1. Blacklist the JWT token
            // 2. Clear any cached user sessions
            // 3. Log the logout event for audit purposes

            return "Logout successful";
        }

        return "No active session found";
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();

        logger.debug("Attempting to refresh token");

        try {
            // Validate the refresh token
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                throw new BadCredentialsException("Invalid refresh token");
            }

            // Check if it's actually a refresh token
            if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
                throw new BadCredentialsException("Token is not a refresh token");
            }

            // Extract user information from refresh token
            String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
            Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

            // Load user to ensure they still exist and are active
            User user = userRepository.findByIdWithRolesAndPermissions(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

            // Validate account status
            validateAccountStatus(user);

            // Create new authentication object
            CustomUserPrincipal userPrincipal = new CustomUserPrincipal(user);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userPrincipal, null, userPrincipal.getAuthorities());

            // Generate new access token
            String newAccessToken = jwtTokenProvider.generateToken(authentication);
            long expiresIn = jwtTokenProvider.getTokenRemainingTime(newAccessToken);

            logger.debug("Token refreshed successfully for user: {}", username);

            return new TokenResponse(newAccessToken, expiresIn);

        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            throw new BadCredentialsException("Token refresh failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();

            // Check if user still exists and is active
            return userRepository.findByIdActive(userPrincipal.getId()).isPresent() &&
                    userPrincipal.isEnabled() &&
                    userPrincipal.isAccountNonExpired() &&
                    userPrincipal.isAccountNonLocked() &&
                    userPrincipal.isCredentialsNonExpired();
        } catch (Exception e) {
            logger.error("Error validating authentication", e);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAccountActive(String username) {
        try {
            User user = userRepository.findByUsernameOrEmailActive(username)
                    .orElse(null);

            if (user == null) {
                return false;
            }

            return user.isEnabled() &&
                    user.isAccountNonExpired() &&
                    user.isAccountNonLocked() &&
                    user.isCredentialsNonExpired();
        } catch (Exception e) {
            logger.error("Error checking account status for user: {}", username, e);
            return false;
        }
    }

    @Override
    public Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            return userPrincipal.getId();
        }
        return null;
    }

    @Override
    public String getCurrentUsername(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    @Override
    public boolean hasRole(Authentication authentication, String roleName) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_" + roleName) || authority.equals(roleName));
    }

    @Override
    public boolean hasPermission(Authentication authentication, String resource, String action) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Check if user has the specific permission through authorities
        String permissionAuthority = "PERMISSION_" + resource.toUpperCase() + "_" + action.toUpperCase();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(permissionAuthority));
    }

    /**
     * Validates the account status and throws appropriate exceptions.
     * 
     * @param user the user to validate
     * @throws AuthenticationException if account is not valid
     */
    private void validateAccountStatus(User user) throws AuthenticationException {
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        if (!user.isAccountNonExpired()) {
            throw new BadCredentialsException("Account has expired");
        }

        if (!user.isAccountNonLocked()) {
            throw new LockedException("Account is locked");
        }

        if (!user.isCredentialsNonExpired()) {
            throw new BadCredentialsException("Credentials have expired");
        }
    }

    @Override
    public boolean changeCurrentUserPassword(String currentPassword, String newPassword,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Attempt to change password without authentication");
            return false;
        }

        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            User user = userRepository.findByIdActive(userPrincipal.getId())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                logger.warn("Invalid current password for user: {}", user.getUsername());
                return false;
            }

            // Validate new password (you might want to add password strength validation)
            if (newPassword == null || newPassword.trim().length() < 8) {
                logger.warn("New password does not meet requirements for user: {}", user.getUsername());
                return false;
            }

            // Update password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            logger.info("Password changed successfully for user: {}", user.getUsername());
            return true;

        } catch (Exception e) {
            logger.error("Failed to change password for user", e);
            return false;
        }
    }

    @Override
    public boolean updateCurrentUserProfile(Map<String, Object> profileData, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Attempt to update profile without authentication");
            return false;
        }

        try {
            CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
            User user = userRepository.findByIdActive(userPrincipal.getId())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            boolean updated = false;

            // Update allowed profile fields
            if (profileData.containsKey("firstName")) {
                String firstName = (String) profileData.get("firstName");
                if (firstName != null && !firstName.equals(user.getFirstName())) {
                    user.setFirstName(firstName.trim());
                    updated = true;
                }
            }

            if (profileData.containsKey("lastName")) {
                String lastName = (String) profileData.get("lastName");
                if (lastName != null && !lastName.equals(user.getLastName())) {
                    user.setLastName(lastName.trim());
                    updated = true;
                }
            }

            if (profileData.containsKey("email")) {
                String email = (String) profileData.get("email");
                if (email != null && !email.equals(user.getEmail())) {
                    // Validate email format and uniqueness
                    if (isValidEmail(email) && !userRepository.existsByEmailAndIdNot(email, user.getId())) {
                        user.setEmail(email.trim().toLowerCase());
                        updated = true;
                    } else {
                        logger.warn("Invalid or duplicate email for user: {}", user.getUsername());
                        return false;
                    }
                }
            }

            if (updated) {
                userRepository.save(user);
                logger.info("Profile updated successfully for user: {}", user.getUsername());
                return true;
            }

            return true; // No changes needed
        } catch (Exception e) {
            logger.error("Failed to update profile for user", e);
            return false;
        }
    }

    /**
     * Validates email format.
     * 
     * @param email the email to validate
     * @return true if email is valid
     */
    private boolean isValidEmail(String email) {
        return email != null &&
                email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }

    /**
     * Builds user information for authentication response.
     * 
     * @param user the user entity
     * @return user info DTO
     */
    private AuthResponse.UserInfo buildUserInfo(User user) {
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName());

        userInfo.setEnabled(user.isEnabled());

        // Extract role names
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        userInfo.setRoles(roleNames);

        // Extract permission names
        Set<String> permissionNames = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
        userInfo.setPermissions(permissionNames);

        return userInfo;
    }
}