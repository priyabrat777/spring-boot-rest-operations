package com.enterprise.api.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating existing roles.
 * Contains updatable role information and validation rules.
 * 
 * Requirements addressed:
 * - 4.6: Role updates with validation
 * - 4.7: Role management with proper validation
 */
public class UpdateRoleRequest {

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private Boolean systemRole;

    // Constructors
    public UpdateRoleRequest() {}

    public UpdateRoleRequest(String description) {
        this.description = description;
    }

    // Getters and Setters
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getSystemRole() {
        return systemRole;
    }

    public void setSystemRole(Boolean systemRole) {
        this.systemRole = systemRole;
    }

    /**
     * Checks if any field has been set for update.
     * 
     * @return true if at least one field is set for update
     */
    public boolean hasUpdates() {
        return description != null || systemRole != null;
    }

    @Override
    public String toString() {
        return "UpdateRoleRequest{" +
                "description='" + description + '\'' +
                ", systemRole=" + systemRole +
                '}';
    }
}