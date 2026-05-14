package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.ActivityCreateRequest;
import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.enums.ActivityStatus;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.mapper.ActivityMapper;
import com.example.manage_activities.repository.ActivityRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ActivityService {
    
    ActivityRepository activityRepository;
    ActivityMapper activityMapper;
    
    /**
     * Create a new activity
     */
    public ActivityResponse createActivity(ActivityCreateRequest request) {
        log.info("Creating activity: {}", request.getTitle());
        
        String organizerId = SecurityContextHolder.getContext().getAuthentication().getName();
        
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

    private String normalizeSearchValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
    /**
     * Get activity by ID
     */
    public ActivityResponse getActivityById(String id) {
        log.info("Getting activity with ID: {}", id);
        
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ACTIVITY_NOT_FOUND));
        
        return activityMapper.toDTO(activity);
    }

    /**
     * Approve activity so it can be publicly available.
     */
    public ActivityResponse approveActivity(String id) {
        log.info("Approving activity with ID: {}", id);

        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ACTIVITY_NOT_FOUND));

        if (ActivityStatus.APPROVED.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_ALREADY_APPROVED);
        }

        String reviewerId = SecurityContextHolder.getContext().getAuthentication().getName();
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

        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ACTIVITY_NOT_FOUND));

        if (ActivityStatus.REJECTED.equals(activity.getStatus())) {
            throw new AppException(ErrorCode.ACTIVITY_ALREADY_REJECTED);
        }

        String reviewerId = SecurityContextHolder.getContext().getAuthentication().getName();
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

    private ActivityStatus parseActivityStatus(String status) {
        try {
            return ActivityStatus.from(status);
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    /**
     * Generate a unique ID for the activity
     */

    private String generateActivityId() {
        String id = UUID.randomUUID().toString().substring(0, 10);
        while (activityRepository.existsById(id)) {
            id = UUID.randomUUID().toString().substring(0, 10);
        }
        return id;
    }
}
