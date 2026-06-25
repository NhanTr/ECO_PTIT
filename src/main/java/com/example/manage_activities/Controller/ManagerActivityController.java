package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.RejectActivityRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.ActivityFileResponse;
import com.example.manage_activities.dto.response.ActivityReviewResponse;
import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.dto.response.ActivityScheduleConflictResponse;
import com.example.manage_activities.dto.response.ManagerActivityStatisticsResponse;
import com.example.manage_activities.service.ActivityService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @deprecated Prefer {@link AdminActivityController} at {@code /api/admin/activities} for
 * search, approve, reject, cancel approval, and schedule conflict checks (Module 2).
 * Report and statistics endpoints remain here until migrated.
 */
@Deprecated
@RestController
@RequestMapping("/api/manager/activities")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ManagerActivityController {

    ActivityService activityService;

    /**
     * Search/filter activities by lifecycle status.
     * GET /api/manager/activities?statuses=Pending&statuses=Approved&keyword=...
     * @deprecated Use {@link AdminActivityController#searchActivities} — GET /api/admin/activities
     */
    @Deprecated
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public APIResponse<Page<ActivityResponse>> searchActivities(
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime,
            Pageable pageable) {
        return APIResponse.<Page<ActivityResponse>>builder()
                .result(activityService.searchActivitiesForManager(statuses, keyword, location, fromTime, toTime, pageable))
                .build();
    }

    /**
     * Approve activity to be publicly available.
     * Patch /api/manager/activities/{id}/approve
     * @deprecated Use PUT /api/admin/activities/{id}/approve
     */
    @Deprecated
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<APIResponse<ActivityReviewResponse>> approveActivity(@PathVariable String id) {
        log.info("Approve activity request received for ID: {}", id);
        ActivityReviewResponse result = activityService.approveActivityWithWarnings(id);
        return ResponseEntity.ok(APIResponse.<ActivityReviewResponse>builder()
                .code(1000)
                .message("Hoat dong da duoc duyet")
                .result(result)
                .build());
    }

    /**
     * Reject activity.
     * Patch /api/manager/activities/{id}/reject
     * @deprecated Use PUT /api/admin/activities/{id}/reject
     */
    @Deprecated
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<APIResponse<Void>> rejectActivity(
            @PathVariable String id,
            @Valid @RequestBody RejectActivityRequest request) {
        log.info("Reject activity request received for ID: {}, reason: {}", id, request.getRejectReason());
        activityService.rejectActivity(id, request.getRejectReason());
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .code(1000)
                .message("Da tu choi")
                .build());
    }

    /**
     * Approve cancellation request.
     * PATCH /api/manager/activities/{id}/cancel-requests/approve
     * @deprecated Use PUT /api/admin/activities/{id}/approve-cancel
     */
    @Deprecated
    @PatchMapping("/{id}/cancel-requests/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<APIResponse<ActivityResponse>> approveCancelRequest(@PathVariable String id) {
        return ResponseEntity.ok(APIResponse.<ActivityResponse>builder()
                .code(1000)
                .message("Da duyet yeu cau huy hoat dong")
                .result(activityService.approveCancelRequest(id))
                .build());
    }

    /**
     * Reject cancellation request.
     * PATCH /api/manager/activities/{id}/cancel-requests/reject
     */
    @PatchMapping("/{id}/cancel-requests/reject")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<APIResponse<ActivityResponse>> rejectCancelRequest(
            @PathVariable String id,
            @Valid @RequestBody RejectActivityRequest request) {
        return ResponseEntity.ok(APIResponse.<ActivityResponse>builder()
                .code(1000)
                .message("Da tu choi yeu cau huy hoat dong")
                .result(activityService.rejectCancelRequest(id, request.getReason()))
                .build());
    }

    /**
     * Check room/time conflicts against approved activities.
     * GET /api/manager/activities/{id}/schedule-conflicts
     * @deprecated Use GET /api/admin/activities/{id}/schedule-conflicts
     */
    @Deprecated
    @GetMapping("/{id}/schedule-conflicts")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public APIResponse<List<ActivityScheduleConflictResponse>> getScheduleConflicts(@PathVariable String id) {
        return APIResponse.<List<ActivityScheduleConflictResponse>>builder()
                .result(activityService.getScheduleConflicts(id))
                .build();
    }

    /**
     * Manager statistics.
     * GET /api/manager/activities/statistics?year=2026&semester=1
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public APIResponse<ManagerActivityStatisticsResponse> getStatistics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester) {
        return APIResponse.<ManagerActivityStatisticsResponse>builder()
                .result(activityService.getManagerStatistics(year, semester))
                .build();
    }

    /**
     * List post-activity reports.
     * GET /api/manager/activities/reports?activityId=...&reportStatus=Pending
     */
    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public APIResponse<List<ActivityFileResponse>> searchReports(
            @RequestParam(required = false) String activityId,
            @RequestParam(required = false) String reportStatus) {
        return APIResponse.<List<ActivityFileResponse>>builder()
                .result(activityService.searchReports(activityId, reportStatus))
                .build();
    }

    /**
     * Approve a post-activity report and lock points.
     * PATCH /api/manager/activities/reports/{reportId}/approve
     */
    @PatchMapping("/reports/{reportId}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<APIResponse<ActivityFileResponse>> approveReport(@PathVariable String reportId) {
        return ResponseEntity.ok(APIResponse.<ActivityFileResponse>builder()
                .code(1000)
                .message("Da duyet bao cao sau hoat dong")
                .result(activityService.approveReport(reportId))
                .build());
    }

    /**
     * Reject a post-activity report so organizer can resubmit.
     * PATCH /api/manager/activities/reports/{reportId}/reject
     */
    @PatchMapping("/reports/{reportId}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<APIResponse<ActivityFileResponse>> rejectReport(
            @PathVariable String reportId,
            @Valid @RequestBody RejectActivityRequest request) {
        return ResponseEntity.ok(APIResponse.<ActivityFileResponse>builder()
                .code(1000)
                .message("Da tu choi bao cao sau hoat dong")
                .result(activityService.rejectReport(reportId, request.getReason()))
                .build());
    }
}

