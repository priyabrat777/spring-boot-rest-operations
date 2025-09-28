package com.enterprise.api.security;

import com.enterprise.api.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom UserPrincipal implementation that wraps the User entity
 * and provides Spring Security UserDetails interface implementation.
 * 
 * Requirements addressed:
 * - 4.1: User authentication with UserDetails implementation
 * - 4.3: Role-based access control with authorities mapping
 * - 4.7: User account status validation
 */
public class CustomUserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String email;
    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserPrincipal(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.email = user.getEmail();
        this.enabled = user.isEnabled();
        this.accountNonExpired = user.isAccountNonExpired();
        this.accountNonLocked = user.isAccountNonLocked();
        this.credentialsNonExpired = user.isCredentialsNonExpired();
        this.authorities = mapRolesToAuthorities(user);
    }

    /**
     * Maps user roles and permissions to Spring Security authorities.
     * Creates authorities for both roles (ROLE_*) and permissions (PERMISSION_*).
     * 
     * @param user the user entity
     * @return collection of granted authorities
     */
    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(User user) {
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> {
                    Set<GrantedAuthority> roleAuthorities = role.getPermissions().stream()
                            .map(permission -> new SimpleGrantedAuthority("PERMISSION_" + permission.getName()))
                            .collect(Collectors.toSet());
                    
                    // Add role authority
                    roleAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                    
                    return roleAuthorities.stream();
                })
                .collect(Collectors.toSet());
        
        return authorities;
    }

    // UserDetails interface methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Additional getters for JWT token generation
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Checks if the user has a specific role.
     * 
     * @param roleName the role name to check
     * @return true if the user has the role, false otherwise
     */
    public boolean hasRole(String roleName) {
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + roleName));
    }

    /**
     * Checks if the user has a specific permission.
     * 
     * @param permissionName the permission name to check
     * @return true if the user has the permission, false otherwise
     */
    public boolean hasPermission(String permissionName) {
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals("PERMISSION_" + permissionName));
    }

    /**
     * Gets all role names for this user.
     * 
     * @return set of role names
     */
    public Set<String> getRoleNames() {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5)) // Remove "ROLE_" prefix
                .collect(Collectors.toSet());
    }

    /**
     * Gets all permission names for this user.
     * 
     * @return set of permission names
     */
    public Set<String> getPermissionNames() {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("PERMISSION_"))
                .map(authority -> authority.substring(11)) // Remove "PERMISSION_" prefix
                .collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CustomUserPrincipal that = (CustomUserPrincipal) obj;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CustomUserPrincipal{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", enabled=" + enabled +
                ", accountNonExpired=" + accountNonExpired +
                ", accountNonLocked=" + accountNonLocked +
                ", credentialsNonExpired=" + credentialsNonExpired +
                ", authoritiesCount=" + (authorities != null ? authorities.size() : 0) +
                '}';
    }
}