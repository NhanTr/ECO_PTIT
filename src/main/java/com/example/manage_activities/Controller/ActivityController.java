package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.ActivityCreateRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.dto.response.PageMode;
import com.example.manage_activities.service.ActivityService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ActivityResponse> createActivity(@Valid @RequestBody ActivityCreateRequest request) {
        log.info("Create activity request received for title: {}", request.getTitle());
        ActivityResponse response = activityService.createActivity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get all activities with optional filters and pagination
     * GET /api/v1/activities?page=0&size=10&status=Active&sponsor=ABC&startTime=2024-01-01&endTime=2024-12-31&location=HCM&sort=createdAt,desc
     * 
     * Query Parameters (all optional):
     * - status: Filter by activity status (Draft, Pending, Approved, etc.)
     * - sponsor: Filter by sponsor name
     * - startTime: Filter activities starting from this date (format: yyyy-MM-dd)
     * - endTime: Filter activities ending before this date (format: yyyy-MM-dd)
     * - location: Filter by location name
     * - page: Page number (default: 0)
     * - size: Page size (default: 10)
     * - sort: Sort criteria (example: createdAt,desc)
     */
    @GetMapping
    public APIResponse<PageMode<ActivityResponse>> getAllActivities(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sponsor,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String location,
            Pageable pageable) {
        log.info("Get activities with filters - status: {}, sponsor: {}, startTime: {}, endTime: {}, location: {}", 
                status, sponsor, startTime, endTime, location);
        PageMode<ActivityResponse> activities = activityService.getAllActivities(status, sponsor, startTime, endTime, location, pageable);
        return APIResponse.<PageMode<ActivityResponse>>builder()
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
}
