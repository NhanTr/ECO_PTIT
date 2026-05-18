package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.RejectActivityRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.dto.response.ActivityReviewResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin / Manager activity approval and schedule control (Module 2 — QLHĐ).
 * Replaces legacy {@link ManagerActivityController} endpoints for lifecycle review.
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
     * Schedule conflicts for an activity (QLHĐ_QĐ 2).
     * GET /api/admin/activities/{id}/schedule-conflicts
     */
    @GetMapping("/{id}/schedule-conflicts")
    public APIResponse<List<ActivityScheduleConflictResponse>> getScheduleConflicts(@PathVariable String id) {
        return APIResponse.<List<ActivityScheduleConflictResponse>>builder()
                .result(activityService.getScheduleConflicts(id))
                .build();
    }

    /**
     * Approve activity proposal (QLHĐ_QĐ 1).
     * PUT /api/admin/activities/{id}/approve
     */
    @PutMapping("/{id}/approve")
    public APIResponse<ActivityReviewResponse> approveActivity(@PathVariable String id) {
        log.info("Admin approve activity: {}", id);
        return APIResponse.<ActivityReviewResponse>builder()
                .message("Hoat dong da duoc duyet")
                .result(activityService.approveActivityWithWarnings(id))
                .build();
    }

    /**
     * Reject activity proposal (QLHĐ_QĐ 1).
     * PUT /api/admin/activities/{id}/reject
     */
    @PutMapping("/{id}/reject")
    public APIResponse<ActivityResponse> rejectActivity(
            @PathVariable String id,
            @Valid @RequestBody RejectActivityRequest request) {
        log.info("Admin reject activity: {}, reason: {}", id, request.getRejectReason());
        return APIResponse.<ActivityResponse>builder()
                .message("Da tu choi hoat dong")
                .result(activityService.rejectActivity(id, request.getRejectReason()))
                .build();
    }

    /**
     * Approve club cancellation request.
     * PUT /api/admin/activities/{id}/approve-cancel
     */
    @PutMapping("/{id}/approve-cancel")
    public APIResponse<ActivityResponse> approveCancelRequest(@PathVariable String id) {
        log.info("Admin approve cancel request for activity: {}", id);
        return APIResponse.<ActivityResponse>builder()
                .message("Da duyet yeu cau huy hoat dong")
                .result(activityService.approveCancelRequest(id))
                .build();
    }
}
