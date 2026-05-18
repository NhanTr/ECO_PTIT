package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.SystemLogResponse;
import com.example.manage_activities.service.SystemLogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/system-logs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class SystemLogController {

    SystemLogService systemLogService;

    @GetMapping
    public APIResponse<Page<SystemLogResponse>> searchLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String tableAffected,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime,
            Pageable pageable) {
        String resolvedAction = actionType != null && !actionType.isBlank() ? actionType : action;
        return APIResponse.<Page<SystemLogResponse>>builder()
                .result(systemLogService.searchLogs(
                        userId, resolvedAction, tableAffected, fromTime, toTime, pageable))
                .build();
    }
}
