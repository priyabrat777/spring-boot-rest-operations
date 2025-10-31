package com.enterprise.api.dto.request;

import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Request DTO for creating new roles.
 * Contains role information and validation rules.
 * 
 * Requirements addressed:
 * - 4.6: Role creation with validation
 * - 4.7: Role management with proper validation
 */
public class CreateRoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(min = 2, max = 50, message = "Role name must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Z_]+$", message = "Role name must contain only uppercase letters and underscores")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private boolean systemRole = false;

    private List<Long> permissionIds;

    // Constructors
    public CreateRoleRequest() {}

    public CreateRoleRequest(String name) {
        this.name = name;
    }

    public CreateRoleRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
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

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isSystemRole() {
        return systemRole;
    }

    public void setSystemRole(boolean systemRole) {
        this.systemRole = systemRole;
    }

    public List<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Long> permissionIds) {
        this.permissionIds = permissionIds;
    }

    @Override
    public String toString() {
        return "CreateRoleRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", systemRole=" + systemRole +
                ", permissionIds=" + permissionIds +
                '}';
    }
}