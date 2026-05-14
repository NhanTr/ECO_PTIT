package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.RegistrationResponse;
import com.example.manage_activities.dto.response.StudentActivityHistoryResponse;
import com.example.manage_activities.dto.response.StudentPointsResponse;
import com.example.manage_activities.service.RegistrationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/registrations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RegistrationController {

    RegistrationService registrationService;

    /**
     * Register for activity
     * POST /api/v1/registrations/{activityId}
     */
    @PostMapping("/{activityId}")
    @PreAuthorize("hasRole('STUDENT')")
    public APIResponse<RegistrationResponse> registerActivity(@PathVariable String activityId) {
        log.info("Register activity request received for activity: {}", activityId);
        RegistrationResponse registration = registrationService.registerActivity(activityId);
        return APIResponse.<RegistrationResponse>builder()
                .result(registration)
                .build();
    }

    /**
     * Unregister from activity
     * DELETE /api/v1/registrations/{activityId}
     */
    @DeleteMapping("/{activityId}")
    @PreAuthorize("hasRole('STUDENT')")
    public APIResponse<Void> unregisterActivity(@PathVariable String activityId) {
        log.info("Unregister activity request received for activity: {}", activityId);
        registrationService.unregisterActivity(activityId);
        return APIResponse.<Void>builder()
                .result(null)
                .build();
    }

    /**
     * Get user's registrations
     * GET /api/v1/registrations/my-registrations
     */
    @GetMapping("/my-registrations")
    public APIResponse<List<RegistrationResponse>> getUserRegistrations() {
        log.info("Get user registrations request received");
        List<RegistrationResponse> registrations = registrationService.getUserRegistrations();
        return APIResponse.<List<RegistrationResponse>>builder()
                .result(registrations)
                .build();
    }

    /**
     * Get current student's activity history.
     * GET /api/v1/registrations/my-history?year=2026&semester=1
     */
    @GetMapping("/my-history")
    @PreAuthorize("hasRole('STUDENT')")
    public APIResponse<List<StudentActivityHistoryResponse>> getMyActivityHistory(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester) {
        log.info("Get student activity history request received");
        return APIResponse.<List<StudentActivityHistoryResponse>>builder()
                .result(registrationService.getMyActivityHistory(year, semester))
                .build();
    }

    /**
     * Get current student's points.
     * GET /api/v1/registrations/my-points?year=2026&semester=1
     */
    @GetMapping("/my-points")
    @PreAuthorize("hasRole('STUDENT')")
    public APIResponse<StudentPointsResponse> getMyPoints(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester) {
        log.info("Get student points request received");
        return APIResponse.<StudentPointsResponse>builder()
                .result(registrationService.getMyPoints(year, semester))
                .build();
    }

    /**
     * Get activity registrations
     * GET /api/v1/registrations/activity/{activityId}
     */
    @GetMapping("/activity/{activityId}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN','MANAGER')")
    public APIResponse<List<RegistrationResponse>> getActivityRegistrations(@PathVariable String activityId) {
        log.info("Get activity registrations request received for activity: {}", activityId);
        List<RegistrationResponse> registrations = registrationService.getActivityRegistrations(activityId);
        return APIResponse.<List<RegistrationResponse>>builder()
                .result(registrations)
                .build();
    }

    /**
     * Get registration count for activity
     * GET /api/v1/registrations/activity/{activityId}/count
     */
    @GetMapping("/activity/{activityId}/count")
    public APIResponse<Long> getActivityRegistrationCount(@PathVariable String activityId) {
        log.info("Get activity registration count request received for activity: {}", activityId);
        Long count = registrationService.getActivityRegistrationCount(activityId);
        return APIResponse.<Long>builder()
                .result(count)
                .build();
    }
}