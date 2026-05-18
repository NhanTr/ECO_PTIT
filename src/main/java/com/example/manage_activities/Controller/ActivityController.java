package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.ActivityCreateRequest;
import com.example.manage_activities.dto.request.ActivityReportRequest;
import com.example.manage_activities.dto.request.ActivityUpdateRequest;
import com.example.manage_activities.dto.request.CancelActivityRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.ActivityFileResponse;
import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.dto.response.ActivityReviewResponse;
import com.example.manage_activities.dto.response.ClubStatisticsResponse;
import com.example.manage_activities.service.ActivityService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
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
     * Only ORGANIZER can create activities
     */
    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ActivityResponse> createActivity(@Valid @RequestBody ActivityCreateRequest request) {
        log.info("Create activity request received for title: {}", request.getTitle());
        ActivityResponse response = activityService.createActivity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update activity.
     * PUT /api/v1/activities/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable String id,
            @Valid @RequestBody ActivityUpdateRequest request) {
        log.info("Update activity request received for ID: {}", id);
        return ResponseEntity.ok(activityService.updateActivity(id, request));
    }

    /**
     * Delete activity.
     * DELETE /api/v1/activities/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<APIResponse<Void>> deleteActivity(@PathVariable String id) {
        log.info("Delete activity request received for ID: {}", id);
        activityService.deleteActivity(id);
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .code(1000)
                .message("Da xoa hoat dong")
                .build());
    }

    /**
     * Submit draft activity for review.
     * PATCH /api/v1/activities/{id}/submit
     */
    @PatchMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<ActivityReviewResponse> submitForReview(@PathVariable String id) {
        log.info("Submit activity for review request received for ID: {}", id);
        return ResponseEntity.ok(activityService.submitForReview(id));
    }

    /**
     * Request cancellation for an approved activity.
     * PATCH /api/v1/activities/{id}/cancel-request
     */
    @PatchMapping("/{id}/cancel-request")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<ActivityResponse> requestCancelActivity(
            @PathVariable String id,
            @Valid @RequestBody CancelActivityRequest request) {
        log.info("Cancel activity request received for ID: {}", id);
        return ResponseEntity.ok(activityService.requestCancelActivity(id, request.getReason()));
    }

    /**
     * Submit activity report after the activity is closed.
     * POST /api/v1/activities/{id}/reports
     */
    @PostMapping("/{id}/reports")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<ActivityFileResponse> submitReport(
            @PathVariable String id,
            @Valid @RequestBody ActivityReportRequest request) {
        log.info("Submit report request received for activity ID: {}", id);
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.submitReport(id, request));
    }

    /**
     * Get internal club statistics.
     * GET /api/v1/activities/my-club/statistics?year=2026&semester=1
     */
    @GetMapping("/my-club/statistics")
    @PreAuthorize("hasRole('ORGANIZER')")
    public APIResponse<ClubStatisticsResponse> getMyClubStatistics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester) {
        return APIResponse.<ClubStatisticsResponse>builder()
                .result(activityService.getMyClubStatistics(year, semester))
                .build();
    }

    /**
     * Get all activities with pagination
     * GET /api/v1/activities?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping
    public APIResponse<Page<ActivityResponse>> getAllActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sponsor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String location,
            Pageable pageable) {
        log.info("Get all activities request received - page: {}, size: {}", page, size);
        Page<ActivityResponse> activities = activityService.searchActivities(
                status, sponsor, startTime, endTime, location, pageable);
        return APIResponse.<Page<ActivityResponse>>builder()
                .result(activities)
                .build();
    }

    /**
     * Get activities visible to students.
     * GET /api/v1/activities/available?keyword=&location=&fromTime=&toTime=
     */
    @GetMapping("/available")
    public APIResponse<Page<ActivityResponse>> getAvailableActivities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime,
            Pageable pageable) {
        log.info("Get available activities request received");
        Page<ActivityResponse> activities = activityService.getAvailableActivities(
                keyword, location, fromTime, toTime, pageable);
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
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public APIResponse<List<ActivityResponse>> getActivityByStatus(@PathVariable String status) {
        log.info("Get activities request received for status: {}", status);
        List<ActivityResponse> activities = activityService.getActivityByStatus(status);
        return APIResponse.<List<ActivityResponse>>builder()
                .result(activities)
                .build();
    }
}
