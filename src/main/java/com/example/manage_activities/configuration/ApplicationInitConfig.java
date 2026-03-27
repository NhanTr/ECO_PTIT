package com.example.manage_activities.configuration;


import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.manage_activities.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.manage_activities.repository.UserRepository;

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
                admin.setUsername("admin");
                admin.setRoleId(1); // ADMIN role
                admin.setPasswordHash(passwordEncoder.encode("admin123")); // Default password, should be changed in production
                admin.setStatus("active");
                return userRepository.save(admin);
            });
        };
    }

}
