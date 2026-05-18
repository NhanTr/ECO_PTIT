package com.example.manage_activities.configuration;

import com.example.manage_activities.entity.Role;
import com.example.manage_activities.enums.Roles;
import com.example.manage_activities.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleDataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Roles role : Roles.values()) {
            String roleName = role.name();
            if (roleRepository.findByRoleName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().roleName(roleName).build());
                log.info("Seeded role: expectedId={}, name={}", role.getId(), roleName);
            }
        }
    }
}
