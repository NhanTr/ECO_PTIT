package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.ActivityCreateRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.service.ActivityService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ActivityController {
    
    ActivityService activityService;
    
    /**
     * Create a new activity
     * POST /api/v1/activities
     * Only ORGANIZER and ADMIN can create activities
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER')")
    public ResponseEntity<ActivityResponse> createActivity(@Valid @RequestBody ActivityCreateRequest request) {
        log.info("Create activity request received for title: {}", request.getTitle());
        ActivityResponse response = activityService.createActivity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get all activities with pagination
     * GET /api/v1/activities?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping
    public APIResponse<Page<ActivityResponse>> getAllActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size,
            Pageable pageable) {
        log.info("Get all activities request received - page: {}, size: {}", page, size);
        Page<ActivityResponse> activities = activityService.getAllActivities(pageable);
        return APIResponse.<Page<ActivityResponse>>builder()
                .result(activities)
                .build();
    }
    
    /**
     * Get activity by ID
     * GET /api/v1/activities/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ActivityResponse> getActivityById(@PathVariable String id) {
        log.info("Get activity request received for ID: {}", id);
        ActivityResponse activity = activityService.getActivityById(id);
        return ResponseEntity.ok(activity);
    }
    
    /**
     * Get activities by organizer
     * GET /api/v1/activities/organizer/{organizerId}
     */
    @GetMapping("/organizer/{organizerId}")
    public APIResponse<List<ActivityResponse>> getActivityByOrganizer(@PathVariable String organizerId) {
        log.info("Get activities request received for organizer: {}", organizerId);
        List<ActivityResponse> activities = activityService.getActivityByOrganizer(organizerId);
        return APIResponse.<List<ActivityResponse>>builder()
                .result(activities)
                .build();
    }
    
    /**
     * Get activities by status
     * GET /api/v1/activities/status/{status}
     */
    @GetMapping("/status/{status}")
    public APIResponse<List<ActivityResponse>> getActivityByStatus(@PathVariable String status) {
        log.info("Get activities request received for status: {}", status);
        List<ActivityResponse> activities = activityService.getActivityByStatus(status);
        return APIResponse.<List<ActivityResponse>>builder()
                .result(activities)
                .build();
    }
}
