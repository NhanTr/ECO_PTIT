package com.example.manage_activities.controller;

import com.example.manage_activities.Controller.AdminUserController;
import com.example.manage_activities.dto.request.AssignRoleRequest;
import com.example.manage_activities.dto.request.UserCreateRequest;
import com.example.manage_activities.dto.request.UserIdentityRequest;
import com.example.manage_activities.dto.response.UserResponse;
import com.example.manage_activities.enums.Roles;
import com.example.manage_activities.enums.UserAccountStatus;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.exception.GlobalExceptionHandler;
import com.example.manage_activities.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class AdminUserControllerSecurityTest {

    private static final String BASE_URL = "/api/admin/users";
    private static final String ADMIN_USER_ID = "ADMIN00001";
    private static final String STUDENT_USER_ID = "STUDENT001";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UserService userService;

    @Nested
    @DisplayName("ADMIN role")
    class AdminRoleTests {

        @Test
        @WithMockUser(username = "admin-caller", roles = "ADMIN")
        void getUsers_returnsOkWithList() throws Exception {
            List<UserResponse> users = List.of(sampleStudentResponse(STUDENT_USER_ID));
            when(userService.searchUsers(null, null, null)).thenReturn(users);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Success"))
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result[0].id").value(STUDENT_USER_ID))
                    .andExpect(jsonPath("$.result[0].roleName").value("STUDENT"));

            verify(userService).searchUsers(null, null, null);
        }

        @Test
        @WithMockUser(username = "admin-caller", roles = "ADMIN")
        void createStudent_returnsOkWithCreatedUser() throws Exception {
            UserCreateRequest request = validStudentCreateRequest();
            UserResponse created = sampleStudentResponse("NEWSTUD01");
            when(userService.createUser(any(UserCreateRequest.class))).thenReturn(created);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Success"))
                    .andExpect(jsonPath("$.result.id").value("NEWSTUD01"))
                    .andExpect(jsonPath("$.result.roleName").value("STUDENT"));

            verify(userService).createUser(any(UserCreateRequest.class));
        }

        @Test
        @WithMockUser(username = "admin-caller", roles = "ADMIN")
        void deactivateUser_returnsNoContent() throws Exception {
            doNothing().when(userService).deactivateUser(STUDENT_USER_ID);

            mockMvc.perform(delete(BASE_URL + "/" + STUDENT_USER_ID))
                    .andExpect(status().isNoContent());

            verify(userService).deactivateUser(STUDENT_USER_ID);
        }
    }

    @Nested
    @DisplayName("MANAGER role — policy boundaries")
    class ManagerRoleTests {

        @Test
        @WithMockUser(username = "manager-caller", roles = "MANAGER")
        void createStudent_returnsOk() throws Exception {
            UserCreateRequest request = validStudentCreateRequest();
            when(userService.createUser(any(UserCreateRequest.class)))
                    .thenReturn(sampleStudentResponse("MGRSTU001"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.result.roleName").value("STUDENT"));
        }

        @Test
        @WithMockUser(username = "manager-caller", roles = "MANAGER")
        void deactivateStudent_returnsNoContent() throws Exception {
            doNothing().when(userService).deactivateUser(STUDENT_USER_ID);

            mockMvc.perform(delete(BASE_URL + "/" + STUDENT_USER_ID))
                    .andExpect(status().isNoContent());

            verify(userService).deactivateUser(STUDENT_USER_ID);
        }

        @Test
        @WithMockUser(username = "manager-caller", roles = "MANAGER")
        void deactivateAdmin_returnsForbiddenWithPolicyErrorCode() throws Exception {
            doThrow(new AppException(ErrorCode.CANNOT_MODIFY_ADMIN_USER))
                    .when(userService).deactivateUser(ADMIN_USER_ID);

            mockMvc.perform(delete(BASE_URL + "/" + ADMIN_USER_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.CANNOT_MODIFY_ADMIN_USER.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.CANNOT_MODIFY_ADMIN_USER.getMessage()));

            verify(userService).deactivateUser(ADMIN_USER_ID);
        }

        @Test
        @WithMockUser(username = "manager-caller", roles = "MANAGER")
        void assignAdminRole_returnsForbiddenWithPolicyErrorCode() throws Exception {
            AssignRoleRequest request = AssignRoleRequest.builder()
                    .roleId(Roles.ADMIN.getId())
                    .build();

            doThrow(new AppException(ErrorCode.ROLE_ASSIGNMENT_FORBIDDEN))
                    .when(userService).assignPrimaryRole(eq(STUDENT_USER_ID), eq(Roles.ADMIN.getId()));

            mockMvc.perform(put(BASE_URL + "/" + STUDENT_USER_ID + "/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ROLE_ASSIGNMENT_FORBIDDEN.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.ROLE_ASSIGNMENT_FORBIDDEN.getMessage()));

            verify(userService).assignPrimaryRole(STUDENT_USER_ID, Roles.ADMIN.getId());
        }
    }

    @Nested
    @DisplayName("STUDENT / ORGANIZER — denied at controller security")
    class UnauthorizedRoleTests {

        @Test
        @WithMockUser(username = "student-caller", roles = "STUDENT")
        void studentGetUsers_returnsForbidden() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()));
        }

        @Test
        @WithMockUser(username = "student-caller", roles = "STUDENT")
        void studentCreateUser_returnsForbidden() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validStudentCreateRequest())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()));
        }

        @Test
        @WithMockUser(username = "organizer-caller", roles = "ORGANIZER")
        void organizerGetUsers_returnsForbidden() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
        }

        @Test
        @WithMockUser(username = "organizer-caller", roles = "ORGANIZER")
        void organizerCreateUser_returnsForbidden() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validStudentCreateRequest())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
        }
    }

    private static UserCreateRequest validStudentCreateRequest() {
        return UserCreateRequest.builder()
                .username("sv_security_test")
                .password("password1")
                .email("sv_security@test.edu.vn")
                .roleId(Roles.STUDENT.getId())
                .status(UserAccountStatus.ACTIVE.getValue())
                .identity(UserIdentityRequest.builder()
                        .studentCode("B21DCAT999")
                        .className("D21CQAT01")
                        .department("CNTT")
                        .fullName("Security Test Student")
                        .build())
                .build();
    }

    private static UserResponse sampleStudentResponse(String id) {
        return UserResponse.builder()
                .id(id)
                .username("sv_sample")
                .email("sv_sample@test.edu.vn")
                .roleId(Roles.STUDENT.getId())
                .roleName(Roles.STUDENT.name())
                .roleDisplayName(Roles.STUDENT.getDisplayNameVi())
                .status(UserAccountStatus.ACTIVE.getValue())
                .createdAt(LocalDateTime.of(2026, 5, 18, 10, 0))
                .build();
    }
}
