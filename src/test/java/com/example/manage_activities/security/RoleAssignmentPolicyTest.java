package com.example.manage_activities.security;

import com.example.manage_activities.entity.User;
import com.example.manage_activities.enums.Roles;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAssignmentPolicyTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    RoleAssignmentPolicy roleAssignmentPolicy;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void managerCannotModifyAdminUser() {
        authenticateAs(Roles.MANAGER);
        when(userRepository.findById("admin-1"))
                .thenReturn(Optional.of(User.builder().id("admin-1").roleId(Roles.ADMIN.getId()).build()));

        AppException ex = assertThrows(AppException.class,
                () -> roleAssignmentPolicy.assertCanManageUser("admin-1"));

        assertEquals(ErrorCode.CANNOT_MODIFY_ADMIN_USER, ex.getErrorCode());
    }

    @Test
    void managerCannotAssignAdminRole() {
        authenticateAs(Roles.MANAGER);

        AppException ex = assertThrows(AppException.class,
                () -> roleAssignmentPolicy.assertCanAssignRole(Roles.ADMIN.getId()));

        assertEquals(ErrorCode.ROLE_ASSIGNMENT_FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void adminCanAssignAnyRole() {
        authenticateAs(Roles.ADMIN);
        roleAssignmentPolicy.assertCanAssignRole(Roles.ADMIN.getId());
        roleAssignmentPolicy.assertCanAssignRole(Roles.STUDENT.getId());
    }

    private void authenticateAs(Roles role) {
        var authentication = new UsernamePasswordAuthenticationToken(
                "caller-id",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
