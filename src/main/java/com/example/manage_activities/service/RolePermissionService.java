package com.example.manage_activities.service;

import com.example.manage_activities.entity.Permission;
import com.example.manage_activities.entity.RolePermission;
import com.example.manage_activities.enums.Roles;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.PermissionRepository;
import com.example.manage_activities.repository.RolePermissionRepository;
import com.example.manage_activities.security.PermissionCatalog;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RolePermissionService {

    PermissionRepository permissionRepository;
    RolePermissionRepository rolePermissionRepository;

    /**
     * Khởi tạo: nếu bảng permission rỗng thì seed toàn bộ catalog + default role mapping.
     * Phải chạy SAU khi Hibernate tạo schema xong (ApplicationReadyEvent),
     * không thể dùng @PostConstruct vì EntityManager chưa sẵn sàng tại thời điểm đó.
     * Idempotent — chỉ chạy khi DB rỗng.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initDefaultsIfEmpty() {
        try {
            if (permissionRepository.count() > 0) {
                return;
            }
            permissionRepository.saveAll(PermissionCatalog.toEntities());
            for (Map.Entry<Integer, Set<String>> entry : PermissionCatalog.DEFAULT_PERMISSIONS_BY_ROLE.entrySet()) {
                Integer roleId = entry.getKey();
                for (String key : entry.getValue()) {
                    rolePermissionRepository.save(RolePermission.builder()
                            .roleId(roleId)
                            .permissionKey(key)
                            .enabled(true)
                            .build());
                }
            }
            log.info("RolePermissionService: seeded default permissions for {} roles",
                    PermissionCatalog.DEFAULT_PERMISSIONS_BY_ROLE.size());
        } catch (Exception e) {
            log.warn("RolePermissionService: skip seeding (DB not ready or table missing): {}",
                    e.getMessage());
        }
    }

    public List<Permission> listAllPermissions() {
        return permissionRepository.findAll();
    }

    public List<RolePermission> listByRole(Integer roleId) {
        validateRoleId(roleId);
        return rolePermissionRepository.findByRoleId(roleId);
    }

    /**
     * Toggle 1 permission cho 1 role. Nếu bản ghi chưa có thì tạo mới.
     */
    @Transactional
    public RolePermission setPermission(Integer roleId, String permissionKey, boolean enabled) {
        validateRoleId(roleId);
        if (!permissionRepository.existsById(permissionKey)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        RolePermission.PK pk = new RolePermission.PK(roleId, permissionKey);
        RolePermission entity = rolePermissionRepository.findById(pk)
                .orElseGet(() -> RolePermission.builder()
                        .roleId(roleId)
                        .permissionKey(permissionKey)
                        .build());
        entity.setEnabled(enabled);
        return rolePermissionRepository.save(entity);
    }

    /**
     * Bulk update: nhận danh sách (permissionKey, enabled) cho 1 role.
     * Ghi đè toàn bộ mapping của role đó (chỉ giữ các permission tồn tại trong catalog).
     */
    @Transactional
    public List<RolePermission> replaceRolePermissions(Integer roleId, List<Map<String, Object>> items) {
        validateRoleId(roleId);
        List<String> validKeys = permissionRepository.findAll().stream().map(Permission::getKey).toList();
        List<RolePermission> results = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String key = String.valueOf(item.get("permissionKey"));
            Boolean enabled = (Boolean) item.get("enabled");
            if (!validKeys.contains(key) || enabled == null) continue;
            RolePermission entity = RolePermission.builder()
                    .roleId(roleId)
                    .permissionKey(key)
                    .enabled(enabled)
                    .build();
            results.add(rolePermissionRepository.save(entity));
        }
        return results;
    }

    /**
     * Reset về default (giúp user sửa sai có cái quay lại).
     */
    @Transactional
    public void resetToDefault(Integer roleId) {
        validateRoleId(roleId);
        rolePermissionRepository.deleteByRoleId(roleId);
        Set<String> defaults = PermissionCatalog.DEFAULT_PERMISSIONS_BY_ROLE.get(roleId);
        if (defaults == null) return;
        for (String key : defaults) {
            rolePermissionRepository.save(RolePermission.builder()
                    .roleId(roleId)
                    .permissionKey(key)
                    .enabled(true)
                    .build());
        }
    }

    private void validateRoleId(Integer roleId) {
        if (roleId == null || roleId < 1 || roleId > Roles.values().length) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }
}