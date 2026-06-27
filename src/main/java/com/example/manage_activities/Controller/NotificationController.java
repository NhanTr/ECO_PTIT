package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.NotificationCreateRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.NotificationResponse;
import com.example.manage_activities.service.NotificationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationController {

    NotificationService notificationService;

    /**
     * Get current user's notifications.
     * GET /api/notifications
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<APIResponse<List<NotificationResponse>>> getMyNotifications() {
        log.info("Get notifications request received for current user");

        List<NotificationResponse> notifications = notificationService.getMyNotifications();
        return ResponseEntity.ok(APIResponse.<List<NotificationResponse>>builder()
                .code(1000)
                .message("Lấy danh sách thông báo thành công")
                .result(notifications)
                .build());
    }

    /**
     * Get notifications sent by current user.
     * GET /api/notifications/sent
     */
    @GetMapping("/sent")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ORGANIZER')")
    public ResponseEntity<APIResponse<List<NotificationResponse>>> getSentNotifications() {
        List<NotificationResponse> notifications = notificationService.getSentNotifications();
        return ResponseEntity.ok(APIResponse.<List<NotificationResponse>>builder()
                .code(1000)
                .message("Lấy danh sách thông báo đã gửi thành công")
                .result(notifications)
                .build());
    }

    /**
     * Get notification detail for receiver or sender.
     * GET /api/notifications/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<APIResponse<NotificationResponse>> getNotificationDetail(@PathVariable String id) {
        NotificationResponse notification = notificationService.getNotificationDetail(id);
        return ResponseEntity.ok(APIResponse.<NotificationResponse>builder()
                .code(1000)
                .message("Lấy chi tiết thông báo thành công")
                .result(notification)
                .build());
    }

    /**
     * Mark current user's notification as read.
     * PATCH /api/notifications/{id}/read
     */
    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<APIResponse<NotificationResponse>> markAsRead(@PathVariable String id) {
        NotificationResponse notification = notificationService.markNotificationReadStatus(id, true);
        return ResponseEntity.ok(APIResponse.<NotificationResponse>builder()
                .code(1000)
                .message("Đánh dấu thông báo đã đọc thành công")
                .result(notification)
                .build());
    }

    /**
     * Mark current user's notification as unread.
     * PATCH /api/notifications/{id}/unread
     */
    @PatchMapping("/{id}/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<APIResponse<NotificationResponse>> markAsUnread(@PathVariable String id) {
        NotificationResponse notification = notificationService.markNotificationReadStatus(id, false);
        return ResponseEntity.ok(APIResponse.<NotificationResponse>builder()
                .code(1000)
                .message("Đánh dấu thông báo chưa đọc thành công")
                .result(notification)
                .build());
    }

    /**
     * Send notifications to users (all users for System type or empty recipient list).
     * POST /api/notifications
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ORGANIZER')")
    public ResponseEntity<APIResponse<Integer>> sendNotifications(
            @Valid @RequestBody NotificationCreateRequest request) {
        log.info("Send notifications request received, type: {}", request.getType());

        int sentCount = notificationService.sendNotifications(request);
        return ResponseEntity.ok(APIResponse.<Integer>builder()
                .code(1000)
                .message("Gửi thông báo thành công")
                .result(sentCount)
                .build());
    }
}
