package com.example.manage_activities.service;

import com.example.manage_activities.dto.response.RegistrationResponse;
import com.example.manage_activities.dto.response.StudentActivityHistoryResponse;
import com.example.manage_activities.dto.response.StudentPointsResponse;
import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.entity.Attendance;
import com.example.manage_activities.entity.Profile;
import com.example.manage_activities.entity.Registration;
import com.example.manage_activities.entity.User;
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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RegistrationService {

    private static final List<RegistrationStatus> ACTIVE_REGISTRATION_STATUSES =
            List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED);
    private static final List<ActivityStatus> STUDENT_CONFLICT_ACTIVITY_STATUSES =
            List.of(ActivityStatus.APPROVED, ActivityStatus.ONGOING);

    RegistrationRepository registrationRepository;
    RegistrationMapper registrationMapper;
    NotificationService notificationService;
    ActivityRepository activityRepository;
    AttendanceRepository attendanceRepository;
    UserRepository userRepository;
    ProfileRepository profileRepository;
    SystemLogService systemLogService;
    SystemConfigService systemConfigService;

    /**
     * Register user for activity
     */
    @Transactional
    public RegistrationResponse registerActivity(String activityId) {
        log.info("Registering user for activity: {}", activityId);

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new AppException(ErrorCode.ACTIVITY_NOT_FOUND));

        validateRegistrationRule(activity);

        Optional<Registration> existingRegistration =
                registrationRepository.findByActivityIdAndStudentId(activityId, userId);
        Registration registration;

        if (existingRegistration.isPresent()) {
            registration = existingRegistration.get();
            if (!RegistrationStatus.CANCELLED.equals(registration.getStatus())) {
                throw new AppException(ErrorCode.EXIST_REGISTRATIONS);
            }
            registration.setStatus(RegistrationStatus.PENDING);
            registration.setApprovedBy(null);
            registration.setApprovedAt(null);
            registration.setCancelledAt(null);
            registration.setRejectReason(null);
            registration.setCreatedAt(LocalDateTime.now());
        } else {
            registration = new Registration();
            registration.setId(generateRegistrationId());
            registration.setActivityId(activityId);
            registration.setStudentId(userId);
            registration.setStatus(RegistrationStatus.PENDING);
            registration.setCreatedAt(LocalDateTime.now());
        }

        validateStudentScheduleConflict(activity, userId);

        Registration savedRegistration = registrationRepository.save(registration);
        refreshActivityParticipantCount(activity);
        systemLogService.logAction(userId, "REGISTER_ACTIVITY", "registrations",
                null,
                "registrationId=" + savedRegistration.getId() + ", activityId=" + activityId + ", status=" + savedRegistration.getStatus().getValue());
        return registrationMapper.toDTO(savedRegistration);
    }

    /**
     * Unregister user from activity
     */
    @Transactional
    public void unregisterActivity(String activityId) {
        log.info("Unregistering user from activity: {}", activityId);

        String studentId = SecurityContextHolder.getContext().getAuthentication().getName();
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new AppException(ErrorCode.ACTIVITY_NOT_FOUND));

        Registration registration = registrationRepository.findByActivityIdAndStudentId(activityId, studentId)
                .orElseThrow(() -> new AppException(ErrorCode.NO_REGISTRATIONS));

        validateCancellationRule(activity, registration);

        RegistrationStatus oldStatus = registration.getStatus();
        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());
        registrationRepository.save(registration);
        refreshActivityParticipantCount(activity);

        systemLogService.logAction(
                studentId,
                "CANCEL_REGISTRATION",
                "registrations",
                "registrationId=" + registration.getId() + ", status=" + oldStatus.getValue(),
                "registrationId=" + registration.getId() + ", status=" + RegistrationStatus.CANCELLED.getValue()
        );

        log.info("User cancelled registration successfully from activity: {}", activityId);
    }

    /**
     * Get user's registrations
     */
    public List<RegistrationResponse> getUserRegistrations() {
        log.info("Getting user registrations");

        String studentId = SecurityContextHolder.getContext().getAuthentication().getName();

        List<Registration> registrations = registrationRepository.findByStudentId(studentId);

        if (registrations.isEmpty()) {
            log.info("No registrations found for user: {}", studentId);
            throw new AppException(ErrorCode.NO_REGISTRATIONS);
        } else {
            log.info("Found {} registrations for user: {}", registrations.size(), studentId);
        }

        return registrations.stream()
                .map(this::toRegistrationResponse)
                .collect(Collectors.toList());
    }

    public List<StudentActivityHistoryResponse> getMyActivityHistory(Integer year, Integer semester) {
        validateSemester(semester);
        String studentId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Registration> registrations = registrationRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        return buildHistoryResponses(registrations, year, semester);
    }

    public StudentPointsResponse getMyPoints(Integer year, Integer semester) {
        List<StudentActivityHistoryResponse> activities = getMyActivityHistory(year, semester);
        int totalPoints = activities.stream()
                .map(StudentActivityHistoryResponse::getEarnedPoints)
                .filter(points -> points != null)
                .mapToInt(Integer::intValue)
                .sum();

        return StudentPointsResponse.builder()
                .totalPoints(totalPoints)
                .activities(activities)
                .build();
    }

    /**
     * Get activity registrations
     */
    public List<RegistrationResponse> getActivityRegistrations(String activityId) {
        log.info("Getting registrations for activity: {}", activityId);

        return registrationRepository.findByActivityId(activityId)
                .stream()
                .map(this::toRegistrationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RegistrationResponse approveRegistration(String activityId, String studentId) {
        log.info("Approving registration for activityId: {}, studentId: {}", activityId, studentId);

        Registration registration = registrationRepository.findByActivityIdAndStudentId(activityId, studentId)
                .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new AppException(ErrorCode.ACTIVITY_NOT_FOUND));
        ensureCanManageActivity(activity);

        if (!RegistrationStatus.PENDING.equals(registration.getStatus())) {
            if (RegistrationStatus.APPROVED.equals(registration.getStatus())) {
                throw new AppException(ErrorCode.REGISTRATION_ALREADY_APPROVED);
            }
            if (RegistrationStatus.REJECTED.equals(registration.getStatus())) {
                throw new AppException(ErrorCode.REGISTRATION_ALREADY_REJECTED);
            }
            if (RegistrationStatus.CANCELLED.equals(registration.getStatus())) {
                throw new AppException(ErrorCode.REGISTRATION_CANCELLED);
            }
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        validateStudentScheduleConflict(activity, studentId);

        registration.setStatus(RegistrationStatus.APPROVED);
        registration.setApprovedBy(getCurrentUserId());
        registration.setApprovedAt(LocalDateTime.now());
        registration.setRejectReason(null);
        Registration savedRegistration = registrationRepository.save(registration);
        refreshActivityParticipantCount(activity);
        notificationService.sendParticipationApprovedNotification(studentId, activity.getTitle());
        systemLogService.logAction(getCurrentUserId(), "APPROVE_REGISTRATION", "registrations",
                "registrationId=" + registration.getId() + ", status=Pending",
                "registrationId=" + registration.getId() + ", status=Approved");
        return registrationMapper.toDTO(savedRegistration);
    }
    /**
     * Get registration count for activity
     */
    public Long getActivityRegistrationCount(String activityId) {
        log.info("Getting registration count for activity: {}", activityId);
        return registrationRepository.countByActivityIdAndStatusIn(activityId, ACTIVE_REGISTRATION_STATUSES);
    }

    /**
     * Reject one registered student in an activity and notify the student.
     */
    public void rejectRegistration(String activityId, String studentId, String reason) {
        log.info("Rejecting registration for activityId: {}, studentId: {}", activityId, studentId);

        Registration registration = registrationRepository
                .findByActivityIdAndStudentId(activityId, studentId)
                .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_NOT_FOUND));
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new AppException(ErrorCode.ACTIVITY_NOT_FOUND));
        ensureCanManageActivity(activity);

        if (!RegistrationStatus.PENDING.equals(registration.getStatus())) {
            if (RegistrationStatus.REJECTED.equals(registration.getStatus())) {
                throw new AppException(ErrorCode.REGISTRATION_ALREADY_REJECTED);
            }
            if (RegistrationStatus.CANCELLED.equals(registration.getStatus())) {
                throw new AppException(ErrorCode.REGISTRATION_CANCELLED);
            }
            if (RegistrationStatus.APPROVED.equals(registration.getStatus())) {
                throw new AppException(ErrorCode.REGISTRATION_ALREADY_APPROVED);
            }
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        registration.setStatus(RegistrationStatus.REJECTED);
        registration.setApprovedBy(null);
        registration.setApprovedAt(null);
        registration.setRejectReason(reason);
        registrationRepository.save(registration);
        refreshActivityParticipantCount(activity);

        notificationService.sendParticipationRejectedNotification(
                registration.getStudentId(),
                activity.getTitle(),
                reason
        );
        systemLogService.logAction(getCurrentUserId(), "REJECT_REGISTRATION", "registrations",
                "registrationId=" + registration.getId() + ", status=Pending",
                "registrationId=" + registration.getId() + ", status=Rejected, reason=" + reason);

        log.info("Registration rejected and notification sent for activityId: {}, studentId: {}", activityId, studentId);
    }

    private void validateRegistrationRule(Activity activity) {
        if (!ActivityStatus.APPROVED.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_NOT_AVAILABLE_FOR_REGISTRATION);
        }

        LocalDateTime now = LocalDateTime.now();
        if (activity.getRegistrationDeadline() != null && now.isAfter(activity.getRegistrationDeadline())) {
            throw new AppException(ErrorCode.REGISTRATION_DEADLINE_EXPIRED);
        }

        Long activeCount = registrationRepository.countByActivityIdAndStatusIn(
                activity.getId(), ACTIVE_REGISTRATION_STATUSES);
        if (activity.getMaxParticipants() != null && activeCount >= activity.getMaxParticipants()) {
            throw new AppException(ErrorCode.ACTIVITY_FULL);
        }
    }

    private void validateStudentScheduleConflict(Activity activity, String studentId) {
        List<Registration> conflicts = registrationRepository.findStudentScheduleConflicts(
                studentId,
                activity.getId(),
                ACTIVE_REGISTRATION_STATUSES,
                STUDENT_CONFLICT_ACTIVITY_STATUSES,
                activity.getStartTime(),
                activity.getEndTime());

        if (!conflicts.isEmpty()) {
            throw new AppException(ErrorCode.STUDENT_ACTIVITY_TIME_CONFLICT);
        }
    }

    private void validateCancellationRule(Activity activity, Registration registration) {
        if (!ACTIVE_REGISTRATION_STATUSES.contains(registration.getStatus())) {
            throw new AppException(ErrorCode.REGISTRATION_CANNOT_CANCEL);
        }

        if (ActivityStatus.ONGOING.equals(activity.getStatus())
                || ActivityStatus.CLOSED.equals(activity.getStatus())
                || ActivityStatus.CANCELLED.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.REGISTRATION_CANNOT_CANCEL);
        }

        if (activity.getStartTime() == null) {
            return;
        }

        int cancelDeadlineHours = systemConfigService.getIntValue(
                SystemConfigService.REGISTRATION_CANCEL_DEADLINE_HOURS,
                24);
        LocalDateTime cancellationDeadline = activity.getStartTime().minusHours(cancelDeadlineHours);
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(cancellationDeadline)) {
            throw new AppException(ErrorCode.REGISTRATION_CANNOT_CANCEL);
        }
    }

    private void refreshActivityParticipantCount(Activity activity) {
        Long activeCount = registrationRepository.countByActivityIdAndStatusIn(
                activity.getId(), ACTIVE_REGISTRATION_STATUSES);
        activity.setCurrentParticipants(activeCount.intValue());
        activityRepository.save(activity);
    }

    private List<StudentActivityHistoryResponse> buildHistoryResponses(
            List<Registration> registrations,
            Integer year,
            Integer semester) {
        List<String> registrationIds = registrations.stream()
                .map(Registration::getId)
                .toList();
        Map<String, Attendance> attendanceByRegistrationId = attendanceRepository.findByRegistrationIdIn(registrationIds)
                .stream()
                .collect(Collectors.toMap(Attendance::getRegistrationId, Function.identity(), (first, second) -> first));

        return registrations.stream()
                .map(registration -> toHistoryResponse(registration, attendanceByRegistrationId.get(registration.getId())))
                .filter(history -> matchesPeriod(history.getStartTime(), year, semester))
                .toList();
    }

    private StudentActivityHistoryResponse toHistoryResponse(Registration registration, Attendance attendance) {
        Activity activity = activityRepository.findById(registration.getActivityId()).orElse(null);
        return StudentActivityHistoryResponse.builder()
                .registrationId(registration.getId())
                .activityId(registration.getActivityId())
                .activityTitle(activity == null ? null : activity.getTitle())
                .location(activity == null ? null : activity.getLocation())
                .startTime(activity == null ? null : activity.getStartTime())
                .endTime(activity == null ? null : activity.getEndTime())
                .activityStatus(activity == null || activity.getStatus() == null ? null : activity.getStatus().getValue())
                .registrationStatus(registration.getStatus() == null ? null : registration.getStatus().getValue())
                .isPresent(attendance == null ? null : attendance.getIsPresent())
                .checkInTime(attendance == null ? null : attendance.getCheckInTime())
                .earnedPoints(attendance == null ? 0 : attendance.getEarnedPoints())
                .registeredAt(registration.getCreatedAt())
                .build();
    }

    private RegistrationResponse toRegistrationResponse(Registration registration) {
        Attendance attendance = attendanceRepository.findByRegistrationId(registration.getId()).orElse(null);
        return toRegistrationResponse(registration, attendance);
    }

    private RegistrationResponse toRegistrationResponse(Registration registration, Attendance attendance) {
        RegistrationResponse response = registrationMapper.toDTO(registration);
        if (response == null) {
            return null;
        }
        User student = userRepository.findById(registration.getStudentId()).orElse(null);
        Profile profile = profileRepository.findByUserId(registration.getStudentId());
        if (student != null) {
            response.setStudentEmail(student.getEmail());
        }
        if (profile != null) {
            response.setStudentName(profile.getFullName());
            response.setStudentCode(profile.getStudentCode());
            response.setClassName(profile.getClassName());
            response.setDepartment(profile.getDepartment());
        }
        if (attendance == null) {
            return response;
        }

        response.setAttendanceId(attendance.getId());
        response.setIsPresent(attendance.getIsPresent());
        response.setCheckInTime(attendance.getCheckInTime());
        response.setEarnedPoints(attendance.getEarnedPoints());
        return response;
    }

    private boolean matchesPeriod(LocalDateTime startTime, Integer year, Integer semester) {
        if (year == null && semester == null) {
            return true;
        }
        if (startTime == null) {
            return false;
        }
        if (year != null && startTime.getYear() != year) {
            return false;
        }
        if (semester == null) {
            return true;
        }

        int month = startTime.getMonthValue();
        return semester == 1 ? month <= 6 : month >= 7;
    }

    private void validateSemester(Integer semester) {
        if (semester != null && semester != 1 && semester != 2) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private void ensureCanManageActivity(Activity activity) {
        if (hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")) {
            return;
        }
        if (!getCurrentUserId().equals(activity.getOrganizerId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private boolean hasAnyAuthority(String... authorities) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (String authority : authorities) {
            boolean matched = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * Generate a unique ID for the registration
      */
    private String generateRegistrationId() {
        String id = UUID.randomUUID().toString().substring(0, 10);
        while (registrationRepository.existsById(id)) {
            id = UUID.randomUUID().toString().substring(0, 10);
        }
        return id;
    }
}
