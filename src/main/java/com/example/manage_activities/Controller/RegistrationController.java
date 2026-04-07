package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.RegistrationRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.RegistrationResponse;
import com.example.manage_activities.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;



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
    public APIResponse<RegistrationResponse> registerActivity(@PathVariable String activityId) {
        log.info("Register activity request received for activity: {}", activityId);
        registrationService.registerActivity(activityId);
        return APIResponse.<RegistrationResponse>builder()
                .result(null)
                .build();
    }
    
    /**
     * Unregister from activity
     * DELETE /api/v1/registrations/{activityId}
     */
    @DeleteMapping("/{activityId}")
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
     * Get activity registrations
     * GET /api/v1/registrations/activity/{activityId}
     */
    @GetMapping("/activity/{activityId}")
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
