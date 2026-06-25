package com.example.manage_activities.security;

import com.example.manage_activities.entity.User;
import com.example.manage_activities.enums.Roles;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleAssignmentPolicy {

    private final UserRepository userRepository;

    public Roles getCallerRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .map(Roles::valueOf)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
    }

    public void assertCanManageUsers() {
        Roles caller = getCallerRole();
        if (caller == Roles.ADMIN || caller == Roles.MANAGER) {
            return;
        }
        throw new AppException(ErrorCode.ROLE_ASSIGNMENT_FORBIDDEN);
    }

    public void assertCanManageUser(String targetUserId) {
        assertCanManageUsers();
        if (getCallerRole() == Roles.ADMIN) {
            return;
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (Roles.fromId(target.getRoleId()) == Roles.ADMIN) {
            throw new AppException(ErrorCode.CANNOT_MODIFY_ADMIN_USER);
        }
    }

    public void assertCanAssignRole(Integer newRoleId) {
        assertCanManageUsers();
        if (getCallerRole() == Roles.ADMIN) {
            return;
        }
        if (Roles.fromId(newRoleId) == Roles.ADMIN) {
            throw new AppException(ErrorCode.ROLE_ASSIGNMENT_FORBIDDEN);
        }
    }
}
