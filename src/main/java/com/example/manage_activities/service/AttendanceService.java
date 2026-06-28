package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.AttendanceRequest;
import com.example.manage_activities.dto.request.AwardPointsRequest;
import com.example.manage_activities.dto.response.AttendanceResponse;
import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.entity.Attendance;
import com.example.manage_activities.entity.Registration;
import com.example.manage_activities.enums.ActivityStatus;
import com.example.manage_activities.enums.RegistrationStatus;
import com.example.manage_activities.enums.ReportStatus;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.ActivityFileRepository;
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
    ActivityFileRepository activityFileRepository;
    ActivityService activityService;
    SystemLogService systemLogService;

    @Transactional
    public AttendanceResponse checkIn(AttendanceRequest request) {
        return checkInInternal(request, true);
    }

    @Transactional
    public AttendanceResponse selfCheckIn(AttendanceRequest request) {
        return checkInInternal(request, false);
    }

    private AttendanceResponse checkInInternal(AttendanceRequest request, boolean requireActivityManager) {
        Registration registration = registrationRepository.findById(request.getRegistrationId())
                .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));
        Activity activity = activityService.getActivityEntity(registration.getActivityId());
        if (requireActivityManager) {
            activityService.ensureCanManageActivity(activity);
        } else if (!getCurrentUserId().equals(registration.getStudentId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        boolean attendanceAllowed = requireActivityManager
                ? ActivityStatus.ONGOING.equals(activity.getStatus()) || ActivityStatus.CLOSED.equals(activity.getStatus())
                : ActivityStatus.ONGOING.equals(activity.getStatus());
        if (!attendanceAllowed) {
            throw new AppException(ErrorCode.ATTENDANCE_NOT_ALLOWED);
        }
        if (!RegistrationStatus.APPROVED.equals(registration.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        var existingAttendance = attendanceRepository.findByRegistrationId(registration.getId());
        boolean newAttendance = existingAttendance.isEmpty();
        Attendance attendance = existingAttendance
                .orElseGet(() -> Attendance.builder()
                        .id(generateAttendanceId())
                        .registrationId(registration.getId())
                        .build());

        if (requireActivityManager) {
            attendance.setIsPresent(Boolean.TRUE.equals(request.getIsPresent()));
        } else {
            attendance.setCheckInTime(LocalDateTime.now());
            if (newAttendance) {
                attendance.setIsPresent(null);
            }
        }

        Attendance savedAttendance = attendanceRepository.save(attendance);
        systemLogService.logAction(
                getCurrentUserId(),
                "CHECK_IN_ATTENDANCE",
                "attendance",
                "registrationId=" + registration.getId(),
                "attendanceId=" + savedAttendance.getId() + ", isPresent=" + savedAttendance.getIsPresent());
        return toResponse(savedAttendance);
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

        if (!activityFileRepository
                .findFirstByActivityIdAndFileTypeAndReportStatusOrderByUploadedAtDesc(
                        activity.getId(), "Report", ReportStatus.APPROVED)
                .isPresent()) {
            throw new AppException(ErrorCode.REPORT_NOT_APPROVED);
        }

        Attendance attendance = attendanceRepository.findByRegistrationId(registration.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ATTENDANCE_NOT_FOUND));
        if (!Boolean.TRUE.equals(attendance.getIsPresent())) {
            throw new AppException(ErrorCode.POINT_AWARD_NOT_ALLOWED);
        }

        attendance.setEarnedPoints(request.getEarnedPoints() == null ? activity.getTrainingPoints() : request.getEarnedPoints());
        Attendance savedAttendance = attendanceRepository.save(attendance);
        systemLogService.logAction(
                getCurrentUserId(),
                "AWARD_POINTS",
                "attendance",
                "attendanceId=" + attendance.getId(),
                "attendanceId=" + attendance.getId() + ", earnedPoints=" + savedAttendance.getEarnedPoints());
        return toResponse(savedAttendance);
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

    private String getCurrentUserId() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
