package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.ActivityCreateRequest;
import com.example.manage_activities.dto.request.ActivityReportRequest;
import com.example.manage_activities.dto.request.ActivityUpdateRequest;
import com.example.manage_activities.dto.response.ActivityFileResponse;
import com.example.manage_activities.dto.response.ActivityReviewResponse;
import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.dto.response.ActivityScheduleConflictResponse;
import com.example.manage_activities.dto.response.ClubStatisticsResponse;
import com.example.manage_activities.dto.response.ManagerActivityStatisticsResponse;
import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.entity.ActivityFile;
import com.example.manage_activities.entity.Attendance;
import com.example.manage_activities.entity.Registration;
import com.example.manage_activities.enums.ActivityStatus;
import com.example.manage_activities.enums.RegistrationStatus;
import com.example.manage_activities.enums.ReportStatus;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.mapper.ActivityMapper;
import com.example.manage_activities.repository.ActivityFileRepository;
import com.example.manage_activities.repository.ActivityRepository;
import com.example.manage_activities.repository.AttendanceRepository;
import com.example.manage_activities.repository.RegistrationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ActivityService {

    private static final List<ActivityStatus> EDITABLE_ACTIVITY_STATUSES =
            List.of(ActivityStatus.DRAFT, ActivityStatus.PENDING, ActivityStatus.REJECTED);
    private static final List<ActivityStatus> DELETABLE_ACTIVITY_STATUSES =
            List.of(ActivityStatus.DRAFT, ActivityStatus.PENDING);
    private static final List<RegistrationStatus> COUNTED_REGISTRATION_STATUSES =
            List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED);

    ActivityRepository activityRepository;
    ActivityMapper activityMapper;
    RegistrationRepository registrationRepository;
    ActivityFileRepository activityFileRepository;
    AttendanceRepository attendanceRepository;
    NotificationService notificationService;
    SystemConfigService systemConfigService;
    SystemLogService systemLogService;

    /**
     * Create a new activity
     */
    public ActivityResponse createActivity(ActivityCreateRequest request) {
        log.info("Creating activity: {}", request.getTitle());

        String organizerId = getCurrentUserId();
        log.info("Authenticated organizer ID: {}", organizerId);

        Activity activity = activityMapper.toEntity(request);

        activity.setId(generateActivityId());
        activity.setOrganizerId(organizerId);
        activity.setCurrentParticipants(0);
        activity.setStatus(ActivityStatus.DRAFT);
        activity.setCreatedAt(LocalDateTime.now());
        if (activity.getTrainingPoints() == null) {
            activity.setTrainingPoints(systemConfigService.getIntValue(SystemConfigService.DEFAULT_TRAINING_POINTS, 5));
        }

        Activity savedActivity = activityRepository.save(activity);
        systemLogService.logAction(organizerId, "CREATE_ACTIVITY", "activities", null,
                "activityId=" + savedActivity.getId() + ", status=" + savedActivity.getStatus().getValue());
        log.info("Activity created successfully with ID: {}", savedActivity.getId());
        return activityMapper.toDTO(savedActivity);
    }

    /**
     * Search activities visible to students.
     */
    public Page<ActivityResponse> getAvailableActivities(
            String keyword,
            String location,
            LocalDateTime fromTime,
            LocalDateTime toTime,
            Pageable pageable) {
        log.info("Getting available activities for students");

        return activityRepository.searchAvailableActivities(
                        List.of(ActivityStatus.APPROVED, ActivityStatus.ONGOING),
                        normalizeSearchValue(keyword),
                        normalizeSearchValue(location),
                        fromTime,
                        toTime,
                        pageable)
                .map(activityMapper::toDTO);
    }

    public Page<ActivityResponse> searchActivitiesForManager(
            List<String> statuses,
            String keyword,
            String location,
            LocalDateTime fromTime,
            LocalDateTime toTime,
            Pageable pageable) {
        return activityRepository.searchActivities(
                        parseActivityStatuses(statuses),
                        normalizeSearchValue(keyword),
                        normalizeSearchValue(location),
                        null,
                        fromTime,
                        toTime,
                        pageable)
                .map(activityMapper::toDTO);
    }

    public Page<ActivityResponse> searchActivities(
            String status,
            String sponsor,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String location,
            Pageable pageable) {
        List<String> statuses = status == null || status.isBlank() ? null : List.of(status);
        return activityRepository.searchActivities(
                        parseActivityStatuses(statuses),
                        null,
                        normalizeSearchValue(location),
                        normalizeSearchValue(sponsor),
                        startTime,
                        endTime,
                        pageable)
                .map(activityMapper::toDTO);
    }

    @Transactional
    public ActivityResponse updateActivity(String id, ActivityUpdateRequest request) {
        Activity activity = getActivityEntity(id);
        ensureCanManageActivity(activity);

        if (!EDITABLE_ACTIVITY_STATUSES.contains(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_CANNOT_EDIT);
        }

        applyUpdate(activity, request);
        Activity savedActivity = activityRepository.save(activity);
        systemLogService.logAction(getCurrentUserId(), "UPDATE_ACTIVITY", "activities",
                "activityId=" + id,
                "activityId=" + id + ", status=" + savedActivity.getStatus().getValue());
        return activityMapper.toDTO(savedActivity);
    }

    @Transactional
    public ActivityResponse managerUpdateActivity(String id, ActivityUpdateRequest request) {
        Activity activity = getActivityEntity(id);
        applyUpdate(activity, request);
        Activity savedActivity = activityRepository.save(activity);
        systemLogService.logAction(getCurrentUserId(), "MANAGER_UPDATE_ACTIVITY", "activities",
                "activityId=" + id,
                "activityId=" + id + ", status=" + savedActivity.getStatus().getValue());
        return activityMapper.toDTO(savedActivity);
    }

    @Transactional
    public void deleteActivity(String id) {
        Activity activity = getActivityEntity(id);
        ensureCanManageActivity(activity);

        if (!DELETABLE_ACTIVITY_STATUSES.contains(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_CANNOT_DELETE);
        }

        activityRepository.delete(activity);
        systemLogService.logAction(getCurrentUserId(), "DELETE_ACTIVITY", "activities",
                "activityId=" + id + ", status=" + activity.getStatus().getValue(), null);
    }

    @Transactional
    public ActivityResponse submitForReview(String id) {
        Activity activity = getActivityEntity(id);
        ensureCanManageActivity(activity);

        if (!ActivityStatus.DRAFT.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }

        activity.setStatus(ActivityStatus.PENDING);
        Activity savedActivity = activityRepository.save(activity);
        systemLogService.logAction(getCurrentUserId(), "SUBMIT_ACTIVITY", "activities",
                "activityId=" + id + ", status=Draft",
                "activityId=" + id + ", status=Pending");
        return activityMapper.toDTO(savedActivity);
    }

    @Transactional
    public ActivityResponse requestCancelActivity(String id, String reason) {
        Activity activity = getActivityEntity(id);
        ensureCanManageActivity(activity);

        if (!ActivityStatus.APPROVED.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }

        activity.setCancelReason(reason);
        activity.setStatus(ActivityStatus.REVIEWING);
        Activity savedActivity = activityRepository.save(activity);
        systemLogService.logAction(getCurrentUserId(), "REQUEST_CANCEL_ACTIVITY", "activities",
                "activityId=" + id + ", status=Approved",
                "activityId=" + id + ", status=Reviewing, reason=" + reason);
        return activityMapper.toDTO(savedActivity);
    }

    @Transactional
    public ActivityFileResponse submitReport(String activityId, ActivityReportRequest request) {
        Activity activity = getActivityEntity(activityId);
        ensureCanManageActivity(activity);

        if (!ActivityStatus.CLOSED.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_REPORT_NOT_ALLOWED);
        }

        ActivityFile report = ActivityFile.builder()
                .id(generateActivityFileId())
                .activityId(activityId)
                .uploadedBy(getCurrentUserId())
                .reportStatus(ReportStatus.PENDING)
                .fileUrl(request.getFileUrl())
                .fileType("Report")
                .originalFileName(request.getOriginalFileName())
                .contentType(request.getContentType())
                .fileSize(request.getFileSize())
                .uploadedAt(LocalDateTime.now())
                .reviewNote(request.getReviewNote())
                .build();

        ActivityFile savedReport = activityFileRepository.save(report);
        systemLogService.logAction(getCurrentUserId(), "SUBMIT_ACTIVITY_REPORT", "activity_files",
                null,
                "reportId=" + savedReport.getId() + ", activityId=" + activityId);
        return toActivityFileResponse(savedReport);
    }

    public ClubStatisticsResponse getMyClubStatistics(Integer year, Integer semester) {
        validateSemester(semester);
        String organizerId = getCurrentUserId();

        List<Activity> activities = activityRepository.findByOrganizerId(organizerId).stream()
                .filter(activity -> matchesPeriod(activity.getStartTime(), year, semester))
                .toList();

        long totalParticipants = activities.stream()
                .mapToLong(activity -> registrationRepository.countByActivityIdAndStatusIn(
                        activity.getId(), COUNTED_REGISTRATION_STATUSES))
                .sum();
        long closedActivities = activities.stream()
                .filter(activity -> ActivityStatus.CLOSED.equals(activity.getStatus()))
                .count();
        double completionRate = activities.isEmpty() ? 0 : (closedActivities * 100.0) / activities.size();

        return ClubStatisticsResponse.builder()
                .organizedActivities((long) activities.size())
                .totalParticipants(totalParticipants)
                .completionRate(completionRate)
                .year(year)
                .semester(semester)
                .build();
    }

    /**
     * Get activity by ID
     */
    public ActivityResponse getActivityById(String id) {
        log.info("Getting activity with ID: {}", id);
        return activityMapper.toDTO(getActivityEntity(id));
    }

    /**
     * Approve activity so it can be publicly available.
     */
    @Transactional
    public ActivityReviewResponse approveActivityWithWarnings(String id) {
        ActivityResponse activity = approveActivity(id);
        return ActivityReviewResponse.builder()
                .activity(activity)
                .scheduleConflicts(getScheduleConflicts(id))
                .build();
    }

    @Transactional
    public ActivityResponse approveActivity(String id) {
        log.info("Approving activity with ID: {}", id);

        Activity activity = getActivityEntity(id);

        if (ActivityStatus.APPROVED.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_ALREADY_APPROVED);
        }

        String reviewerId = getCurrentUserId();
        activity.setStatus(ActivityStatus.APPROVED);
        activity.setReviewerId(reviewerId);

        Activity savedActivity = activityRepository.save(activity);
        notifyOrganizer(savedActivity,
                "Hoat dong da duoc duyet",
                "Hoat dong \"" + savedActivity.getTitle() + "\" da duoc duyet.");
        systemLogService.logAction(reviewerId, "APPROVE_ACTIVITY", "activities",
                "activityId=" + id,
                "activityId=" + id + ", status=Approved");
        return activityMapper.toDTO(savedActivity);
    }

    /**
     * Reject activity so it can be publicly available.
     */
    @Transactional
    public ActivityResponse rejectActivity(String id, String reason) {
        log.info("Rejecting activity with ID: {}, reason: {}", id, reason);

        Activity activity = getActivityEntity(id);

        if (ActivityStatus.REJECTED.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_ALREADY_REJECTED);
        }

        String reviewerId = getCurrentUserId();
        activity.setStatus(ActivityStatus.REJECTED);
        activity.setReviewerId(reviewerId);

        Activity savedActivity = activityRepository.save(activity);
        notifyOrganizer(savedActivity,
                "Hoat dong bi tu choi",
                "Hoat dong \"" + savedActivity.getTitle() + "\" bi tu choi. Ly do: " + reason);
        systemLogService.logAction(reviewerId, "REJECT_ACTIVITY", "activities",
                "activityId=" + id,
                "activityId=" + id + ", status=Rejected, reason=" + reason);
        return activityMapper.toDTO(savedActivity);
    }

    @Transactional
    public ActivityResponse approveCancelRequest(String id) {
        Activity activity = getActivityEntity(id);
        if (!ActivityStatus.REVIEWING.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }

        activity.setStatus(ActivityStatus.CANCELLED);
        activity.setReviewerId(getCurrentUserId());
        Activity savedActivity = activityRepository.save(activity);
        notifyOrganizer(savedActivity,
                "Yeu cau huy hoat dong da duoc duyet",
                "Yeu cau huy hoat dong \"" + savedActivity.getTitle() + "\" da duoc chap nhan.");
        systemLogService.logAction(getCurrentUserId(), "APPROVE_CANCEL_ACTIVITY", "activities",
                "activityId=" + id + ", status=Reviewing",
                "activityId=" + id + ", status=Cancelled");
        return activityMapper.toDTO(savedActivity);
    }

    @Transactional
    public ActivityResponse rejectCancelRequest(String id, String reason) {
        Activity activity = getActivityEntity(id);
        if (!ActivityStatus.REVIEWING.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }

        activity.setStatus(ActivityStatus.APPROVED);
        activity.setReviewerId(getCurrentUserId());
        Activity savedActivity = activityRepository.save(activity);
        notifyOrganizer(savedActivity,
                "Yeu cau huy hoat dong bi tu choi",
                "Yeu cau huy hoat dong \"" + savedActivity.getTitle() + "\" bi tu choi. Ly do: " + reason);
        systemLogService.logAction(getCurrentUserId(), "REJECT_CANCEL_ACTIVITY", "activities",
                "activityId=" + id + ", status=Reviewing",
                "activityId=" + id + ", status=Approved, reason=" + reason);
        return activityMapper.toDTO(savedActivity);
    }

    public List<ActivityScheduleConflictResponse> getScheduleConflicts(String id) {
        Activity activity = getActivityEntity(id);
        return findScheduleConflicts(activity);
    }

    public ManagerActivityStatisticsResponse getManagerStatistics(Integer year, Integer semester) {
        validateSemester(semester);
        List<Activity> activities = activityRepository.findAll().stream()
                .filter(activity -> matchesPeriod(activity.getStartTime(), year, semester))
                .toList();
        List<String> activityIds = activities.stream()
                .map(Activity::getId)
                .toList();
        List<Registration> registrations = activityIds.isEmpty()
                ? List.of()
                : registrationRepository.findByActivityIdIn(activityIds);
        List<String> registrationIds = registrations.stream()
                .map(Registration::getId)
                .toList();
        List<Attendance> attendances = registrationIds.isEmpty()
                ? List.of()
                : attendanceRepository.findByRegistrationIdIn(registrationIds);

        long registeredStudents = registrations.stream()
                .filter(registration -> COUNTED_REGISTRATION_STATUSES.contains(registration.getStatus()))
                .count();
        long attendedStudents = attendances.stream()
                .filter(attendance -> Boolean.TRUE.equals(attendance.getIsPresent()))
                .count();
        long totalTrainingPoints = activities.stream()
                .map(Activity::getTrainingPoints)
                .filter(points -> points != null)
                .mapToLong(Integer::longValue)
                .sum();
        long totalEarnedPoints = attendances.stream()
                .map(Attendance::getEarnedPoints)
                .filter(points -> points != null)
                .mapToLong(Integer::longValue)
                .sum();

        return ManagerActivityStatisticsResponse.builder()
                .totalActivities((long) activities.size())
                .registeredStudents(registeredStudents)
                .attendedStudents(attendedStudents)
                .totalTrainingPoints(totalTrainingPoints)
                .totalEarnedPoints(totalEarnedPoints)
                .year(year)
                .semester(semester)
                .build();
    }

    public List<ActivityFileResponse> searchReports(String activityId, String reportStatus) {
        ReportStatus status = reportStatus == null || reportStatus.isBlank() ? null : parseReportStatus(reportStatus);
        return activityFileRepository.searchReports(normalizeSearchValue(activityId), status)
                .stream()
                .map(this::toActivityFileResponse)
                .toList();
    }

    @Transactional
    public ActivityFileResponse approveReport(String reportId) {
        ActivityFile report = getReportEntity(reportId);
        ensurePendingReport(report);

        Activity activity = getActivityEntity(report.getActivityId());
        report.setReportStatus(ReportStatus.APPROVED);
        report.setReviewerId(getCurrentUserId());
        report.setReviewedAt(LocalDateTime.now());
        ActivityFile savedReport = activityFileRepository.save(report);
        fixActivityPoints(activity);
        notifyOrganizer(activity,
                "Bao cao sau hoat dong da duoc duyet",
                "Bao cao cua hoat dong \"" + activity.getTitle() + "\" da duoc duyet. Diem hoat dong da duoc xac nhan.");
        systemLogService.logAction(getCurrentUserId(), "APPROVE_ACTIVITY_REPORT", "activity_files",
                "reportId=" + reportId + ", status=Pending",
                "reportId=" + reportId + ", status=Approved");
        return toActivityFileResponse(savedReport);
    }

    @Transactional
    public ActivityFileResponse rejectReport(String reportId, String reason) {
        ActivityFile report = getReportEntity(reportId);
        ensurePendingReport(report);

        Activity activity = getActivityEntity(report.getActivityId());
        report.setReportStatus(ReportStatus.REJECTED);
        report.setReviewerId(getCurrentUserId());
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewNote(reason);
        ActivityFile savedReport = activityFileRepository.save(report);
        notifyOrganizer(activity,
                "Bao cao sau hoat dong bi tu choi",
                "Bao cao cua hoat dong \"" + activity.getTitle() + "\" bi tu choi. Ly do: " + reason);
        systemLogService.logAction(getCurrentUserId(), "REJECT_ACTIVITY_REPORT", "activity_files",
                "reportId=" + reportId + ", status=Pending",
                "reportId=" + reportId + ", status=Rejected, reason=" + reason);
        return toActivityFileResponse(savedReport);
    }

    /**
     * Get all activities with pagination
     */
    public Page<ActivityResponse> getAllActivities(Pageable pageable) {
        log.info("Getting activities with pagination - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Activity> activities = activityRepository.findAll(pageable);
        return activities.map(activityMapper::toDTO);
    }

    /**
     * Get activities by organizer
     */
    public List<ActivityResponse> getActivityByOrganizer(String organizerId) {
        log.info("Getting activities for organizer: {}", organizerId);

        return activityRepository.findByOrganizerId(organizerId)
                .stream()
                .map(activityMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get activities by status
     */
    public List<ActivityResponse> getActivityByStatus(String status) {
        log.info("Getting activities with status: {}", status);
        ActivityStatus activityStatus = parseActivityStatus(status);

        return activityRepository.findByStatus(activityStatus)
                .stream()
                .map(activityMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Activity getActivityEntity(String id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ACTIVITY_NOT_FOUND));
    }

    public void ensureCanManageActivity(Activity activity) {
        if (hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")) {
            return;
        }

        String userId = getCurrentUserId();
        if (!userId.equals(activity.getOrganizerId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void applyUpdate(Activity activity, ActivityUpdateRequest request) {
        if (request.getTitle() != null) activity.setTitle(request.getTitle());
        if (request.getDescription() != null) activity.setDescription(request.getDescription());
        if (request.getLocation() != null) activity.setLocation(request.getLocation());
        if (request.getStartTime() != null) activity.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) activity.setEndTime(request.getEndTime());
        if (request.getRegistrationDeadline() != null) activity.setRegistrationDeadline(request.getRegistrationDeadline());
        if (request.getMaxParticipants() != null) activity.setMaxParticipants(request.getMaxParticipants());
        if (request.getBudget() != null) activity.setBudget(request.getBudget());
        if (request.getSponsor() != null) activity.setSponsor(request.getSponsor());
        if (request.getTargetAudience() != null) activity.setTargetAudience(request.getTargetAudience());
        if (request.getPurpose() != null) activity.setPurpose(request.getPurpose());
        if (request.getTrainingPoints() != null) activity.setTrainingPoints(request.getTrainingPoints());
    }

    private ActivityFileResponse toActivityFileResponse(ActivityFile activityFile) {
        return ActivityFileResponse.builder()
                .id(activityFile.getId())
                .activityId(activityFile.getActivityId())
                .reviewerId(activityFile.getReviewerId())
                .uploadedBy(activityFile.getUploadedBy())
                .reportStatus(activityFile.getReportStatus() == null ? null : activityFile.getReportStatus().getValue())
                .fileUrl(activityFile.getFileUrl())
                .fileType(activityFile.getFileType())
                .originalFileName(activityFile.getOriginalFileName())
                .contentType(activityFile.getContentType())
                .fileSize(activityFile.getFileSize())
                .uploadedAt(activityFile.getUploadedAt())
                .reviewedAt(activityFile.getReviewedAt())
                .reviewNote(activityFile.getReviewNote())
                .build();
    }

    private ActivityStatus parseActivityStatus(String status) {
        try {
            return ActivityStatus.from(status);
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private Collection<ActivityStatus> parseActivityStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Arrays.asList(ActivityStatus.values());
        }

        List<ActivityStatus> parsedStatuses = statuses.stream()
                .filter(status -> status != null && !status.isBlank())
                .map(this::parseActivityStatus)
                .toList();
        return parsedStatuses.isEmpty() ? Arrays.asList(ActivityStatus.values()) : parsedStatuses;
    }

    private ReportStatus parseReportStatus(String status) {
        try {
            return ReportStatus.from(status);
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private ActivityFile getReportEntity(String reportId) {
        ActivityFile report = activityFileRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.ACTIVITY_FILE_NOT_FOUND));
        if (!"Report".equalsIgnoreCase(report.getFileType())) {
            throw new AppException(ErrorCode.ACTIVITY_FILE_NOT_FOUND);
        }
        return report;
    }

    private void ensurePendingReport(ActivityFile report) {
        if (!ReportStatus.PENDING.equals(report.getReportStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_REPORT_ALREADY_REVIEWED);
        }
    }

    private void fixActivityPoints(Activity activity) {
        List<Registration> approvedRegistrations = registrationRepository.findByActivityId(activity.getId())
                .stream()
                .filter(registration -> RegistrationStatus.APPROVED.equals(registration.getStatus()))
                .toList();
        if (approvedRegistrations.isEmpty()) {
            return;
        }

        Map<String, Registration> registrationById = approvedRegistrations.stream()
                .collect(Collectors.toMap(Registration::getId, Function.identity()));
        List<Attendance> updatedAttendances = attendanceRepository.findByRegistrationIdIn(registrationById.keySet())
                .stream()
                .filter(attendance -> Boolean.TRUE.equals(attendance.getIsPresent()))
                .filter(attendance -> attendance.getEarnedPoints() == null)
                .peek(attendance -> attendance.setEarnedPoints(activity.getTrainingPoints() == null ? 0 : activity.getTrainingPoints()))
                .toList();

        if (!updatedAttendances.isEmpty()) {
            attendanceRepository.saveAll(updatedAttendances);
        }
    }

    private List<ActivityScheduleConflictResponse> findScheduleConflicts(Activity activity) {
        if (activity.getStartTime() == null || activity.getEndTime() == null) {
            return List.of();
        }

        return activityRepository.findApprovedOverlappingActivities(
                        activity.getId(),
                        ActivityStatus.APPROVED,
                        activity.getStartTime(),
                        activity.getEndTime())
                .stream()
                .map(conflict -> toScheduleConflictResponse(activity, conflict))
                .toList();
    }

    private ActivityScheduleConflictResponse toScheduleConflictResponse(Activity activity, Activity conflict) {
        boolean sameLocation = isSameLocation(activity.getLocation(), conflict.getLocation());
        boolean overlappingTime = isOverlapping(activity, conflict);
        String warning = sameLocation
                ? "Trung phong va trung khung gio voi hoat dong da duyet"
                : "Trung khung gio voi hoat dong da duyet";

        return ActivityScheduleConflictResponse.builder()
                .activityId(conflict.getId())
                .title(conflict.getTitle())
                .location(conflict.getLocation())
                .startTime(conflict.getStartTime())
                .endTime(conflict.getEndTime())
                .sameLocation(sameLocation)
                .overlappingTime(overlappingTime)
                .warning(warning)
                .build();
    }

    private boolean isSameLocation(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        return first.trim().toLowerCase(Locale.ROOT).equals(second.trim().toLowerCase(Locale.ROOT));
    }

    private boolean isOverlapping(Activity first, Activity second) {
        return first.getStartTime() != null
                && first.getEndTime() != null
                && second.getStartTime() != null
                && second.getEndTime() != null
                && first.getStartTime().isBefore(second.getEndTime())
                && first.getEndTime().isAfter(second.getStartTime());
    }

    private void notifyOrganizer(Activity activity, String title, String content) {
        notificationService.sendNotificationToUser(activity.getOrganizerId(), title, content, "Activity");
    }

    private String normalizeSearchValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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

    private String generateActivityId() {
        String id = UUID.randomUUID().toString().substring(0, 10);
        while (activityRepository.existsById(id)) {
            id = UUID.randomUUID().toString().substring(0, 10);
        }
        return id;
    }

    private String generateActivityFileId() {
        String id = UUID.randomUUID().toString().substring(0, 10);
        while (activityFileRepository.existsById(id)) {
            id = UUID.randomUUID().toString().substring(0, 10);
        }
        return id;
    }
}
