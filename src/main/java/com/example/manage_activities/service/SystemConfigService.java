package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.SystemConfigRequest;
import com.example.manage_activities.dto.response.SystemConfigResponse;
import com.example.manage_activities.entity.SystemConfig;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.SystemConfigRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemConfigService {

    public static final String DEFAULT_TRAINING_POINTS = "DEFAULT_TRAINING_POINTS";
    public static final String REGISTRATION_CANCEL_DEADLINE_HOURS = "REGISTRATION_CANCEL_DEADLINE_HOURS";
    public static final String SESSION_TIMEOUT_SECONDS = "SESSION_TIMEOUT_SECONDS";
    public static final String ACTIVITY_LIFECYCLE_STATUSES = "ACTIVITY_LIFECYCLE_STATUSES";

    private static final Map<String, SystemConfig> DEFAULT_CONFIGS = Map.of(
            DEFAULT_TRAINING_POINTS, defaultConfig(DEFAULT_TRAINING_POINTS, "5", "INTEGER", "Default points for activities without explicit points"),
            REGISTRATION_CANCEL_DEADLINE_HOURS, defaultConfig(REGISTRATION_CANCEL_DEADLINE_HOURS, "24", "INTEGER", "Hours before start time that students can cancel registration"),
            SESSION_TIMEOUT_SECONDS, defaultConfig(SESSION_TIMEOUT_SECONDS, "3600", "INTEGER", "Session timeout in seconds"),
            ACTIVITY_LIFECYCLE_STATUSES, defaultConfig(ACTIVITY_LIFECYCLE_STATUSES, "Draft,Pending,Reviewing,Approved,Ongoing,Closed,Rejected,Cancelled", "STRING", "Configurable activity lifecycle status labels")
    );

    SystemConfigRepository systemConfigRepository;
    SystemLogService systemLogService;

    @Transactional
    public List<SystemConfigResponse> getAllConfigs() {
        ensureDefaults();
        return systemConfigRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SystemConfigResponse getConfig(String key) {
        ensureDefaults();
        return systemConfigRepository.findById(key)
                .map(this::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
    }

    @Transactional
    public SystemConfigResponse updateConfig(String key, SystemConfigRequest request) {
        ensureDefaults();
        validateValue(key, request.getValue());
        SystemConfig config = systemConfigRepository.findById(key)
                .orElseGet(() -> SystemConfig.builder().key(key).build());
        String oldValue = config.getValue();

        config.setValue(request.getValue());
        if (request.getValueType() != null) config.setValueType(request.getValueType());
        if (request.getDescription() != null) config.setDescription(request.getDescription());
        config.setUpdatedBy(getCurrentUserId());
        config.setUpdatedAt(LocalDateTime.now());

        SystemConfig savedConfig = systemConfigRepository.save(config);
        systemLogService.logAction(
                getCurrentUserId(),
                "UPDATE_SYSTEM_CONFIG",
                "system_configs",
                key + "=" + oldValue,
                key + "=" + savedConfig.getValue());
        return toResponse(savedConfig);
    }

    public int getIntValue(String key, int fallbackValue) {
        return systemConfigRepository.findById(key)
                .map(SystemConfig::getValue)
                .map(value -> parseInt(value, fallbackValue))
                .orElse(fallbackValue);
    }

    private void ensureDefaults() {
        DEFAULT_CONFIGS.forEach((key, config) -> {
            if (!systemConfigRepository.existsById(key)) {
                systemConfigRepository.save(config);
            }
        });
    }

    private void validateValue(String key, String value) {
        if (DEFAULT_TRAINING_POINTS.equals(key) || REGISTRATION_CANCEL_DEADLINE_HOURS.equals(key)
                || SESSION_TIMEOUT_SECONDS.equals(key)) {
            int parsedValue = parseInt(value, -1);
            if (parsedValue < 0) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }
    }

    private int parseInt(String value, int fallbackValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallbackValue;
        }
    }

    private SystemConfigResponse toResponse(SystemConfig config) {
        return SystemConfigResponse.builder()
                .key(config.getKey())
                .value(config.getValue())
                .valueType(config.getValueType())
                .description(config.getDescription())
                .updatedBy(config.getUpdatedBy())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private static SystemConfig defaultConfig(String key, String value, String valueType, String description) {
        return SystemConfig.builder()
                .key(key)
                .value(value)
                .valueType(valueType)
                .description(description)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
