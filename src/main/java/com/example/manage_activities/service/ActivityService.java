package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.ActivityCreateRequest;
import com.example.manage_activities.dto.request.ActivityReportRequest;
import com.example.manage_activities.dto.request.ActivityUpdateRequest;
import com.example.manage_activities.dto.response.ActivityFileResponse;
import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.dto.response.ClubStatisticsResponse;
import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.entity.ActivityFile;
import com.example.manage_activities.enums.ActivityStatus;
import com.example.manage_activities.enums.RegistrationStatus;
import com.example.manage_activities.enums.ReportStatus;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.mapper.ActivityMapper;
import com.example.manage_activities.repository.ActivityFileRepository;
import com.example.manage_activities.repository.ActivityRepository;
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
import java.util.List;
import java.util.UUID;
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

        Activity savedActivity = activityRepository.save(activity);
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

    @Transactional
    public ActivityResponse updateActivity(String id, ActivityUpdateRequest request) {
        Activity activity = getActivityEntity(id);
        ensureCanManageActivity(activity);

        if (!EDITABLE_ACTIVITY_STATUSES.contains(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_CANNOT_EDIT);
        }

        applyUpdate(activity, request);
        return activityMapper.toDTO(activityRepository.save(activity));
    }

    @Transactional
    public void deleteActivity(String id) {
        Activity activity = getActivityEntity(id);
        ensureCanManageActivity(activity);

        if (!DELETABLE_ACTIVITY_STATUSES.contains(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_CANNOT_DELETE);
        }

        activityRepository.delete(activity);
    }

    @Transactional
    public ActivityResponse submitForReview(String id) {
        Activity activity = getActivityEntity(id);
        ensureCanManageActivity(activity);

        if (!ActivityStatus.DRAFT.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION);
        }

        activity.setStatus(ActivityStatus.PENDING);
        return activityMapper.toDTO(activityRepository.save(activity));
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
        return activityMapper.toDTO(activityRepository.save(activity));
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

        return toActivityFileResponse(activityFileRepository.save(report));
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
        return activityMapper.toDTO(savedActivity);
    }

    /**
     * Reject activity so it can be publicly available.
     */
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
        return activityMapper.toDTO(savedActivity);
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