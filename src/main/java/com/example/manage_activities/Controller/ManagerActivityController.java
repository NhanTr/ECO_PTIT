package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.RejectActivityRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.service.ActivityService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/activities")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ManagerActivityController {

    ActivityService activityService;

    /**
     * Approve activity to be publicly available.
     * Patch /api/manager/activities/{id}/approve
     */
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<APIResponse<Void>> approveActivity(@PathVariable String id) {
        log.info("Approve activity request received for ID: {}", id);
        activityService.approveActivity(id);
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .code(1000)
                .message("Hoat dong da duoc duyet")
                .build());
    }

    /**
     * Reject activity.
     * Patch /api/manager/activities/{id}/reject
     */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<APIResponse<Void>> rejectActivity(
            @PathVariable String id,
            @Valid @RequestBody RejectActivityRequest request) {
        log.info("Reject activity request received for ID: {}, reason: {}", id, request.getReason());
        activityService.rejectActivity(id, request.getReason());
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .code(1000)
                .message("Da tu choi")
                .build());
    }
}

