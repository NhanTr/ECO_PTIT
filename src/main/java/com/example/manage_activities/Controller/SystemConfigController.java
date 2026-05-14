package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.SystemConfigRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.SystemConfigResponse;
import com.example.manage_activities.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/system-configs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemConfigController {

    SystemConfigService systemConfigService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public APIResponse<List<SystemConfigResponse>> getAllConfigs() {
        return APIResponse.<List<SystemConfigResponse>>builder()
                .result(systemConfigService.getAllConfigs())
                .build();
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public APIResponse<SystemConfigResponse> getConfig(@PathVariable String key) {
        return APIResponse.<SystemConfigResponse>builder()
                .result(systemConfigService.getConfig(key))
                .build();
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public APIResponse<SystemConfigResponse> updateConfig(
            @PathVariable String key,
            @Valid @RequestBody SystemConfigRequest request) {
        return APIResponse.<SystemConfigResponse>builder()
                .message("Da cap nhat cau hinh he thong")
                .result(systemConfigService.updateConfig(key, request))
                .build();
    }
}
