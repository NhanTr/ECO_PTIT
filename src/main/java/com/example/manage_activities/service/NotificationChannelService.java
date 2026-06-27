package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.NotificationChannelRequest;
import com.example.manage_activities.dto.response.NotificationChannelResponse;
import com.example.manage_activities.entity.NotificationChannel;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.NotificationChannelRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * QTHT #8 - Quản lý kênh gửi thông báo.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationChannelService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    NotificationChannelRepository notificationChannelRepository;

    @Transactional(readOnly = true)
    public List<NotificationChannelResponse> getAll() {
        return notificationChannelRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationChannelResponse create(NotificationChannelRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (notificationChannelRepository.existsByCode(code)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        NotificationChannel channel = NotificationChannel.builder()
                .id(generateId())
                .code(code)
                .name(request.getName().trim())
                .description(request.getDescription())
                .status(resolveStatus(request.getStatus()))
                .createdAt(LocalDateTime.now())
                .build();
        return toResponse(notificationChannelRepository.save(channel));
    }

    @Transactional
    public NotificationChannelResponse update(String id, NotificationChannelRequest request) {
        NotificationChannel channel = notificationChannelRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        String code = request.getCode().trim().toUpperCase();
        if (!channel.getCode().equals(code) && notificationChannelRepository.existsByCode(code)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        channel.setCode(code);
        channel.setName(request.getName().trim());
        if (request.getDescription() != null) {
            channel.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getStatus())) {
            channel.setStatus(resolveStatus(request.getStatus()));
        }
        channel.setUpdatedAt(LocalDateTime.now());
        return toResponse(notificationChannelRepository.save(channel));
    }

    @Transactional
    public void delete(String id) {
        NotificationChannel channel = notificationChannelRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        channel.setStatus(STATUS_INACTIVE);
        channel.setUpdatedAt(LocalDateTime.now());
        notificationChannelRepository.save(channel);
    }

    private String resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return STATUS_ACTIVE;
        }
        String s = status.trim().toUpperCase();
        if (!STATUS_ACTIVE.equals(s) && !STATUS_INACTIVE.equals(s)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return s;
    }

    private NotificationChannelResponse toResponse(NotificationChannel channel) {
        return NotificationChannelResponse.builder()
                .id(channel.getId())
                .code(channel.getCode())
                .name(channel.getName())
                .description(channel.getDescription())
                .status(channel.getStatus())
                .createdAt(channel.getCreatedAt())
                .updatedAt(channel.getUpdatedAt())
                .build();
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}