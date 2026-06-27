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
import com.example.manage_activities.entity.Profile;
import com.example.manage_activities.entity.Registration;
import com.example.manage_activities.entity.Room;
import com.example.manage_activities.entity.User;
import com.example.manage_activities.enums.ActivityStatus;
import com.example.manage_activities.enums.RegistrationStatus;
import com.example.manage_activities.enums.ReportStatus;
import com.example.manage_activities.enums.Roles;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.mapper.ActivityMapper;
import com.example.manage_activities.repository.ActivityFileRepository;
import com.example.manage_activities.repository.ActivityRepository;
import com.example.manage_activities.repository.AttendanceRepository;
import com.example.manage_activities.repository.ProfileRepository;
import com.example.manage_activities.repository.RegistrationRepository;
import com.example.manage_activities.repository.RoomRepository;
import com.example.manage_activities.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
    private static final List<ActivityStatus> AUTO_CLOSE_ACTIVITY_STATUSES =
            List.of(ActivityStatus.APPROVED, ActivityStatus.ONGOING);
    private static final List<ActivityStatus> SCHEDULE_CONFLICT_STATUSES =
            List.of(ActivityStatus.APPROVED, ActivityStatus.ONGOING);
    private static final List<ActivityStatus> ORGANIZER_TIME_CONFLICT_STATUSES =
            List.of(
                    ActivityStatus.DRAFT,
                    ActivityStatus.PENDING,
                    ActivityStatus.REVIEWING,
                    ActivityStatus.CANCELLATION_REQUESTED,
                    ActivityStatus.APPROVED,
                    ActivityStatus.ONGOING);
    private static final Path ACTIVITY_REPORT_UPLOAD_DIRECTORY = Path.of("uploads", "activity-reports");

    ActivityRepository activityRepository;
    ActivityMapper activityMapper;
    RegistrationRepository registrationRepository;
    ActivityFileRepository activityFileRepository;
    AttendanceRepository attendanceRepository;
    RoomRepository roomRepository;
    ProfileRepository profileRepository;
    UserRepository userRepository;
    NotificationService notificationService;
    SystemConfigService systemConfigService;
    SystemLogService systemLogService;

    @Scheduled(fixedDelayString = "${activity.lifecycle.start-due.fixed-delay-ms:60000}")
    @Transactional
    public void startDueActivities() {
        int startedCount = startDueActivities(LocalDateTime.now());
        if (startedCount > 0) {
            log.info("Auto-started {} due activities", startedCount);
        }
    }

    int startDueActivities(LocalDateTime now) {
        return activityRepository.startDueActivities(
                ActivityStatus.APPROVED,
                ActivityStatus.ONGOING,
                now);
    }

    @Scheduled(fixedDelayString = "${activity.lifecycle.close-expired.fixed-delay-ms:60000}")
    @Transactional
    public void closeExpiredActivities() {
        int closedCount = closeExpiredActivities(LocalDateTime.now());
        if (closedCount > 0) {
            log.info("Auto-closed {} expired activities", closedCount);
        }
    }

    int closeExpiredActivities(LocalDateTime now) {
        return activityRepository.closeExpiredActivities(
                AUTO_CLOSE_ACTIVITY_STATUSES,
                ActivityStatus.CLOSED,
                now);
    }

    /**
     * Create a new activity
     */
    public ActivityResponse createActivity(ActivityCreateRequest request) {
        log.info("Creating activity: {}", request.getTitle());

        String organizerId = getCurrentUserId();
        log.info("Authenticated organizer ID: {}", organizerId);
        validateActivityTimeRange(request.getStartTime(), request.getEndTime());

        Activity activity = activityMapper.toEntity(request);

        activity.setId(generateActivityId());
        activity.setOrganizerId(organizerId);
        applyRoom(activity, request.getRoomId());
        activity.setCurrentParticipants(0);
        activity.setStatus(ActivityStatus.DRAFT);
        activity.setCreatedAt(LocalDateTime.now());
        if (activity.getTrainingPoints() == null) {
            activity.setTrainingPoints(systemConfigService.getIntValue(SystemConfigService.DEFAULT_TRAINING_POINTS, 5));
        }
        ensureOrganizerHasNoOverlappingActivity(activity);

        Activity savedActivity = activityRepository.save(activity);
        systemLogService.logAction(organizerId, "CREATE_ACTIVITY", "activities", null,
                "activityId=" + savedActivity.getId() + ", status=" + savedActivity.getStatus().getValue());
        log.info("Activity created successfully with ID: {}", savedActivity.getId());
        return toActivityResponse(savedActivity);
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
                .map(this::toActivityResponse);
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
                .map(this::toActivityResponse);
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
                .map(this::toActivityResponse);
    }

    @Transactional
    public ActivityResponse updateActivity(String id, ActivityUpdateRequest request) {
        Activity activity = getActivityEntity(id);
        ensureCanManageActivity(activity);

        if (!EDITABLE_ACTIVITY_STATUSES.contains(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_CANNOT_EDIT);
        }

        applyUpdate(activity, request);
        validateActivityTimeRange(activity.getStartTime(), activity.getEndTime());
        ensureOrganizerHasNoOverlappingActivity(activity);
        Activity savedActivity = activityRepository.save(activity);
        systemLogService.logAction(getCurrentUserId(), "UPDATE_ACTIVITY", "activities",
                "activityId=" + id,
                "activityId=" + id + ", status=" + savedActivity.getStatus().getValue());
        return toActivityResponse(savedActivity);
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
    public ActivityReviewResponse submitForReview(String id) {
        Activity activity = getActivityEntity(id);
        ensureCanManageActivity(activity);

        if (!ActivityStatus.DRAFT.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }

        validateActivityTimeRange(activity.getStartTime(), activity.getEndTime());
        ensureOrganizerHasNoOverlappingActivity(activity);
        activity.setStatus(ActivityStatus.PENDING);
        Activity savedActivity = activityRepository.save(activity);
        List<ActivityScheduleConflictResponse> scheduleConflicts = findScheduleConflicts(savedActivity);
        if (!scheduleConflicts.isEmpty()) {
            log.warn("Schedule conflict warning for activityId={}: {} conflicting activity(ies)",
                    id, scheduleConflicts.size());
        }
        systemLogService.logAction(getCurrentUserId(), "SUBMIT_ACTIVITY", "activities",
                "activityId=" + id + ", status=Draft",
                "activityId=" + id + ", status=Pending");
        return ActivityReviewResponse.builder()
                .activity(toActivityResponse(savedActivity))
                .scheduleConflicts(scheduleConflicts)
                .build();
    }

    @Transactional
    public ActivityResponse requestCancelActivity(String id, String reason) {
        Activity activity = getActivityEntity(id);
        ensureCanManageActivity(activity);

        if (!ActivityStatus.APPROVED.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }

        activity.setCancelReason(reason);
        activity.setStatus(ActivityStatus.CANCELLATION_REQUESTED);
        Activity savedActivity = activityRepository.save(activity);
        systemLogService.logAction(getCurrentUserId(), "REQUEST_CANCEL_ACTIVITY", "activities",
                "activityId=" + id + ", status=Approved",
                "activityId=" + id + ", status=CancellationRequested, reason=" + reason);
        return toActivityResponse(savedActivity);
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

    @Transactional
    public ActivityFileResponse submitReportFile(String activityId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        Activity activity = getActivityEntity(activityId);
        ensureCanManageActivity(activity);

        if (!ActivityStatus.CLOSED.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_REPORT_NOT_ALLOWED);
        }

        String originalFileName = normalizeFileName(file.getOriginalFilename());
        if (!isExcelFile(originalFileName)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        String reportId = generateActivityFileId();
        String storedFileName = reportId + getExtension(originalFileName);
        Path uploadPath = ACTIVITY_REPORT_UPLOAD_DIRECTORY.toAbsolutePath().normalize();
        Path targetPath = uploadPath.resolve(storedFileName).normalize();

        if (!targetPath.startsWith(uploadPath)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        try {
            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            log.error("Could not store report file for activityId={}", activityId, exception);
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        ActivityFile report = ActivityFile.builder()
                .id(reportId)
                .activityId(activityId)
                .uploadedBy(getCurrentUserId())
                .reportStatus(ReportStatus.PENDING)
                .fileUrl("/uploads/activity-reports/" + storedFileName)
                .fileType("Report")
                .originalFileName(originalFileName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .build();

        ActivityFile savedReport = activityFileRepository.save(report);
        systemLogService.logAction(getCurrentUserId(), "UPLOAD_ACTIVITY_REPORT", "activity_files",
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
        return toActivityResponse(getActivityEntity(id));
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
    public ActivityResponse assignReviewer(String id, String reviewerId) {
        Activity activity = getActivityEntity(id);
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Roles reviewerRole = Roles.fromId(reviewer.getRoleId());
        if (reviewerRole != Roles.ADMIN && reviewerRole != Roles.MANAGER) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        if (!"active".equalsIgnoreCase(reviewer.getStatus())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String oldReviewerId = activity.getReviewerId();
        activity.setReviewerId(reviewerId);
        Activity savedActivity = activityRepository.save(activity);
        notifyOrganizer(savedActivity,
                "Hoạt động đã được phân công người duyệt",
                "Hoạt động \"" + savedActivity.getTitle() + "\" đã được phân công người phụ trách kiểm duyệt.");
        systemLogService.logAction(getCurrentUserId(), "ASSIGN_ACTIVITY_REVIEWER", "activities",
                "activityId=" + id + ", reviewerId=" + oldReviewerId,
                "activityId=" + id + ", reviewerId=" + reviewerId);
        return toActivityResponse(savedActivity);
    }

    @Transactional
    public ActivityResponse startActivityReview(String id) {
        Activity activity = getActivityEntity(id);

        if (ActivityStatus.REVIEWING.equals(activity.getStatus())) {
            return toActivityResponse(activity);
        }
        if (!ActivityStatus.PENDING.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }

        String reviewerId = getCurrentUserId();
        activity.setStatus(ActivityStatus.REVIEWING);
        activity.setReviewerId(reviewerId);
        Activity savedActivity = activityRepository.save(activity);
        systemLogService.logAction(reviewerId, "START_ACTIVITY_REVIEW", "activities",
                "activityId=" + id + ", status=Pending",
                "activityId=" + id + ", status=Reviewing");
        return toActivityResponse(savedActivity);
    }

    @Transactional
    public ActivityResponse approveActivity(String id) {
        log.info("Approving activity with ID: {}", id);

        Activity activity = getActivityEntity(id);

        if (ActivityStatus.APPROVED.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_ALREADY_APPROVED);
        }
        if (!ActivityStatus.REVIEWING.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }

        validateActivityTimeRange(activity.getStartTime(), activity.getEndTime());
        ensureOrganizerHasNoOverlappingActivity(activity);
        String reviewerId = getCurrentUserId();
        activity.setStatus(ActivityStatus.APPROVED);
        activity.setReviewerId(reviewerId);

        Activity savedActivity = activityRepository.save(activity);
        notifyOrganizer(savedActivity,
                "Hoạt động đã được duyệt",
                "Hoạt động \"" + savedActivity.getTitle() + "\" đã được duyệt.");
        systemLogService.logAction(reviewerId, "APPROVE_ACTIVITY", "activities",
                "activityId=" + id,
                "activityId=" + id + ", status=Approved");
        return toActivityResponse(savedActivity);
    }

    /**
     * Reject activity proposal (QLHĐ_QĐ 1).
     */
    @Transactional
    public ActivityResponse rejectActivity(String id, String rejectReason) {
        log.info("Rejecting activity with ID: {}, reason: {}", id, rejectReason);

        Activity activity = getActivityEntity(id);

        if (ActivityStatus.REJECTED.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_ALREADY_REJECTED);
        }
        if (!ActivityStatus.REVIEWING.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }

        String reviewerId = getCurrentUserId();
        activity.setRejectReason(rejectReason);
        activity.setStatus(ActivityStatus.REJECTED);
        activity.setReviewerId(reviewerId);

        Activity savedActivity = activityRepository.save(activity);
        notifyOrganizer(savedActivity,
                "Hoạt động bị từ chối",
                "Hoạt động \"" + savedActivity.getTitle() + "\" bị từ chối. Lý do: " + rejectReason);
        systemLogService.logAction(reviewerId, "REJECT_ACTIVITY", "activities",
                "activityId=" + id,
                "activityId=" + id + ", status=Rejected, rejectReason=" + rejectReason);
        return toActivityResponse(savedActivity);
    }

    @Transactional
    public ActivityResponse approveCancelRequest(String id) {
        Activity activity = getActivityEntity(id);
        if (!ActivityStatus.CANCELLATION_REQUESTED.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }

        activity.setStatus(ActivityStatus.CANCELLED);
        activity.setReviewerId(getCurrentUserId());
        Activity savedActivity = activityRepository.save(activity);
        notifyOrganizer(savedActivity,
                "Yêu cầu hủy hoạt động đã được duyệt",
                "Yêu cầu hủy hoạt động \"" + savedActivity.getTitle() + "\" đã được chấp nhận.");
        systemLogService.logAction(getCurrentUserId(), "APPROVE_CANCEL_ACTIVITY", "activities",
                "activityId=" + id + ", status=CancellationRequested",
                "activityId=" + id + ", status=Cancelled");
        return toActivityResponse(savedActivity);
    }

    @Transactional
    public ActivityResponse rejectCancelRequest(String id, String reason) {
        Activity activity = getActivityEntity(id);
        if (!ActivityStatus.CANCELLATION_REQUESTED.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }

        activity.setStatus(ActivityStatus.APPROVED);
        activity.setReviewerId(getCurrentUserId());
        Activity savedActivity = activityRepository.save(activity);
        notifyOrganizer(savedActivity,
                "Yêu cầu hủy hoạt động bị từ chối",
                "Yêu cầu hủy hoạt động \"" + savedActivity.getTitle() + "\" bị từ chối. Lý do: " + reason);
        systemLogService.logAction(getCurrentUserId(), "REJECT_CANCEL_ACTIVITY", "activities",
                "activityId=" + id + ", status=CancellationRequested",
                "activityId=" + id + ", status=Approved, reason=" + reason);
        return toActivityResponse(savedActivity);
    }

    @Transactional
    public ActivityResponse cancelApprovedActivityByManager(String id, String reason) {
        Activity activity = getActivityEntity(id);
        if (!ActivityStatus.APPROVED.equals(activity.getStatus())
                && !ActivityStatus.ONGOING.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }

        String reviewerId = getCurrentUserId();
        ActivityStatus oldStatus = activity.getStatus();
        activity.setStatus(ActivityStatus.CANCELLED);
        activity.setReviewerId(reviewerId);
        activity.setCancelReason(reason);
        Activity savedActivity = activityRepository.save(activity);
        notifyOrganizer(savedActivity,
                "Hoạt động đã bị hủy",
                "Hoạt động \"" + savedActivity.getTitle() + "\" đã bị hủy. Lý do: " + reason);
        systemLogService.logAction(reviewerId, "CANCEL_APPROVED_ACTIVITY", "activities",
                "activityId=" + id + ", status=" + oldStatus.getValue(),
                "activityId=" + id + ", status=Cancelled, reason=" + reason);
        return toActivityResponse(savedActivity);
    }

    public List<ActivityScheduleConflictResponse> getScheduleConflicts(String id) {
        Activity activity = getActivityEntity(id);
        return findScheduleConflicts(activity);
    }

    public List<ActivityScheduleConflictResponse> previewScheduleConflicts(
            String roomId,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        validateActivityTimeRange(startTime, endTime);
        Activity activity = Activity.builder()
                .id("")
                .startTime(startTime)
                .endTime(endTime)
                .build();
        applyRoom(activity, roomId);
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

    public List<ActivityFileResponse> searchMyReports(String activityId, String reportStatus) {
        ReportStatus status = reportStatus == null || reportStatus.isBlank() ? null : parseReportStatus(reportStatus);
        return activityFileRepository.searchReportsByOrganizer(getCurrentUserId(), normalizeSearchValue(activityId), status)
                .stream()
                .map(this::toActivityFileResponse)
                .toList();
    }

    @Transactional
    public ActivityFileResponse cancelMyReport(String reportId) {
        ActivityFile report = getReportEntity(reportId);
        Activity activity = getActivityEntity(report.getActivityId());
        ensureCanManageActivity(activity);

        if (!ReportStatus.PENDING.equals(report.getReportStatus())
                && !ReportStatus.APPROVED.equals(report.getReportStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }

        ReportStatus oldStatus = report.getReportStatus();
        report.setReportStatus(ReportStatus.CANCELLED);
        ActivityFile savedReport = activityFileRepository.save(report);
        systemLogService.logAction(getCurrentUserId(), "CANCEL_ACTIVITY_REPORT", "activity_files",
                "reportId=" + reportId + ", status=" + oldStatus.getValue(),
                "reportId=" + reportId + ", status=Cancelled");
        return toActivityFileResponse(savedReport);
    }

    @Transactional
    public ActivityFileResponse startReportReview(String reportId) {
        ActivityFile report = getReportEntity(reportId);

        if (ReportStatus.REVIEWING.equals(report.getReportStatus())) {
            return toActivityFileResponse(report);
        }
        if (!ReportStatus.PENDING.equals(report.getReportStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_REPORT_ALREADY_REVIEWED);
        }

        report.setReportStatus(ReportStatus.REVIEWING);
        report.setReviewerId(getCurrentUserId());
        ActivityFile savedReport = activityFileRepository.save(report);
        systemLogService.logAction(getCurrentUserId(), "DOWNLOAD_ACTIVITY_REPORT", "activity_files",
                "reportId=" + reportId + ", status=Pending",
                "reportId=" + reportId + ", status=Reviewing");
        return toActivityFileResponse(savedReport);
    }

    @Transactional
    public ActivityFileResponse approveReport(String reportId) {
        ActivityFile report = getReportEntity(reportId);
        ensureReviewingReport(report);

        Activity activity = getActivityEntity(report.getActivityId());
        report.setReportStatus(ReportStatus.APPROVED);
        report.setReviewerId(getCurrentUserId());
        report.setReviewedAt(LocalDateTime.now());
        ActivityFile savedReport = activityFileRepository.save(report);
        fixActivityPoints(activity);
        notifyOrganizer(activity,
                "Báo cáo sau hoạt động đã được duyệt",
                "Báo cáo của hoạt động \"" + activity.getTitle() + "\" đã được duyệt. Điểm hoạt động đã được xác nhận.");
        systemLogService.logAction(getCurrentUserId(), "APPROVE_ACTIVITY_REPORT", "activity_files",
                "reportId=" + reportId + ", status=Reviewing",
                "reportId=" + reportId + ", status=Approved");
        return toActivityFileResponse(savedReport);
    }

    @Transactional
    public ActivityFileResponse rejectReport(String reportId, String reason) {
        ActivityFile report = getReportEntity(reportId);
        ensureReviewingReport(report);

        Activity activity = getActivityEntity(report.getActivityId());
        report.setReportStatus(ReportStatus.REJECTED);
        report.setReviewerId(getCurrentUserId());
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewNote(reason);
        ActivityFile savedReport = activityFileRepository.save(report);
        notifyOrganizer(activity,
                "Báo cáo sau hoạt động bị từ chối",
                "Báo cáo của hoạt động \"" + activity.getTitle() + "\" bị từ chối. Lý do: " + reason);
        systemLogService.logAction(getCurrentUserId(), "REJECT_ACTIVITY_REPORT", "activity_files",
                "reportId=" + reportId + ", status=Reviewing",
                "reportId=" + reportId + ", status=Rejected, reason=" + reason);
        return toActivityFileResponse(savedReport);
    }

    /**
     * Get all activities with pagination
     */
    public Page<ActivityResponse> getAllActivities(Pageable pageable) {
        log.info("Getting activities with pagination - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Activity> activities = activityRepository.findAll(pageable);
        return activities.map(this::toActivityResponse);
    }

    /**
     * Get activities by organizer
     */
    public List<ActivityResponse> getActivityByOrganizer(String organizerId) {
        log.info("Getting activities for organizer: {}", organizerId);

        return activityRepository.findByOrganizerId(organizerId)
                .stream()
                .map(this::toActivityResponse)
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
                .map(this::toActivityResponse)
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
        if (request.getRoomId() != null) applyRoom(activity, request.getRoomId());
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

    private ActivityResponse toActivityResponse(Activity activity) {
        ActivityResponse response = activityMapper.toDTO(activity);
        if (response != null) {
            response.setOrganizerName(resolveOrganizerName(activity.getOrganizerId()));
        }
        return response;
    }

    private String resolveOrganizerName(String organizerId) {
        if (organizerId == null || organizerId.isBlank()) {
            return null;
        }

        Profile profile = profileRepository.findByUserId(organizerId);
        if (profile != null && profile.getFullName() != null && !profile.getFullName().isBlank()) {
            return profile.getFullName();
        }

        return userRepository.findById(organizerId)
                .map(user -> {
                    if (user.getUsername() != null && !user.getUsername().isBlank()) {
                        return user.getUsername();
                    }
                    if (user.getEmail() != null && !user.getEmail().isBlank()) {
                        return user.getEmail();
                    }
                    return user.getId();
                })
                .orElse(organizerId);
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

    private void ensureReviewingReport(ActivityFile report) {
        if (ReportStatus.PENDING.equals(report.getReportStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_REPORT_NOT_DOWNLOADED);
        }
        if (!ReportStatus.REVIEWING.equals(report.getReportStatus())) {
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
        String normalizedRoomId = normalizeSearchValue(activity.getRoomId());
        if (normalizedRoomId == null) {
            return List.of();
        }

        return activityRepository.findScheduleConflicts(
                        activity.getId(),
                        SCHEDULE_CONFLICT_STATUSES,
                        normalizedRoomId,
                        activity.getStartTime(),
                        activity.getEndTime())
                .stream()
                .map(conflict -> toScheduleConflictResponse(conflict))
                .toList();
    }

    private ActivityScheduleConflictResponse toScheduleConflictResponse(Activity conflict) {
        return ActivityScheduleConflictResponse.builder()
                .activityId(conflict.getId())
                .title(conflict.getTitle())
                .location(conflict.getLocation())
                .startTime(conflict.getStartTime())
                .endTime(conflict.getEndTime())
                .sameLocation(true)
                .overlappingTime(true)
                .warning("Trung phong va trung khung gio voi hoat dong da duyet hoac dang dien ra")
                .build();
    }

    private void ensureActivityStatusIn(Activity activity, Collection<ActivityStatus> allowedStatuses, String action) {
        if (!allowedStatuses.contains(activity.getStatus())) {
            log.warn("Invalid status transition for activityId={}: current={}, action={}",
                    activity.getId(), activity.getStatus(), action);
            throw new AppException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }
    }

    private void ensureOrganizerHasNoOverlappingActivity(Activity activity) {
        if (activity.getOrganizerId() == null || activity.getStartTime() == null || activity.getEndTime() == null) {
            return;
        }

        boolean hasConflict = activityRepository.existsOverlappingOrganizerActivity(
                activity.getOrganizerId(),
                activity.getId(),
                ORGANIZER_TIME_CONFLICT_STATUSES,
                activity.getStartTime(),
                activity.getEndTime());
        if (hasConflict) {
            throw new AppException(ErrorCode.ORGANIZER_ACTIVITY_TIME_CONFLICT);
        }
    }

    private void validateActivityTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new AppException(ErrorCode.ACTIVITY_INVALID_TIME_RANGE);
        }
    }

    private String normalizeLocation(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        return location.trim();
    }

    private void applyRoom(Activity activity, String roomId) {
        String normalizedRoomId = normalizeSearchValue(roomId);
        if (normalizedRoomId == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        Room room = roomRepository.findById(normalizedRoomId)
                .or(() -> roomRepository.findByCodeIgnoreCase(normalizedRoomId))
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        if (!"active".equalsIgnoreCase(room.getStatus())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        activity.setRoomId(room.getId());
        activity.setRoom(room);
        activity.setRoomCode(room.getCode());
        activity.setLocation(room.getCode());
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

    private String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return Path.of(fileName).getFileName().toString();
    }

    private boolean isExcelFile(String fileName) {
        String normalized = fileName.toLowerCase();
        return normalized.endsWith(".xls") || normalized.endsWith(".xlsx");
    }

    private String getExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return fileName.substring(index).toLowerCase();
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
