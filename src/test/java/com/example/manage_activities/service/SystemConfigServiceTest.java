package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.SystemConfigBulkUpdateRequest;
import com.example.manage_activities.dto.request.SystemConfigRequest;
import com.example.manage_activities.dto.request.SystemConfigUpdateItem;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.SystemConfigRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemConfigServiceTest {

    private final SystemConfigRepository systemConfigRepository = mock(SystemConfigRepository.class);
    private final SystemLogService systemLogService = mock(SystemLogService.class);
    private final SystemConfigService systemConfigService =
            new SystemConfigService(systemConfigRepository, systemLogService);

    @Test
    void updateConfig_shouldRejectActivityLifecycleStatuses() {
        when(systemConfigRepository.existsById(SystemConfigService.DEFAULT_TRAINING_POINTS)).thenReturn(true);
        when(systemConfigRepository.existsById(SystemConfigService.REGISTRATION_CANCEL_DEADLINE_HOURS)).thenReturn(true);
        when(systemConfigRepository.existsById(SystemConfigService.SESSION_TIMEOUT_SECONDS)).thenReturn(true);
        when(systemConfigRepository.existsById(SystemConfigService.ACTIVITY_LIFECYCLE_STATUSES)).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> systemConfigService.updateConfig(
                        SystemConfigService.ACTIVITY_LIFECYCLE_STATUSES,
                        SystemConfigRequest.builder().value("Draft,Approved").build()));

        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
    }

    @Test
    void updateConfigs_shouldRejectActivityLifecycleStatusesInBulk() {
        when(systemConfigRepository.existsById(SystemConfigService.DEFAULT_TRAINING_POINTS)).thenReturn(true);
        when(systemConfigRepository.existsById(SystemConfigService.REGISTRATION_CANCEL_DEADLINE_HOURS)).thenReturn(true);
        when(systemConfigRepository.existsById(SystemConfigService.SESSION_TIMEOUT_SECONDS)).thenReturn(true);
        when(systemConfigRepository.existsById(SystemConfigService.ACTIVITY_LIFECYCLE_STATUSES)).thenReturn(true);

        SystemConfigBulkUpdateRequest request = SystemConfigBulkUpdateRequest.builder()
                .configs(List.of(SystemConfigUpdateItem.builder()
                        .key(SystemConfigService.ACTIVITY_LIFECYCLE_STATUSES)
                        .value("Draft,Approved")
                        .build()))
                .build();

        AppException exception = assertThrows(AppException.class,
                () -> systemConfigService.updateConfigs(request));

        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
    }
}
