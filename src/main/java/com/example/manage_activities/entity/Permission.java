package com.example.manage_activities.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Permission cố định trong hệ thống (QTHT_BM 5).
 * Key là mã ổn định để bind với frontend; groupLabel phân nhóm để hiển thị.
 * Không thể tạo permission mới từ UI (theo yêu cầu: "Cho phép QTHT quản lý danh mục ... mà không cần can thiệp mã nguồn").
 */
@Entity
@Table(name = "permissions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Permission {
    @Id
    @Column(name = "permission_key", length = 64)
    String key;          // vd: "users.view", "categories.manage"

    @Column(nullable = false, length = 128)
    String label;        // Tên hiển thị: "Xem người dùng"

    @Column(name = "group_label", nullable = false, length = 64)
    String groupLabel;   // Nhóm: "Người dùng", "Hoạt động", "Hệ thống"
}