package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.RejectRegistrationRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/registrations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ManagerRegistrationController {

    RegistrationService registrationService;


    /**
     * Reject one student's registration in an activity.
     * PATCH /api/manager/registrations/{activityId}/students/{studentId}/reject
     */
    @PatchMapping("/{activityId}/students/{studentId}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','ORGANIZER')")
    public ResponseEntity<APIResponse<Void>> rejectRegistration(
            @PathVariable String activityId,
            @PathVariable String studentId,
            @Valid @RequestBody RejectRegistrationRequest request) {
        log.info("Reject request received for activityId: {}, studentId: {}, reason: {}",
                activityId, studentId, request.getReason());
        registrationService.rejectRegistration(activityId, studentId, request.getReason());
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .code(1000)
                .message("Da tu choi dang ky")
                .build());
    }
}


