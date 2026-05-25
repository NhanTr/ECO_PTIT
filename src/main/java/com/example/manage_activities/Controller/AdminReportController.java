package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.RejectActivityRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.ActivityFileResponse;
import com.example.manage_activities.service.ActivityService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin / Manager report review endpoints for QLHĐ_QĐ 3.
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminReportController {

    ActivityService activityService;

    /**
     * Search post-activity reports.
     * GET /api/admin/reports?activityId=...&reportStatus=Pending
     */
    @GetMapping
    public APIResponse<List<ActivityFileResponse>> searchReports(
            @RequestParam(required = false) String activityId,
            @RequestParam(required = false) String reportStatus) {
        return APIResponse.<List<ActivityFileResponse>>builder()
                .result(activityService.searchReports(activityId, reportStatus))
                .build();
    }

    /**
     * Approve a post-activity report and finalize points.
     * PATCH /api/admin/reports/{reportId}/approve
     */
    @PatchMapping("/{reportId}/approve")
    public APIResponse<ActivityFileResponse> approveReport(@PathVariable String reportId) {
        log.info("Admin approve report: {}", reportId);
        return APIResponse.<ActivityFileResponse>builder()
                .message("Da duyet bao cao sau hoat dong")
                .result(activityService.approveReport(reportId))
                .build();
    }

    /**
     * Reject a post-activity report with reason.
     * PATCH /api/admin/reports/{reportId}/reject
     */
    @PatchMapping("/{reportId}/reject")
    public APIResponse<ActivityFileResponse> rejectReport(
            @PathVariable String reportId,
            @Valid @RequestBody RejectActivityRequest request) {
        log.info("Admin reject report: {}, reason: {}", reportId, request.getRejectReason());
        return APIResponse.<ActivityFileResponse>builder()
                .message("Da tu choi bao cao sau hoat dong")
                .result(activityService.rejectReport(reportId, request.getRejectReason()))
                .build();
    }
}
