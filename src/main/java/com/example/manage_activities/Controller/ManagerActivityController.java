package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.service.ActivityService;
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
     * POST /api/manager/activities/{id}/approve
     */
    @PatchMapping ("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ActivityResponse> approveActivity(@PathVariable String id) {
        log.info("Approve activity request received for ID: {}", id);
        ActivityResponse activity = activityService.approveActivity(id);
        return ResponseEntity.ok(activity);
    }
}

