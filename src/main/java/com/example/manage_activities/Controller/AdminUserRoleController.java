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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * QTHT #2 - Phân quyền người dùng (QTHT_QĐ 1, QTHT_BM 5).
 * Cho phép Admin gán/thu hồi vai trò chính của tài khoản thông qua REST API.
 * Lưu ý: role ADMIN không thể tự thay đổi vai trò của chính mình
 * để tránh lockout - chính sách này được RoleAssignmentPolicy xử lý.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserRoleController {

    UserService userService;

    @PostMapping("/{userId}/assign-role")
    public APIResponse<UserResponse> assignRole(
            @PathVariable String userId,
            @Valid @RequestBody AssignRoleRequest request) {
        return APIResponse.<UserResponse>builder()
                .message("Đã gán vai trò cho người dùng")
                .result(userService.assignPrimaryRole(userId, request.getRoleId()))
                .build();
    }

    @PostMapping("/{userId}/revoke-role")
    public APIResponse<UserResponse> revokeRole(@PathVariable String userId) {
        return APIResponse.<UserResponse>builder()
                .message("Đã thu hồi vai trò, đặt về STUDENT")
                .result(userService.revokePrimaryRole(userId))
                .build();
    }
}