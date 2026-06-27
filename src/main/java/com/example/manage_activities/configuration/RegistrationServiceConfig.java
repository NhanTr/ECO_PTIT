package com.example.manage_activities.configuration;

import com.example.manage_activities.mapper.RegistrationMapper;
import com.example.manage_activities.repository.ActivityRepository;
import com.example.manage_activities.repository.AttendanceRepository;
import com.example.manage_activities.repository.ProfileRepository;
import com.example.manage_activities.repository.RegistrationRepository;
import com.example.manage_activities.repository.UserRepository;
import com.example.manage_activities.service.NotificationService;
import com.example.manage_activities.service.RegistrationService;
import com.example.manage_activities.service.SystemConfigService;
import com.example.manage_activities.service.SystemLogService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RegistrationServiceConfig {

    @Bean
    @Primary
    RegistrationService explicitRegistrationService(
            RegistrationRepository registrationRepository,
            RegistrationMapper registrationMapper,
            NotificationService notificationService,
            ActivityRepository activityRepository,
            AttendanceRepository attendanceRepository,
            UserRepository userRepository,
            ProfileRepository profileRepository,
            SystemLogService systemLogService,
            SystemConfigService systemConfigService) {
        return new RegistrationService(
                registrationRepository,
                registrationMapper,
                notificationService,
                activityRepository,
                attendanceRepository,
                userRepository,
                profileRepository,
                systemLogService,
                systemConfigService);
    }
}
