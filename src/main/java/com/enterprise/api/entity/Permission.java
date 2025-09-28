package com.enterprise.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Permission entity representing granular permissions in the RBAC system.
 * Extends AuditableEntity to track creation, modification, and soft deletion.
 * 
 * Requirements addressed:
 * - 3.1: Audit trail for permission operations
 * - 5.6: Entity relationships for RBAC
 */
@Entity
@Table(name = "permissions", 
    indexes = {
        @Index(name = "idx_permission_name", columnList = "name"),
        @Index(name = "idx_permission_resource_action", columnList = "resource, action"),
        @Index(name = "idx_permission_deleted", columnList = "deleted")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_permission_resource_action", columnNames = {"resource", "action"})
    }
)
public class Permission extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Permission name is required")
    @Size(min = 2, max = 100, message = "Permission name must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Z_:]+$", message = "Permission name must contain only uppercase letters, underscores, and colons")
    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Column(name = "description")
    private String description;

    @NotBlank(message = "Resource is required")
    @Size(min = 2, max = 50, message = "Resource must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-z_]+$", message = "Resource must contain only lowercase letters and underscores")
    @Column(name = "resource", nullable = false, length = 50)
    private String resource;

    @NotBlank(message = "Action is required")
    @Size(min = 2, max = 50, message = "Action must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-z_]+$", message = "Action must contain only lowercase letters and underscores")
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "system_permission", nullable = false)
    private boolean systemPermission = false;

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private Set<Role> roles = new HashSet<>();

    // Constructors
    public Permission() {
        super();
    }

    public Permission(String name, String resource, String action) {
        this();
        this.name = name;
        this.resource = resource;
        this.action = action;
    }

    public Permission(String name, String description, String resource, String action) {
        this(name, resource, action);
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isSystemPermission() {
        return systemPermission;
    }

    public void setSystemPermission(boolean systemPermission) {
        this.systemPermission = systemPermission;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles != null ? roles : new HashSet<>();
    }

    // Helper methods
    public void addRole(Role role) {
        if (role != null) {
            this.roles.add(role);
            role.getPermissions().add(this);
        }
    }

    public void removeRole(Role role) {
        if (role != null) {
            this.roles.remove(role);
            role.getPermissions().remove(this);
        }
    }

    /**
     * Checks if this permission matches the given resource and action.
     * 
     * @param resource the resource to check
     * @param action the action to check
     * @return true if this permission matches the resource and action
     */
    public boolean matches(String resource, String action) {
        return this.resource.equals(resource) && this.action.equals(action);
    }

    /**
     * Gets the full permission string in the format "resource:action".
     * 
     * @return the full permission string
     */
    public String getFullPermission() {
        return resource + ":" + action;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Permission permission = (Permission) obj;
        
        // Use ID for equality if both entities are persisted
        if (id != null && permission.id != null) {
            return id.equals(permission.id);
        }
        
        // Use name for equality if entities are not persisted
        return name != null && name.equals(permission.name);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : (name != null ? name.hashCode() : 0);
    }

    @Override
    public String toString() {
        return "Permission{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", resource='" + resource + '\'' +
                ", action='" + action + '\'' +
                ", systemPermission=" + systemPermission +
                ", rolesCount=" + (roles != null ? roles.size() : 0) +
                "} " + super.toString();
    }
}