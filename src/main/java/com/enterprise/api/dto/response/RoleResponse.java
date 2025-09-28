package com.enterprise.api.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO for role information.
 * Contains role data filtered based on RBAC permissions.
 * 
 * Requirements addressed:
 * - 4.6: Role-based data filtering in role responses
 */
public class RoleResponse {

    private Long id;
    private String name;
    private String description;
    private boolean systemRole;
    private Set<PermissionInfo> permissions;
    private int userCount;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private String createdBy;
    private String lastModifiedBy;
    private Long version;

    // Constructors
    public RoleResponse() {}

    public RoleResponse(Long id, String name, String description, boolean systemRole) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.systemRole = systemRole;
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

    public Set<PermissionInfo> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<PermissionInfo> permissions) {
        this.permissions = permissions;
    }

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Nested class for permission information in role response.
     */
    public static class PermissionInfo {
        private Long id;
        private String name;
        private String description;
        private String resource;
        private String action;
        private boolean systemPermission;

        // Constructors
        public PermissionInfo() {}

        public PermissionInfo(Long id, String name, String description, String resource, String action, boolean systemPermission) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.resource = resource;
            this.action = action;
            this.systemPermission = systemPermission;
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

        @Override
        public String toString() {
            return "PermissionInfo{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", resource='" + resource + '\'' +
                    ", action='" + action + '\'' +
                    ", systemPermission=" + systemPermission +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "RoleResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", systemRole=" + systemRole +
                ", userCount=" + userCount +
                ", permissionsCount=" + (permissions != null ? permissions.size() : 0) +
                ", version=" + version +
                '}';
    }
}