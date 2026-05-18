package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.NotificationBroadcastRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.NotificationBroadcastResponse;
import com.example.manage_activities.service.NotificationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Module 4 — QLHĐ_BM 1 manual broadcast notifications.
 */
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminNotificationController {

    NotificationService notificationService;

    /**
     * POST /api/admin/notifications/broadcast
     */
    @PostMapping("/broadcast")
    public APIResponse<NotificationBroadcastResponse> broadcast(
            @Valid @RequestBody NotificationBroadcastRequest request) {
        log.info("Broadcast notification title={}, roleId={}, className={}, department={}",
                request.getTitle(), request.getRoleId(), request.getClassName(), request.getDepartment());
        return APIResponse.<NotificationBroadcastResponse>builder()
                .message("Da gui thong bao hang loat")
                .result(notificationService.broadcastNotifications(request))
                .build();
    }
}
