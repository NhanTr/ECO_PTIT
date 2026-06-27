package com.example.manage_activities.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.Objects;

/**
 * Bảng phân quyền role <-> permission (QTHT_BM 5).
 * Cho phép QTHT bật/tắt permission theo vai trò mà không cần sửa code.
 */
@Entity
@Table(name = "role_permissions")
@IdClass(RolePermission.PK.class)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RolePermission {

    @Id
    @Column(name = "role_id")
    Integer roleId;

    @Id
    @Column(name = "permission_key", length = 64)
    String permissionKey;

    @Column(nullable = false)
    Boolean enabled;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PK implements Serializable {
        Integer roleId;
        String permissionKey;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK)) return false;
            PK pk = (PK) o;
            return Objects.equals(roleId, pk.roleId) && Objects.equals(permissionKey, pk.permissionKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roleId, permissionKey);
        }
    }
}