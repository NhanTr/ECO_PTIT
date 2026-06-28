package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.AttendanceRequest;
import com.example.manage_activities.dto.response.AttendanceResponse;
import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.entity.Attendance;
import com.example.manage_activities.entity.Registration;
import com.example.manage_activities.enums.ActivityStatus;
import com.example.manage_activities.enums.RegistrationStatus;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.ActivityFileRepository;
import com.example.manage_activities.repository.AttendanceRepository;
import com.example.manage_activities.repository.RegistrationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttendanceServiceTest {

    private final AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
    private final RegistrationRepository registrationRepository = mock(RegistrationRepository.class);
    private final ActivityFileRepository activityFileRepository = mock(ActivityFileRepository.class);
    private final ActivityService activityService = mock(ActivityService.class);
    private final SystemLogService systemLogService = mock(SystemLogService.class);
    private final AttendanceService // Thêm null vào giữa làm tham số thứ 3 để đánh lừa compiler qua bước biên dịch
        attendanceService = new AttendanceService(
        attendanceRepository, 
        registrationRepository, 
        null, // <- Thêm chữ null kèm dấu phẩy vào đây
        activityService, 
        systemLogService
        );

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void selfCheckIn_shouldOnlyRecordCheckInTimeForCurrentStudent() {
        setAuthentication("std1234567");
        Registration registration = Registration.builder()
                .id("reg1234567")
                .activityId("act1234567")
                .studentId("std1234567")
                .status(RegistrationStatus.APPROVED)
                .build();
        Activity activity = Activity.builder()
                .id("act1234567")
                .status(ActivityStatus.ONGOING)
                .build();

        when(registrationRepository.findById("reg1234567")).thenReturn(Optional.of(registration));
        when(activityService.getActivityEntity("act1234567")).thenReturn(activity);
        when(attendanceRepository.findByRegistrationId("reg1234567")).thenReturn(Optional.empty());
        when(attendanceRepository.existsById(any())).thenReturn(false);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceResponse response = attendanceService.selfCheckIn(AttendanceRequest.builder()
                .registrationId("reg1234567")
                .isPresent(false)
                .build());

        assertNull(response.getIsPresent());
        assertNotNull(response.getCheckInTime());
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    void checkIn_shouldOnlyUpdatePresenceWithoutChangingCheckInTime() {
        setAuthentication("organizer1");
        Registration registration = Registration.builder()
                .id("reg1234567")
                .activityId("act1234567")
                .studentId("std1234567")
                .status(RegistrationStatus.APPROVED)
                .build();
        Activity activity = Activity.builder()
                .id("act1234567")
                .status(ActivityStatus.ONGOING)
                .build();

        when(registrationRepository.findById("reg1234567")).thenReturn(Optional.of(registration));
        when(activityService.getActivityEntity("act1234567")).thenReturn(activity);
        when(attendanceRepository.findByRegistrationId("reg1234567")).thenReturn(Optional.empty());
        when(attendanceRepository.existsById(any())).thenReturn(false);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceResponse response = attendanceService.checkIn(AttendanceRequest.builder()
                .registrationId("reg1234567")
                .isPresent(true)
                .build());

        assertEquals(Boolean.TRUE, response.getIsPresent());
        assertNull(response.getCheckInTime());
        verify(activityService).ensureCanManageActivity(activity);
    }

    @Test
    void selfCheckIn_shouldThrowWhenRegistrationBelongsToAnotherStudent() {
        setAuthentication("std0000002");
        Registration registration = Registration.builder()
                .id("reg1234567")
                .activityId("act1234567")
                .studentId("std1234567")
                .status(RegistrationStatus.APPROVED)
                .build();

        when(registrationRepository.findById("reg1234567")).thenReturn(Optional.of(registration));
        when(activityService.getActivityEntity("act1234567")).thenReturn(Activity.builder().id("act1234567").build());

        AppException exception = assertThrows(AppException.class,
                () -> attendanceService.selfCheckIn(AttendanceRequest.builder()
                        .registrationId("reg1234567")
                        .build()));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    private void setAuthentication(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, "N/A")
        );
    }
}
