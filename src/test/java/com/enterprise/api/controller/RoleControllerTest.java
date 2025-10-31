package com.enterprise.api.controller;

import com.enterprise.api.dto.request.CreateRoleRequest;
import com.enterprise.api.dto.request.UpdateRoleRequest;
import com.enterprise.api.dto.response.RoleResponse;
import com.enterprise.api.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.enterprise.api.config.ControllerTestConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for RoleController.
 * Tests all role management endpoints with proper HTTP status codes and RBAC.
 * 
 * Requirements addressed:
 * - 1.1: GET operations testing
 * - 1.2: POST operations testing
 * - 1.3: PUT operations testing
 * - 1.4: DELETE operations testing
 * - 1.5: PATCH operations testing
 * - 1.6: HTTP status codes and response handling
 * - 4.4: Role-based access control testing
 * - 4.6: Role-based data filtering testing
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import({ ControllerTestConfig.class, com.enterprise.api.config.TestConfig.class })
class RoleControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private RoleService roleService;

        @Autowired
        private ObjectMapper objectMapper;

        private CreateRoleRequest createRoleRequest;
        private UpdateRoleRequest updateRoleRequest;
        private RoleResponse roleResponse;
        private Authentication adminAuth;
        private Authentication userAuth;
        private Authentication userManagerAuth;
        private Authentication roleReadAuth;

        @BeforeEach
        void setUp() {
                // Create requests
                createRoleRequest = new CreateRoleRequest();
                createRoleRequest.setName("NEW_ROLE");
                createRoleRequest.setDescription("A new role for testing");
                createRoleRequest.setSystemRole(false);

                updateRoleRequest = new UpdateRoleRequest();
                updateRoleRequest.setDescription("Updated role description");

                // Create role response
                roleResponse = new RoleResponse();
                roleResponse.setId(1L);
                roleResponse.setName("TEST_ROLE");
                roleResponse.setDescription("Test role for unit testing");
                roleResponse.setSystemRole(false);
                roleResponse.setCreatedDate(LocalDateTime.now());
                roleResponse.setVersion(1L);

                // Create authentications
                adminAuth = new UsernamePasswordAuthenticationToken(
                                "admin",
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

                userAuth = new UsernamePasswordAuthenticationToken(
                                "user",
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));

                userManagerAuth = new UsernamePasswordAuthenticationToken(
                                "usermanager",
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

                roleReadAuth = new UsernamePasswordAuthenticationToken(
                                "roleread",
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

                // Mock missing service methods
                when(roleService.partialUpdateRole(anyLong(), any(), any(Authentication.class)))
                                .thenReturn(roleResponse);
                when(roleService.updateRoleStatus(anyLong(), anyBoolean(), any(Authentication.class)))
                                .thenReturn(true);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void createRole_WithValidRequest_ShouldReturnCreatedRole() throws Exception {
                when(roleService.createRole(any(CreateRoleRequest.class), any(Authentication.class)))
                                .thenReturn(roleResponse);

                mockMvc.perform(post("/api/v1/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createRoleRequest))
                                .with(authentication(adminAuth)))
                                .andExpect(status().isCreated())
                                .andExpect(header().string("Location", "/api/v1/roles/1"))
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.name").value("TEST_ROLE"))
                                .andExpect(jsonPath("$.description").value("Test role for unit testing"))
                                .andExpect(jsonPath("$.systemRole").value(false));
        }

        @Test
        void createRole_WithoutAuthentication_ShouldReturnForbidden() throws Exception {
                mockMvc.perform(post("/api/v1/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createRoleRequest)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "USER")
        void createRole_WithInsufficientRole_ShouldReturnForbidden() throws Exception {
                mockMvc.perform(post("/api/v1/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createRoleRequest))
                                .with(authentication(userAuth)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getAllRoles_WithValidAuth_ShouldReturnPageOfRoles() throws Exception {
                Page<RoleResponse> rolePage = new PageImpl<>(List.of(roleResponse), PageRequest.of(0, 20), 1);
                when(roleService.getAllRoles(any(Authentication.class), any(Pageable.class)))
                                .thenReturn(rolePage);

                mockMvc.perform(get("/api/v1/roles")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].id").value(1))
                                .andExpect(jsonPath("$.content[0].name").value("TEST_ROLE"))
                                .andExpect(jsonPath("$.totalElements").value(1))
                                .andExpect(jsonPath("$.size").value(20));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getRoleById_WithExistingRole_ShouldReturnRole() throws Exception {
                when(roleService.getRoleById(eq(1L), any(Authentication.class)))
                                .thenReturn(Optional.of(roleResponse));

                mockMvc.perform(get("/api/v1/roles/1")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.name").value("TEST_ROLE"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getRoleById_WithNonExistingRole_ShouldReturnNotFound() throws Exception {
                when(roleService.getRoleById(eq(999L), any(Authentication.class)))
                                .thenReturn(Optional.empty());

                mockMvc.perform(get("/api/v1/roles/999")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getRoleByName_WithExistingRole_ShouldReturnRole() throws Exception {
                when(roleService.getRoleByName(eq("TEST_ROLE"), any(Authentication.class)))
                                .thenReturn(Optional.of(roleResponse));

                mockMvc.perform(get("/api/v1/roles/name/TEST_ROLE")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.name").value("TEST_ROLE"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void searchRoles_WithSearchTerm_ShouldReturnMatchingRoles() throws Exception {
                Page<RoleResponse> rolePage = new PageImpl<>(List.of(roleResponse), PageRequest.of(0, 20), 1);
                when(roleService.searchRoles(eq("test"), any(Authentication.class), any(Pageable.class)))
                                .thenReturn(rolePage);

                mockMvc.perform(get("/api/v1/roles/search")
                                .param("searchTerm", "test")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.content[0].name").value("TEST_ROLE"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getSystemRoles_WithValidAuth_ShouldReturnSystemRoles() throws Exception {
                RoleResponse systemRole = new RoleResponse();
                systemRole.setId(2L);
                systemRole.setName("SYSTEM_ADMIN");
                systemRole.setSystemRole(true);

                Page<RoleResponse> rolePage = new PageImpl<>(List.of(systemRole), PageRequest.of(0, 20), 1);
                when(roleService.getSystemRoles(any(Authentication.class), any(Pageable.class)))
                                .thenReturn(rolePage);

                mockMvc.perform(get("/api/v1/roles/system")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.content[0].systemRole").value(true));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getNonSystemRoles_WithValidAuth_ShouldReturnCustomRoles() throws Exception {
                Page<RoleResponse> rolePage = new PageImpl<>(List.of(roleResponse), PageRequest.of(0, 20), 1);
                when(roleService.getNonSystemRoles(any(Authentication.class), any(Pageable.class)))
                                .thenReturn(rolePage);

                mockMvc.perform(get("/api/v1/roles/custom")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.content[0].systemRole").value(false));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void updateRole_WithValidRequest_ShouldReturnUpdatedRole() throws Exception {
                RoleResponse updatedRole = new RoleResponse();
                updatedRole.setId(1L);
                updatedRole.setName("TEST_ROLE");
                updatedRole.setDescription("Updated role description");
                updatedRole.setSystemRole(false);

                when(roleService.updateRole(eq(1L), any(UpdateRoleRequest.class), any(Authentication.class)))
                                .thenReturn(updatedRole);

                mockMvc.perform(put("/api/v1/roles/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRoleRequest))
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.description").value("Updated role description"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void assignPermissions_WithValidRequest_ShouldReturnSuccess() throws Exception {
                when(roleService.assignPermissions(eq(1L), anyList(), any(Authentication.class)))
                                .thenReturn(true);

                mockMvc.perform(patch("/api/v1/roles/1/permissions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"permissionIds\": [1, 2, 3]}")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("Permissions assigned successfully"))
                                .andExpect(jsonPath("$.roleId").value(1))
                                .andExpect(jsonPath("$.permissionIds").isArray());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void removePermissions_WithValidRequest_ShouldReturnSuccess() throws Exception {
                when(roleService.removePermissions(eq(1L), anyList(), any(Authentication.class)))
                                .thenReturn(true);

                mockMvc.perform(delete("/api/v1/roles/1/permissions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"permissionIds\": [1, 2]}")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("Permissions removed successfully"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void deleteRole_WithValidRequest_ShouldReturnSuccess() throws Exception {
                when(roleService.deleteRole(eq(1L), any(Authentication.class)))
                                .thenReturn(true);

                mockMvc.perform(delete("/api/v1/roles/1")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("Role deleted successfully"))
                                .andExpect(jsonPath("$.roleId").value(1));
        }

        @Test
        void handleOptions_ShouldReturnAllowedMethods() throws Exception {
                mockMvc.perform(options("/api/v1/roles")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Allow", "GET, POST, OPTIONS, HEAD"));
        }

        @Test
        void handleHead_ShouldReturnHeadersWithoutBody() throws Exception {
                mockMvc.perform(head("/api/v1/roles")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type", "application/json"))
                                .andExpect(content().string(""));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void createRole_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
                CreateRoleRequest invalidRequest = new CreateRoleRequest();
                // Missing required fields

                mockMvc.perform(post("/api/v1/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest))
                                .with(authentication(adminAuth)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void assignPermissions_WithFailedService_ShouldReturnBadRequest() throws Exception {
                when(roleService.assignPermissions(eq(1L), anyList(), any(Authentication.class)))
                                .thenReturn(false);

                mockMvc.perform(patch("/api/v1/roles/1/permissions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"permissionIds\": [1, 2]}")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("Failed to assign permissions"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void removePermissions_WithFailedService_ShouldReturnBadRequest() throws Exception {
                when(roleService.removePermissions(eq(1L), anyList(), any(Authentication.class)))
                                .thenReturn(false);

                mockMvc.perform(delete("/api/v1/roles/1/permissions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"permissionIds\": [1, 2]}")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void deleteRole_WithFailedService_ShouldReturnBadRequest() throws Exception {
                when(roleService.deleteRole(eq(1L), any(Authentication.class)))
                                .thenReturn(false);

                mockMvc.perform(delete("/api/v1/roles/1")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("Failed to delete role"));
        }

        @Test
        @WithMockUser(roles = "USER_MANAGER")
        void getAllRoles_WithUserManagerRole_ShouldReturnRoles() throws Exception {
                Page<RoleResponse> rolePage = new PageImpl<>(List.of(roleResponse), PageRequest.of(0, 20), 1);
                when(roleService.getAllRoles(any(Authentication.class), any(Pageable.class)))
                                .thenReturn(rolePage);

                mockMvc.perform(get("/api/v1/roles")
                                .with(authentication(userManagerAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.content[0].name").value("TEST_ROLE"));
        }

        @Test
        @WithMockUser(authorities = "ROLE_READ")
        void getRoleById_WithRoleReadAuthority_ShouldReturnRole() throws Exception {
                when(roleService.getRoleById(eq(1L), any(Authentication.class)))
                                .thenReturn(Optional.of(roleResponse));

                mockMvc.perform(get("/api/v1/roles/1")
                                .with(authentication(roleReadAuth)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("TEST_ROLE"));
        }
}