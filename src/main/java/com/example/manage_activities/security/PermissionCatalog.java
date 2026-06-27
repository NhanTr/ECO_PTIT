package com.example.manage_activities.security;

import com.example.manage_activities.enums.Roles;
import com.example.manage_activities.entity.Permission;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catalog permission cố định của hệ thống (QTHT_BM 5).
 * Mỗi permission có key ổn định để frontend bind, label Tiếng Việt để hiển thị,
 * groupLabel để gom nhóm.
 *
 * DEFAULT_PERMISSIONS_BY_ROLE định nghĩa permission mặc định cho mỗi role khi DB rỗng.
 * Sau khi admin tùy chỉnh, dữ liệu được lưu vào bảng role_permissions.
 */
public final class PermissionCatalog {

    private PermissionCatalog() {}

    @Getter
    @AllArgsConstructor
    public static class PermissionDef {
        private final String key;
        private final String label;
        private final String groupLabel;
    }

    public static final List<PermissionDef> ALL_PERMISSIONS = List.of(
            // Nhóm Người dùng
            new PermissionDef("users.view", "Xem người dùng", "Người dùng"),
            new PermissionDef("users.create", "Tạo người dùng", "Người dùng"),
            new PermissionDef("users.edit", "Sửa người dùng", "Người dùng"),
            new PermissionDef("users.lock", "Khóa tài khoản", "Người dùng"),
            new PermissionDef("users.assign_role", "Gán vai trò", "Người dùng"),

            // Nhóm Hoạt động
            new PermissionDef("activities.view", "Xem hoạt động", "Hoạt động"),
            new PermissionDef("activities.create", "Tạo hoạt động", "Hoạt động"),
            new PermissionDef("activities.edit", "Sửa hoạt động", "Hoạt động"),
            new PermissionDef("activities.delete", "Xóa hoạt động", "Hoạt động"),
            new PermissionDef("activities.approve", "Duyệt hoạt động", "Hoạt động"),
            new PermissionDef("activities.checkin", "Điểm danh", "Hoạt động"),

            // Nhóm Hệ thống (chỉ QTHT)
            new PermissionDef("system.statistics", "Xem thống kê toàn hệ thống", "Hệ thống"),
            new PermissionDef("system.logs", "Xem nhật ký", "Hệ thống"),
            new PermissionDef("system.backup", "Sao lưu / Phục hồi", "Hệ thống"),
            new PermissionDef("system.config", "Cấu hình hệ thống", "Hệ thống"),
            new PermissionDef("system.categories", "Quản lý danh mục", "Hệ thống"),
            new PermissionDef("system.periods", "Quản lý năm học / học kỳ", "Hệ thống"),
            new PermissionDef("system.notification_channels", "Quản lý kênh thông báo", "Hệ thống"),
            new PermissionDef("system.notification_templates", "Quản lý template thông báo", "Hệ thống"),
            new PermissionDef("system.permissions", "Quản lý phân quyền", "Hệ thống"),

            // Nhóm Thông báo
            new PermissionDef("notifications.send", "Gửi thông báo", "Thông báo")
    );

    /**
     * Map roleId -> Set permission key mặc định (khi DB rỗng).
     * Admin có tất cả; Manager có duyệt hoạt động + thống kê; Organizer có tạo hoạt động + điểm danh;
     * Student chỉ xem + đăng ký (sẽ thêm các key student sau nếu cần).
     */
    public static final Map<Integer, Set<String>> DEFAULT_PERMISSIONS_BY_ROLE = Map.of(
            Roles.ADMIN.getId(), Set.of(
                    "users.view", "users.create", "users.edit", "users.lock", "users.assign_role",
                    "activities.view",
                    "system.statistics", "system.logs", "system.backup", "system.config",
                    "system.categories", "system.periods",
                    "system.notification_channels", "system.notification_templates",
                    "system.permissions", "notifications.send"
            ),
            Roles.MANAGER.getId(), Set.of(
                    "activities.view", "activities.approve",
                    "system.statistics", "notifications.send"
            ),
            Roles.ORGANIZER.getId(), Set.of(
                    "activities.view", "activities.create", "activities.edit", "activities.delete",
                    "activities.checkin", "notifications.send"
            ),
            Roles.STUDENT.getId(), Set.of(
                    "activities.view"
            )
    );

    public static List<Permission> toEntities() {
        return ALL_PERMISSIONS.stream()
                .map(def -> Permission.builder()
                        .key(def.getKey())
                        .label(def.getLabel())
                        .groupLabel(def.getGroupLabel())
                        .build())
                .toList();
    }
}