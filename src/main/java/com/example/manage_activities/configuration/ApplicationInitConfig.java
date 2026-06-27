package com.example.manage_activities.configuration;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.manage_activities.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.manage_activities.repository.UserRepository;

import java.util.UUID;

@Configuration
@Slf4j
public class ApplicationInitConfig {

    @Value("${admin.default-username:admin}")
    private String defaultUsername;

    @Value("${admin.default-password:admin123}")
    private String defaultPassword;

    private final PasswordEncoder passwordEncoder;

    ApplicationInitConfig(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository) {
        return args -> {
            log.info("ApplicationInitConfig: checking default admin account...");
            // Chỉ tạo tài khoản admin mặc định khi DB chưa có user nào.
            // Mật khẩu và username đọc từ env (ADMIN_DEFAULT_USERNAME/ADMIN_DEFAULT_PASSWORD),
            // nếu không set sẽ dùng admin/admin123 — phù hợp cho môi trường dev.
            // Production: BẮT BUỘC set ADMIN_DEFAULT_PASSWORD trong .env và đổi sau lần đăng nhập đầu tiên.
            long count = userRepository.count();
            log.info("ApplicationInitConfig: current user count = {}", count);
            if (count > 0) {
                log.info("ApplicationInitConfig: skip seeding admin (DB already has users)");
                return;
            }

            User admin = new User();
            admin.setId(UUID.randomUUID().toString().substring(0, 10));
            admin.setUsername(defaultUsername);
            admin.setRoleId(1); // ADMIN role
            admin.setEmail(defaultUsername + "@example.com");
            admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
            admin.setStatus("active");
            admin.setCreatedAt(java.time.LocalDateTime.now());
            userRepository.save(admin);
            log.info("ApplicationInitConfig: seeded default admin user '{}'", defaultUsername);
        };
    }

}