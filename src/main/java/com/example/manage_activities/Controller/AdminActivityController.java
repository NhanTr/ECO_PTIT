package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.AssignActivityRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.dto.response.ActivityScheduleConflictResponse;
import com.example.manage_activities.service.ActivityService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin activity search, schedule conflict check, and reviewer assignment.
 */
@RestController
@RequestMapping("/api/admin/activities")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminActivityController {

    ActivityService activityService;

    /**
     * Filter activities by lifecycle status, room, keyword, and time range.
     * GET /api/admin/activities?statuses=Pending&location=...&keyword=...&fromTime=...&toTime=...
     */
    @GetMapping
    public APIResponse<Page<ActivityResponse>> searchActivities(
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime,
            Pageable pageable) {
        log.info("Admin search activities statuses={}, location={}, keyword={}", statuses, location, keyword);
        return APIResponse.<Page<ActivityResponse>>builder()
                .result(activityService.searchActivitiesForManager(statuses, keyword, location, fromTime, toTime, pageable))
                .build();
    }

    /**
     * Schedule conflicts for an activity.
     * GET /api/admin/activities/{id}/schedule-conflicts
     */
    @GetMapping("/{id}/schedule-conflicts")
    public APIResponse<List<ActivityScheduleConflictResponse>> getScheduleConflicts(@PathVariable String id) {
        return APIResponse.<List<ActivityScheduleConflictResponse>>builder()
                .result(activityService.getScheduleConflicts(id))
                .build();
    }

    /**
     * Assign an activity to a manager/admin reviewer.
     * PUT /api/admin/activities/{id}/assign
     */
    @PutMapping("/{id}/assign")
    public APIResponse<ActivityResponse> assignActivity(
            @PathVariable String id,
            @Valid @RequestBody AssignActivityRequest request) {
        log.info("Admin assign activity: {}, reviewerId: {}", id, request.getReviewerId());
        return APIResponse.<ActivityResponse>builder()
                .message("Da phan cong nguoi phu trach hoat dong")
                .result(activityService.assignReviewer(id, request.getReviewerId()))
                .build();
    }
}
