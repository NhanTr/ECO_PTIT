package com.example.manage_activities.repository;

import com.example.manage_activities.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.PK> {
    List<RolePermission> findByRoleId(Integer roleId);
    void deleteByRoleId(Integer roleId);
}