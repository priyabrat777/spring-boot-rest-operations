package com.enterprise.api.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Implementation of AuditorAware to provide the current auditor (user) 
 * for Spring Data JPA auditing functionality.
 * 
 * This component integrates with Spring Security to automatically capture
 * the currently authenticated user for audit fields.
 */
public class AuditAware implements AuditorAware<String> {

    private static final String SYSTEM_USER = "SYSTEM";
    private static final String ANONYMOUS_USER = "ANONYMOUS";

    /**
     * Returns the current auditor (user) based on the security context.
     * 
     * @return Optional containing the current user's username, or SYSTEM/ANONYMOUS if no user is authenticated
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            return Optional.of(SYSTEM_USER);
        }
        
        if (!authentication.isAuthenticated()) {
            return Optional.of(ANONYMOUS_USER);
        }
        
        String username = authentication.getName();
        
        // Handle anonymous users
        if ("anonymousUser".equals(username)) {
            return Optional.of(ANONYMOUS_USER);
        }
        
        // Return the authenticated username, or SYSTEM if null
        return Optional.of(username != null ? username : SYSTEM_USER);
    }
    
    /**
     * Gets the current auditor without Optional wrapper.
     * Useful for programmatic access to current user.
     * 
     * @return Current auditor username
     */
    public String getCurrentAuditorName() {
        return getCurrentAuditor().orElse(SYSTEM_USER);
    }
    
    /**
     * Checks if there is a currently authenticated user.
     * 
     * @return true if a user is authenticated, false otherwise
     */
    public boolean isUserAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               authentication.isAuthenticated() && 
               !"anonymousUser".equals(authentication.getName());
    }
}