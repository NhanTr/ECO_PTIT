package com.example.manage_activities.service;

import com.example.manage_activities.dto.response.RoleResponse;
import com.example.manage_activities.enums.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class RoleService {

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return Arrays.stream(Roles.values())
                .map(role -> RoleResponse.builder()
                        .id(role.getId())
                        .roleName(role.name())
                        .displayNameVi(role.getDisplayNameVi())
                        .build())
                .toList();
    }
}
