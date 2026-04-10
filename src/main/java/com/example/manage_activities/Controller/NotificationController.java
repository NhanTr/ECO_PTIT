package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.NotificationCreateRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.service.NotificationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationController {

    NotificationService notificationService;

    /**
     * Send update notifications to student groups.
     * POST /api/notifications
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<APIResponse<Integer>> sendNotifications(
            @Valid @RequestBody NotificationCreateRequest request) {
        log.info("Send notifications request received, type: {}", request.getType());

        int sentCount = notificationService.sendNotificationsToStudents(request);
        return ResponseEntity.ok(APIResponse.<Integer>builder()
                .code(1000)
                .message("Gui thong bao thanh cong")
                .result(sentCount)
                .build());
    }

}
