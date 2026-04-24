package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.ActivityCreateRequest;
import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.dto.response.PageMode;
import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.mapper.ActivityMapper;
import com.example.manage_activities.repository.ActivityRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
        activity.setStatus("Draft");
        activity.setCreatedAt(LocalDateTime.now());


        
        Activity savedActivity = activityRepository.save(activity);
        log.info("Activity created successfully with ID: {}", savedActivity.getId());
        return activityMapper.toDTO(savedActivity);
    }
    
    /**
     * Get activity by ID
     */
    public ActivityResponse getActivityById(String id) {
        log.info("Getting activity with ID: {}", id);
        
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found with ID: " + id));
        
        return activityMapper.toDTO(activity);
    }
    
    /**
     * Get all activities with optional filters and pagination
     * Supports filtering by status, sponsor, startTime, endTime, and location
     */
    public PageMode<ActivityResponse> getAllActivities(String status, String sponsor, String startTime, String endTime, String location, Pageable pageable) {
        log.info("Getting activities with filters - status: {}, sponsor: {}, startTime: {}, endTime: {}, location: {}", 
                status, sponsor, startTime, endTime, location);
        
        // Build dynamic specification based on provided filters
        Specification<Activity> spec = Specification.where((root, query, cb) -> cb.conjunction());
        
        if (status != null && !status.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
            log.info("Applied filter: status = {}", status);
        }
        
        if (sponsor != null && !sponsor.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("sponsor")), "%" + sponsor.toLowerCase() + "%"));
            log.info("Applied filter: sponsor contains {}", sponsor);
        }
        
        if (location != null && !location.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%"));
            log.info("Applied filter: location contains {}", location);
        }
        
        if (startTime != null && !startTime.trim().isEmpty()) {
            try {
                LocalDateTime startDateTime = parseDateTimeFilter(startTime, false);
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startTime"), startDateTime));
                log.info("Applied filter: startTime >= {}", startDateTime);
            } catch (DateTimeParseException e) {
                log.warn("Invalid startTime format: {}", startTime);
            }
        }

        if (endTime != null && !endTime.trim().isEmpty()) {
            try {
                LocalDateTime endDateTime = parseDateTimeFilter(endTime, true);
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("endTime"), endDateTime));
                log.info("Applied filter: endTime <= {}", endDateTime);
            } catch (DateTimeParseException e) {
                log.warn("Invalid endTime format: {}", endTime);
            }
        }
        
        Page<Activity> activities = activityRepository.findAll(spec, pageable);
        
        // Convert Page<Activity> to PageMode<ActivityResponse>
        return PageMode.<ActivityResponse>builder()
                .content(activities.getContent().stream().map(activityMapper::toDTO).collect(Collectors.toList()))
                .pageNumber(activities.getNumber())
                .pageSize(activities.getSize())
                .totalElements(activities.getTotalElements())
                .totalPages(activities.getTotalPages())
                .isFirst(activities.isFirst())
                .isLast(activities.isLast())
                .isEmpty(activities.isEmpty())
                .build();
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

    /**
     * Parse filter value as either yyyy-MM-dd or ISO local date-time (e.g. yyyy-MM-ddTHH:mm[:ss]).
     */
    private LocalDateTime parseDateTimeFilter(String value, boolean endBoundary) {
        String trimmedValue = value.trim();

        if (trimmedValue.contains("T")) {
            return LocalDateTime.parse(trimmedValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        LocalDate date = LocalDate.parse(trimmedValue, DateTimeFormatter.ISO_LOCAL_DATE);
        return endBoundary ? date.atTime(23, 59, 59) : date.atStartOfDay();
    }
}
