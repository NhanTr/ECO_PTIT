package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.AssignRoleRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.UserResponse;
import com.example.manage_activities.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminRoleController {

    UserService userService;

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public APIResponse<UserResponse> assignRole(
            @PathVariable String userId,
            @Valid @RequestBody AssignRoleRequest request) {
        return APIResponse.<UserResponse>builder()
                .message("Da gan role chinh cho tai khoan")
                .result(userService.assignPrimaryRole(userId, request.getRoleId()))
                .build();
    }

    @DeleteMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public APIResponse<UserResponse> revokeRole(@PathVariable String userId) {
        return APIResponse.<UserResponse>builder()
                .message("Da thu hoi role va dua tai khoan ve STUDENT")
                .result(userService.revokePrimaryRole(userId))
                .build();
    }
}
