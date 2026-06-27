package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.NotificationChannelRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.NotificationChannelResponse;
import com.example.manage_activities.service.NotificationChannelService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * QTHT #8 - Quản lý kênh gửi thông báo (QTHT_QĐ 4).
 */
@RestController
@RequestMapping("/api/admin/notification-channels")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class NotificationChannelController {

    NotificationChannelService service;

    @GetMapping
    public APIResponse<List<NotificationChannelResponse>> list() {
        return APIResponse.<List<NotificationChannelResponse>>builder()
                .result(service.getAll())
                .build();
    }

    @PostMapping
    public APIResponse<NotificationChannelResponse> create(@Valid @RequestBody NotificationChannelRequest request) {
        return APIResponse.<NotificationChannelResponse>builder()
                .message("Đã tạo kênh thông báo")
                .result(service.create(request))
                .build();
    }

    @PutMapping("/{id}")
    public APIResponse<NotificationChannelResponse> update(
            @PathVariable String id,
            @Valid @RequestBody NotificationChannelRequest request) {
        return APIResponse.<NotificationChannelResponse>builder()
                .message("Đã cập nhật kênh thông báo")
                .result(service.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return APIResponse.<Void>builder()
                .message("Đã vô hiệu hóa kênh thông báo")
                .build();
    }
}