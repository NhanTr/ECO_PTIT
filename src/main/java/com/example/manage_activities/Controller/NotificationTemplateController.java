package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.NotificationTemplateRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.NotificationTemplateResponse;
import com.example.manage_activities.service.NotificationTemplateService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * QTHT #8 - Quản lý template thông báo.
 */
@RestController
@RequestMapping("/api/admin/notification-templates")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class NotificationTemplateController {

    NotificationTemplateService service;

    @GetMapping
    public APIResponse<List<NotificationTemplateResponse>> list(
            @RequestParam(required = false) String channelCode) {
        return APIResponse.<List<NotificationTemplateResponse>>builder()
                .result(service.getAll(channelCode))
                .build();
    }

    @PostMapping
    public APIResponse<NotificationTemplateResponse> create(@Valid @RequestBody NotificationTemplateRequest request) {
        return APIResponse.<NotificationTemplateResponse>builder()
                .message("Đã tạo template")
                .result(service.create(request))
                .build();
    }

    @PutMapping("/{id}")
    public APIResponse<NotificationTemplateResponse> update(
            @PathVariable String id,
            @Valid @RequestBody NotificationTemplateRequest request) {
        return APIResponse.<NotificationTemplateResponse>builder()
                .message("Đã cập nhật template")
                .result(service.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return APIResponse.<Void>builder()
                .message("Đã vô hiệu hóa template")
                .build();
    }
}