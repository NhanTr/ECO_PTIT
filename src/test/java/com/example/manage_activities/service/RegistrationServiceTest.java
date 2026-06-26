package com.example.manage_activities.service;

import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.entity.Registration;
import com.example.manage_activities.enums.ActivityStatus;
import com.example.manage_activities.enums.RegistrationStatus;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.mapper.RegistrationMapper;
import com.example.manage_activities.repository.ActivityRepository;
import com.example.manage_activities.repository.AttendanceRepository;
import com.example.manage_activities.repository.ProfileRepository;
import com.example.manage_activities.repository.RegistrationRepository;
import com.example.manage_activities.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {

    private final RegistrationRepository registrationRepository = mock(RegistrationRepository.class);
    private final RegistrationMapper registrationMapper = mock(RegistrationMapper.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ActivityRepository activityRepository = mock(ActivityRepository.class);
    private final AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final SystemLogService systemLogService = mock(SystemLogService.class);
    private final SystemConfigService systemConfigService = mock(SystemConfigService.class);
    private final RegistrationService registrationService =
            new RegistrationService(registrationRepository, registrationMapper, notificationService,
                    activityRepository, attendanceRepository, userRepository, profileRepository,
                    systemLogService, systemConfigService);

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectRegistration_shouldRejectPendingStudentAndSendNotification() {
        setManagerAuthentication();
        Registration registration = Registration.builder()
                .id("reg1234567")
                .activityId("act1234567")
                .studentId("std1234567")
                .status(RegistrationStatus.PENDING)
                .build();

        when(registrationRepository.findByActivityIdAndStudentId("act1234567", "std1234567"))
                .thenReturn(Optional.of(registration));
        when(activityRepository.findById("act1234567"))
                .thenReturn(Optional.of(Activity.builder().id("act1234567").organizerId("org1234567").build()));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registrationService.rejectRegistration("act1234567", "std1234567", "Khong du dieu kien");

        assertEquals(RegistrationStatus.REJECTED, registration.getStatus());
        verify(registrationRepository).save(registration);
        verify(notificationService).sendParticipationRejectedNotification("std1234567", "act1234567", "Khong du dieu kien");
    }

    @Test
    void approveRegistration_shouldApprovePendingStudentAndSendNotification() {
        setManagerAuthentication();
        Registration registration = Registration.builder()
                .id("reg1234567")
                .activityId("act1234567")
                .studentId("std1234567")
                .status(RegistrationStatus.PENDING)
                .build();

        when(registrationRepository.findByActivityIdAndStudentId("act1234567", "std1234567"))
                .thenReturn(Optional.of(registration));
        when(activityRepository.findById("act1234567"))
                .thenReturn(Optional.of(Activity.builder().id("act1234567").organizerId("org1234567").build()));
        when(registrationRepository.findStudentScheduleConflicts(
                "std1234567",
                "act1234567",
                List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED),
                List.of(ActivityStatus.APPROVED, ActivityStatus.ONGOING),
                null,
                null))
                .thenReturn(List.of());
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registrationMapper.toDTO(any(Registration.class))).thenReturn(null);

        registrationService.approveRegistration("act1234567", "std1234567");

        assertEquals(RegistrationStatus.APPROVED, registration.getStatus());
        verify(registrationRepository).save(registration);
        verify(notificationService).sendParticipationApprovedNotification("std1234567", "act1234567");
    }

    @Test
    void rejectRegistration_shouldThrowWhenAlreadyRejected() {
        setManagerAuthentication();
        Registration registration = Registration.builder()
                .id("reg1234567")
                .activityId("act1234567")
                .studentId("std1234567")
                .status(RegistrationStatus.REJECTED)
                .build();

        when(registrationRepository.findByActivityIdAndStudentId("act1234567", "std1234567"))
                .thenReturn(Optional.of(registration));
        when(activityRepository.findById("act1234567"))
                .thenReturn(Optional.of(Activity.builder().id("act1234567").organizerId("org1234567").build()));

        AppException exception = assertThrows(AppException.class,
                () -> registrationService.rejectRegistration("act1234567", "std1234567", "Invalid"));

        assertEquals(ErrorCode.REGISTRATION_ALREADY_REJECTED, exception.getErrorCode());
    }

    @Test
    void registerActivity_shouldThrowWhenStudentHasScheduleConflict() {
        setStudentAuthentication();
        LocalDateTime startTime = LocalDateTime.of(2026, 7, 1, 9, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 7, 1, 11, 0);
        Activity activity = Activity.builder()
                .id("act1234567")
                .status(ActivityStatus.APPROVED)
                .startTime(startTime)
                .endTime(endTime)
                .maxParticipants(100)
                .build();
        Registration conflictingRegistration = Registration.builder()
                .id("reg7654321")
                .activityId("act7654321")
                .studentId("std1234567")
                .status(RegistrationStatus.APPROVED)
                .build();

        when(activityRepository.findById("act1234567")).thenReturn(Optional.of(activity));
        when(registrationRepository.countByActivityIdAndStatusIn("act1234567", List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED)))
                .thenReturn(0L);
        when(registrationRepository.findByActivityIdAndStudentId("act1234567", "std1234567"))
                .thenReturn(Optional.empty());
        when(registrationRepository.findStudentScheduleConflicts(
                "std1234567",
                "act1234567",
                List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED),
                List.of(ActivityStatus.APPROVED, ActivityStatus.ONGOING),
                startTime,
                endTime))
                .thenReturn(List.of(conflictingRegistration));

        AppException exception = assertThrows(AppException.class,
                () -> registrationService.registerActivity("act1234567"));

        assertEquals(ErrorCode.STUDENT_ACTIVITY_TIME_CONFLICT, exception.getErrorCode());
    }

    private void setManagerAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "manager01",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
                )
        );
    }

    private void setStudentAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "std1234567",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                )
        );
    }
}
