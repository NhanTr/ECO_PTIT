package com.example.manage_activities.service;

import com.example.manage_activities.dto.response.ActivityStatisticsItemResponse;
import com.example.manage_activities.dto.response.ActivityStatisticsResponse;
import com.example.manage_activities.dto.response.StudentStatisticsItemResponse;
import com.example.manage_activities.dto.response.StudentStatisticsResponse;
import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.entity.Profile;
import com.example.manage_activities.entity.User;
import com.example.manage_activities.enums.ActivityStatus;
import com.example.manage_activities.enums.RegistrationStatus;
import com.example.manage_activities.enums.Roles;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.ActivityRepository;
import com.example.manage_activities.repository.AttendanceRepository;
import com.example.manage_activities.repository.ProfileRepository;
import com.example.manage_activities.repository.RegistrationRepository;
import com.example.manage_activities.repository.UserRepository;
import com.example.manage_activities.repository.projection.ActivityRegistrationCountProjection;
import com.example.manage_activities.repository.projection.ActivityStatusCountProjection;
import com.example.manage_activities.repository.projection.StudentStatisticsProjection;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StatisticsService {

    private static final List<RegistrationStatus> COUNTED_REGISTRATION_STATUSES =
            List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED);

    ActivityRepository activityRepository;
    RegistrationRepository registrationRepository;
    AttendanceRepository attendanceRepository;
    ProfileRepository profileRepository;
    UserRepository userRepository;

    @Transactional(readOnly = true)
    public ActivityStatisticsResponse getActivityStatistics(LocalDateTime fromTime, LocalDateTime toTime) {
        validateTimeRange(fromTime, toTime);

        List<Activity> activities = activityRepository.findForStatisticsReport(fromTime, toTime);
        Map<ActivityStatus, Long> statusCounts = buildStatusCountMap(fromTime, toTime);

        List<String> activityIds = activities.stream().map(Activity::getId).toList();
        Map<String, Long> registrationCounts = loadRegistrationCounts(activityIds);
        Map<String, Long> attendedCounts = loadAttendedCounts(activityIds);
        Map<String, String> organizerNames = loadOrganizerNames(activities);

        List<ActivityStatisticsItemResponse> items = activities.stream()
                .map(activity -> ActivityStatisticsItemResponse.builder()
                        .activityId(activity.getId())
                        .title(activity.getTitle())
                        .organizerName(organizerNames.getOrDefault(activity.getOrganizerId(), ""))
                        .registeredStudentCount(registrationCounts.getOrDefault(activity.getId(), 0L))
                        .attendedStudentCount(attendedCounts.getOrDefault(activity.getId(), 0L))
                        .status(activity.getStatus() != null ? activity.getStatus().name() : null)
                        .build())
                .toList();

        long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();

        return ActivityStatisticsResponse.builder()
                .fromTime(fromTime)
                .toTime(toTime)
                .totalActivities(total)
                .draftCount(statusCounts.getOrDefault(ActivityStatus.DRAFT, 0L))
                .pendingCount(statusCounts.getOrDefault(ActivityStatus.PENDING, 0L))
                .reviewingCount(statusCounts.getOrDefault(ActivityStatus.REVIEWING, 0L))
                .approvedCount(statusCounts.getOrDefault(ActivityStatus.APPROVED, 0L))
                .ongoingCount(statusCounts.getOrDefault(ActivityStatus.ONGOING, 0L))
                .closedCount(statusCounts.getOrDefault(ActivityStatus.CLOSED, 0L))
                .rejectedCount(statusCounts.getOrDefault(ActivityStatus.REJECTED, 0L))
                .cancelledCount(statusCounts.getOrDefault(ActivityStatus.CANCELLED, 0L))
                .activities(items)
                .build();
    }

    @Transactional(readOnly = true)
    public StudentStatisticsResponse getStudentStatistics(
            LocalDateTime fromTime,
            LocalDateTime toTime,
            String className,
            String department) {
        validateTimeRange(fromTime, toTime);

        String normalizedClass = normalizeFilter(className);
        String normalizedDepartment = normalizeFilter(department);

        List<StudentStatisticsProjection> rows = registrationRepository.aggregateStudentStatistics(
                Roles.STUDENT.getId(),
                fromTime,
                toTime,
                normalizedDepartment,
                normalizedClass);

        List<StudentStatisticsItemResponse> students = rows.stream()
                .map(row -> StudentStatisticsItemResponse.builder()
                        .studentId(row.getStudentId())
                        .studentCode(row.getStudentCode())
                        .fullName(row.getFullName())
                        .className(row.getClassName())
                        .department(row.getDepartment())
                        .participatedActivityCount(row.getParticipatedActivityCount())
                        .totalEarnedPoints(row.getTotalEarnedPoints())
                        .build())
                .toList();

        return StudentStatisticsResponse.builder()
                .fromTime(fromTime)
                .toTime(toTime)
                .className(normalizedClass)
                .department(normalizedDepartment)
                .students(students)
                .build();
    }

    private Map<ActivityStatus, Long> buildStatusCountMap(LocalDateTime fromTime, LocalDateTime toTime) {
        Map<ActivityStatus, Long> counts = new EnumMap<>(ActivityStatus.class);
        for (ActivityStatus status : ActivityStatus.values()) {
            counts.put(status, 0L);
        }
        for (ActivityStatusCountProjection row : activityRepository.countActivitiesGroupByStatus(fromTime, toTime)) {
            if (row.getStatus() != null && row.getCount() != null) {
                counts.put(row.getStatus(), row.getCount());
            }
        }
        return counts;
    }

    private Map<String, Long> loadRegistrationCounts(List<String> activityIds) {
        if (activityIds.isEmpty()) {
            return Map.of();
        }
        return registrationRepository
                .countRegistrationsByActivityIds(activityIds, COUNTED_REGISTRATION_STATUSES)
                .stream()
                .collect(Collectors.toMap(
                        ActivityRegistrationCountProjection::getActivityId,
                        ActivityRegistrationCountProjection::getRegistrationCount,
                        Long::sum));
    }

    private Map<String, Long> loadAttendedCounts(List<String> activityIds) {
        if (activityIds.isEmpty()) {
            return Map.of();
        }
        return attendanceRepository.countPresentAttendeesByActivityIds(activityIds)
                .stream()
                .collect(Collectors.toMap(
                        ActivityRegistrationCountProjection::getActivityId,
                        ActivityRegistrationCountProjection::getRegistrationCount,
                        Long::sum));
    }

    private Map<String, String> loadOrganizerNames(List<Activity> activities) {
        List<String> organizerIds = activities.stream()
                .map(Activity::getOrganizerId)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();
        if (organizerIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Profile> profilesByUserId = profileRepository.findByUserIdIn(organizerIds).stream()
                .collect(Collectors.toMap(Profile::getUserId, Function.identity(), (left, right) -> left));
        Map<String, User> usersById = userRepository.findByIdIn(organizerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));

        return organizerIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        organizerId -> resolveDisplayName(profilesByUserId.get(organizerId), usersById.get(organizerId)),
                        (left, right) -> left));
    }

    private String resolveDisplayName(Profile profile, User user) {
        if (profile != null && profile.getFullName() != null && !profile.getFullName().isBlank()) {
            return profile.getFullName();
        }
        if (user != null && user.getUsername() != null) {
            return user.getUsername();
        }
        return "";
    }

    private void validateTimeRange(LocalDateTime fromTime, LocalDateTime toTime) {
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
