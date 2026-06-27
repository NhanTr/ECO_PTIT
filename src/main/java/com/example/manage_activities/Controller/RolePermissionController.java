package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.RolePermissionToggleRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.RolePermissionResponse;
import com.example.manage_activities.entity.Permission;
import com.example.manage_activities.entity.RolePermission;
import com.example.manage_activities.service.RolePermissionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * QTHT #10 - Quản lý phân quyền động (QTHT_QĐ 6, QTHT_BM 5).
 * Cho phép admin bật/tắt permission theo vai trò mà không cần sửa code.
 */
@RestController
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class RolePermissionController {

    RolePermissionService service;

    @GetMapping
    public APIResponse<List<Permission>> listPermissions() {
        return APIResponse.<List<Permission>>builder()
                .result(service.listAllPermissions())
                .build();
    }

    @GetMapping("/role/{roleId}")
    public APIResponse<List<RolePermission>> listByRole(@PathVariable Integer roleId) {
        return APIResponse.<List<RolePermission>>builder()
                .result(service.listByRole(roleId))
                .build();
    }

    @PutMapping("/role/{roleId}")
    public APIResponse<List<RolePermission>> replaceByRole(
            @PathVariable Integer roleId,
            @RequestBody List<Map<String, Object>> items) {
        return APIResponse.<List<RolePermission>>builder()
                .message("Đã cập nhật phân quyền")
                .result(service.replaceRolePermissions(roleId, items))
                .build();
    }

    @PutMapping("/role/{roleId}/toggle")
    public APIResponse<RolePermission> toggle(
            @PathVariable Integer roleId,
            @Valid @RequestBody RolePermissionToggleRequest request) {
        return APIResponse.<RolePermission>builder()
                .message("Đã cập nhật quyền")
                .result(service.setPermission(roleId, request.getPermissionKey(), request.getEnabled()))
                .build();
    }

    @PostMapping("/role/{roleId}/reset")
    public APIResponse<Void> resetToDefault(@PathVariable Integer roleId) {
        service.resetToDefault(roleId);
        return APIResponse.<Void>builder()
                .message("Đã reset về phân quyền mặc định")
                .build();
    }
}