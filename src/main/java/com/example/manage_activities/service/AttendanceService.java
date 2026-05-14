package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.AttendanceRequest;
import com.example.manage_activities.dto.request.AwardPointsRequest;
import com.example.manage_activities.dto.response.AttendanceResponse;
import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.entity.Attendance;
import com.example.manage_activities.entity.Registration;
import com.example.manage_activities.enums.ActivityStatus;
import com.example.manage_activities.enums.RegistrationStatus;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.AttendanceRepository;
import com.example.manage_activities.repository.RegistrationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AttendanceService {

    AttendanceRepository attendanceRepository;
    RegistrationRepository registrationRepository;
    ActivityService activityService;

    @Transactional
    public AttendanceResponse checkIn(AttendanceRequest request) {
        Registration registration = registrationRepository.findById(request.getRegistrationId())
                .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));
        Activity activity = activityService.getActivityEntity(registration.getActivityId());
        activityService.ensureCanManageActivity(activity);

        if (!ActivityStatus.ONGOING.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ATTENDANCE_NOT_ALLOWED);
        }
        if (!RegistrationStatus.APPROVED.equals(registration.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        Attendance attendance = attendanceRepository.findByRegistrationId(registration.getId())
                .orElseGet(() -> Attendance.builder()
                        .id(generateAttendanceId())
                        .registrationId(registration.getId())
                        .build());

        attendance.setIsPresent(Boolean.TRUE.equals(request.getIsPresent()));
        attendance.setCheckInTime(LocalDateTime.now());
        if (!Boolean.TRUE.equals(attendance.getIsPresent())) {
            attendance.setEarnedPoints(0);
        }

        return toResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public AttendanceResponse awardPoints(AwardPointsRequest request) {
        Registration registration = registrationRepository.findById(request.getRegistrationId())
                .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));
        Activity activity = activityService.getActivityEntity(registration.getActivityId());
        activityService.ensureCanManageActivity(activity);

        if (!ActivityStatus.CLOSED.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.POINT_AWARD_NOT_ALLOWED);
        }

        Attendance attendance = attendanceRepository.findByRegistrationId(registration.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ATTENDANCE_NOT_FOUND));
        if (!Boolean.TRUE.equals(attendance.getIsPresent())) {
            throw new AppException(ErrorCode.POINT_AWARD_NOT_ALLOWED);
        }

        attendance.setEarnedPoints(request.getEarnedPoints() == null ? activity.getTrainingPoints() : request.getEarnedPoints());
        return toResponse(attendanceRepository.save(attendance));
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .registrationId(attendance.getRegistrationId())
                .checkInTime(attendance.getCheckInTime())
                .isPresent(attendance.getIsPresent())
                .earnedPoints(attendance.getEarnedPoints())
                .build();
    }

    private String generateAttendanceId() {
        String id = UUID.randomUUID().toString().substring(0, 10);
        while (attendanceRepository.existsById(id)) {
            id = UUID.randomUUID().toString().substring(0, 10);
        }
        return id;
    }
}