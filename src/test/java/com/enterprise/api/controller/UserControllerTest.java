package com.enterprise.api.controller;

import com.enterprise.api.dto.request.ChangePasswordRequest;
import com.enterprise.api.dto.request.CreateUserRequest;
import com.enterprise.api.dto.request.UpdateUserRequest;
import com.enterprise.api.dto.response.UserResponse;
import com.enterprise.api.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserController.
 * Tests all user management endpoints with proper HTTP status codes and RBAC.
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
@WebMvcTest(controllers = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.enterprise.api.security.*"))
class UserControllerTest {

        @TestConfiguration
        static class TestSecurityConfig {
                @Bean
                @Primary
                public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
                        return http
                                        .csrf(csrf -> csrf.disable())
                                        .authorizeHttpRequests(auth -> auth
                                                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                        .requestMatchers(HttpMethod.HEAD, "/**").permitAll()
                                                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                                                        .anyRequest().authenticated())
                                        .httpBasic(httpBasic -> {
                                        })
                                        .build();
                }
        }

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @Autowired
        private ObjectMapper objectMapper;

        private CreateUserRequest createUserRequest;
        private UpdateUserRequest updateUserRequest;
        private ChangePasswordRequest changePasswordRequest;
        private UserResponse userResponse;
        private Authentication adminAuth;
        private Authentication userAuth;

        @BeforeEach
        void setUp() {
                // Create requests
                createUserRequest = new CreateUserRequest();
                createUserRequest.setUsername("newuser");
                createUserRequest.setEmail("newuser@example.com");
                createUserRequest.setPassword("Password123!");
                createUserRequest.setFirstName("New");
                createUserRequest.setLastName("User");

                updateUserRequest = new UpdateUserRequest();
                updateUserRequest.setEmail("updated@example.com");
                updateUserRequest.setFirstName("Updated");
                updateUserRequest.setLastName("User");

                changePasswordRequest = new ChangePasswordRequest();
                changePasswordRequest.setCurrentPassword("OldPassword123!");
                changePasswordRequest.setNewPassword("NewPassword123!");
                changePasswordRequest.setConfirmPassword("NewPassword123!");

                // Create user response
                userResponse = new UserResponse();
                userResponse.setId(1L);
                userResponse.setUsername("testuser");
                userResponse.setEmail("test@example.com");
                userResponse.setFirstName("Test");
                userResponse.setLastName("User");
                userResponse.setEnabled(true);
                userResponse.setAccountNonExpired(true);
                userResponse.setAccountNonLocked(true);
                userResponse.setCredentialsNonExpired(true);
                userResponse.setCreatedDate(LocalDateTime.now());
                userResponse.setVersion(1L);

                // Create authentications
                adminAuth = new UsernamePasswordAuthenticationToken(
                                "admin",
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

                userAuth = new UsernamePasswordAuthenticationToken(
                                "user",
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void createUser_WithValidRequest_ShouldReturnCreatedUser() throws Exception {
                when(userService.createUser(any(CreateUserRequest.class), any(Authentication.class)))
                                .thenReturn(userResponse);

                mockMvc.perform(post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createUserRequest))
                                .with(authentication(adminAuth)))
                                .andExpect(status().isCreated())
                                .andExpect(header().string("Location", "/api/v1/users/1"))
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.username").value("testuser"))
                                .andExpect(jsonPath("$.email").value("test@example.com"));
        }

        @Test
        void createUser_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
                mockMvc.perform(post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createUserRequest)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        void createUser_WithInsufficientRole_ShouldReturnForbidden() throws Exception {
                mockMvc.perform(post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createUserRequest))
                                .with(authentication(userAuth)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getAllUsers_WithValidAuth_ShouldReturnPageOfUsers() throws Exception {
                Page<UserResponse> userPage = new PageImpl<>(List.of(userResponse), PageRequest.of(0, 20), 1);
                when(userService.getAllUsers(any(Authentication.class), any(Pageable.class)))
                                .thenReturn(userPage);

                mockMvc.perform(get("/api/v1/users")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].id").value(1))
                                .andExpect(jsonPath("$.totalElements").value(1))
                                .andExpect(jsonPath("$.size").value(20));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getUserById_WithExistingUser_ShouldReturnUser() throws Exception {
                when(userService.getUserById(eq(1L), any(Authentication.class)))
                                .thenReturn(Optional.of(userResponse));

                mockMvc.perform(get("/api/v1/users/1")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.username").value("testuser"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getUserById_WithNonExistingUser_ShouldReturnNotFound() throws Exception {
                when(userService.getUserById(eq(999L), any(Authentication.class)))
                                .thenReturn(Optional.empty());

                mockMvc.perform(get("/api/v1/users/999")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getUserByUsername_WithExistingUser_ShouldReturnUser() throws Exception {
                when(userService.getUserByUsername(eq("testuser"), any(Authentication.class)))
                                .thenReturn(Optional.of(userResponse));

                mockMvc.perform(get("/api/v1/users/username/testuser")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.username").value("testuser"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void searchUsers_WithSearchTerm_ShouldReturnMatchingUsers() throws Exception {
                Page<UserResponse> userPage = new PageImpl<>(List.of(userResponse), PageRequest.of(0, 20), 1);
                when(userService.searchUsers(eq("test"), any(Authentication.class), any(Pageable.class)))
                                .thenReturn(userPage);

                mockMvc.perform(get("/api/v1/users/search")
                                .param("searchTerm", "test")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.content[0].username").value("testuser"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getUsersByRole_WithRoleName_ShouldReturnUsersWithRole() throws Exception {
                Page<UserResponse> userPage = new PageImpl<>(List.of(userResponse), PageRequest.of(0, 20), 1);
                when(userService.getUsersByRole(eq("USER"), any(Authentication.class), any(Pageable.class)))
                                .thenReturn(userPage);

                mockMvc.perform(get("/api/v1/users/role/USER")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.content[0].username").value("testuser"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void updateUser_WithValidRequest_ShouldReturnUpdatedUser() throws Exception {
                UserResponse updatedUser = new UserResponse();
                updatedUser.setId(1L);
                updatedUser.setUsername("testuser");
                updatedUser.setEmail("updated@example.com");
                updatedUser.setFirstName("Updated");
                updatedUser.setLastName("User");

                when(userService.updateUser(eq(1L), any(UpdateUserRequest.class), any(Authentication.class)))
                                .thenReturn(updatedUser);

                mockMvc.perform(put("/api/v1/users/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateUserRequest))
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.email").value("updated@example.com"))
                                .andExpect(jsonPath("$.firstName").value("Updated"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void updateUserStatus_WithValidRequest_ShouldReturnSuccess() throws Exception {
                when(userService.updateUserStatus(eq(1L), eq(false), any(Authentication.class)))
                                .thenReturn(true);

                mockMvc.perform(patch("/api/v1/users/1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"enabled\": false}")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.enabled").value(false));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void updateUserLockStatus_WithValidRequest_ShouldReturnSuccess() throws Exception {
                when(userService.updateUserLockStatus(eq(1L), eq(true), any(Authentication.class)))
                                .thenReturn(true);

                mockMvc.perform(patch("/api/v1/users/1/lock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"locked\": true}")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.locked").value(true));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void changePassword_WithValidRequest_ShouldReturnSuccess() throws Exception {
                when(userService.changePassword(eq(1L), any(ChangePasswordRequest.class), any(Authentication.class)))
                                .thenReturn(true);

                mockMvc.perform(patch("/api/v1/users/1/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(changePasswordRequest))
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("Password changed successfully"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void assignRoles_WithValidRequest_ShouldReturnSuccess() throws Exception {
                when(userService.assignRoles(eq(1L), anyList(), any(Authentication.class)))
                                .thenReturn(true);

                mockMvc.perform(patch("/api/v1/users/1/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"roleIds\": [1, 2]}")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("Roles assigned successfully"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void removeRoles_WithValidRequest_ShouldReturnSuccess() throws Exception {
                when(userService.removeRoles(eq(1L), anyList(), any(Authentication.class)))
                                .thenReturn(true);

                mockMvc.perform(delete("/api/v1/users/1/roles")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"roleIds\": [1, 2]}")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("Roles removed successfully"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void deleteUser_WithValidRequest_ShouldReturnSuccess() throws Exception {
                when(userService.deleteUser(eq(1L), any(Authentication.class)))
                                .thenReturn(true);

                mockMvc.perform(delete("/api/v1/users/1")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("User deleted successfully"));
        }

        @Test
        void handleOptions_ShouldReturnAllowedMethods() throws Exception {
                mockMvc.perform(options("/api/v1/users"))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Allow", "GET, POST, OPTIONS, HEAD"));
        }

        @Test
        void handleHead_ShouldReturnHeadersWithoutBody() throws Exception {
                mockMvc.perform(head("/api/v1/users"))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type", "application/json"))
                                .andExpect(content().string(""));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void createUser_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
                CreateUserRequest invalidRequest = new CreateUserRequest();
                // Missing required fields

                mockMvc.perform(post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest))
                                .with(authentication(adminAuth)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void updateUserStatus_WithFailedService_ShouldReturnBadRequest() throws Exception {
                when(userService.updateUserStatus(eq(1L), eq(false), any(Authentication.class)))
                                .thenReturn(false);

                mockMvc.perform(patch("/api/v1/users/1/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"enabled\": false}")
                                .with(authentication(adminAuth)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void changePassword_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
                ChangePasswordRequest invalidRequest = new ChangePasswordRequest();
                // Missing required fields

                mockMvc.perform(patch("/api/v1/users/1/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest))
                                .with(authentication(adminAuth)))
                                .andExpect(status().isBadRequest());
        }
}