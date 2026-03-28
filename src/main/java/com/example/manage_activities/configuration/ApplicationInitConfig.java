package com.example.manage_activities.configuration;


import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.manage_activities.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.manage_activities.repository.UserRepository;

import java.util.UUID;

@Configuration
public class ApplicationInitConfig {
    

    private final PasswordEncoder passwordEncoder;

    ApplicationInitConfig(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository) {
        return args -> {
            userRepository.findByUsername("admin").orElseGet(() -> {
                User admin = new User();
                admin.setId(UUID.randomUUID().toString().substring(0, 10)); // Set ID manually
                admin.setUsername("admin");
                admin.setRoleId(1); // ADMIN role
                admin.setEmail("admin@example.com");
                admin.setPasswordHash(passwordEncoder.encode("admin123")); // Default password, should be changed in production
                admin.setStatus("active");
                admin.setCreatedAt(java.time.LocalDateTime.now());
                return userRepository.save(admin);
            });
        };
    }

}
