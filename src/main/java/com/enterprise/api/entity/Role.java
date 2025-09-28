package com.enterprise.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Where;

import java.util.HashSet;
import java.util.Set;

/**
 * Role entity representing user roles in the RBAC system.
 * Extends AuditableEntity to track creation, modification, and soft deletion.
 * 
 * Requirements addressed:
 * - 3.1: Audit trail for role operations
 * - 5.6: Entity relationships for RBAC
 */
@Entity
@Table(name = "roles", indexes = {
    @Index(name = "idx_role_name", columnList = "name"),
    @Index(name = "idx_role_deleted", columnList = "deleted")
})
@Where(clause = "deleted = false")
public class Role extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Role name is required")
    @Size(min = 2, max = 50, message = "Role name must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Z_]+$", message = "Role name must contain only uppercase letters and underscores")
    @Column(name = "name", unique = true, nullable = false, length = 50)
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Column(name = "description")
    private String description;

    @Column(name = "system_role", nullable = false)
    private boolean systemRole = false;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id"),
        indexes = {
            @Index(name = "idx_role_permissions_role_id", columnList = "role_id"),
            @Index(name = "idx_role_permissions_permission_id", columnList = "permission_id")
        }
    )
    private Set<Permission> permissions = new HashSet<>();

    // Constructors
    public Role() {
        super();
    }

    public Role(String name) {
        this();
        this.name = name;
    }

    public Role(String name, String description) {
        this(name);
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

    public boolean isSystemRole() {
        return systemRole;
    }

    public void setSystemRole(boolean systemRole) {
        this.systemRole = systemRole;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users != null ? users : new HashSet<>();
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions != null ? permissions : new HashSet<>();
    }

    // Helper methods for permission management
    public void addPermission(Permission permission) {
        if (permission != null) {
            this.permissions.add(permission);
            permission.getRoles().add(this);
        }
    }

    public void removePermission(Permission permission) {
        if (permission != null) {
            this.permissions.remove(permission);
            permission.getRoles().remove(this);
        }
    }

    public boolean hasPermission(String permissionName) {
        return permissions.stream()
                .anyMatch(permission -> permission.getName().equals(permissionName));
    }

    public boolean hasPermission(String resource, String action) {
        return permissions.stream()
                .anyMatch(permission -> 
                    permission.getResource().equals(resource) && 
                    permission.getAction().equals(action));
    }

    // Helper methods for user management
    public void addUser(User user) {
        if (user != null) {
            this.users.add(user);
            user.getRoles().add(this);
        }
    }

    public void removeUser(User user) {
        if (user != null) {
            this.users.remove(user);
            user.getRoles().remove(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Role role = (Role) obj;
        
        // Use ID for equality if both entities are persisted
        if (id != null && role.id != null) {
            return id.equals(role.id);
        }
        
        // Use name for equality if entities are not persisted
        return name != null && name.equals(role.name);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : (name != null ? name.hashCode() : 0);
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", systemRole=" + systemRole +
                ", usersCount=" + (users != null ? users.size() : 0) +
                ", permissionsCount=" + (permissions != null ? permissions.size() : 0) +
                "} " + super.toString();
    }
}