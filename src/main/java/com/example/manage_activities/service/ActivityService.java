package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.ActivityCreateRequest;
import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.mapper.ActivityMapper;
import com.example.manage_activities.repository.ActivityRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
     * Get all activities
     */
    public List<ActivityResponse> getAllActivities() {
        log.info("Getting all activities");
        
        return activityRepository.findAll()
                .stream()
                .map(activityMapper::toDTO)
                .collect(Collectors.toList());
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
        
        return activityRepository.findByStatus(status)
                .stream()
                .map(activityMapper::toDTO)
                .collect(Collectors.toList());
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
