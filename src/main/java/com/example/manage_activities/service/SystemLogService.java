package com.example.manage_activities.service;

import com.example.manage_activities.dto.response.SystemLogResponse;
import com.example.manage_activities.entity.SystemLog;
import com.example.manage_activities.repository.SystemLogRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemLogService {

    SystemLogRepository systemLogRepository;

    public Page<SystemLogResponse> searchLogs(
            String userId,
            String action,
            String tableAffected,
            LocalDateTime fromTime,
            LocalDateTime toTime,
            Pageable pageable) {
        return systemLogRepository.searchLogs(
                        normalize(userId),
                        normalize(action),
                        normalize(tableAffected),
                        fromTime,
                        toTime,
                        pageable)
                .map(this::toResponse);
    }

    public void logAction(String userId, String action, String tableAffected, String oldValue, String newValue) {
        SystemLog systemLog = SystemLog.builder()
                .id(generateSystemLogId())
                .userId(userId)
                .action(action)
                .tableAffected(tableAffected)
                .oldValue(oldValue)
                .newValue(newValue)
                .createdAt(LocalDateTime.now())
                .build();

        systemLogRepository.save(systemLog);
    }

    private SystemLogResponse toResponse(SystemLog systemLog) {
        return SystemLogResponse.builder()
                .id(systemLog.getId())
                .userId(systemLog.getUserId())
                .action(systemLog.getAction())
                .tableAffected(systemLog.getTableAffected())
                .oldValue(systemLog.getOldValue())
                .newValue(systemLog.getNewValue())
                .createdAt(systemLog.getCreatedAt())
                .build();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String generateSystemLogId() {
        String id = UUID.randomUUID().toString().substring(0, 10);
        while (systemLogRepository.existsById(id)) {
            id = UUID.randomUUID().toString().substring(0, 10);
        }
        return id;
    }
}
