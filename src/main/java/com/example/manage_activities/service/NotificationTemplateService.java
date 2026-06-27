package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.NotificationTemplateRequest;
import com.example.manage_activities.dto.response.NotificationTemplateResponse;
import com.example.manage_activities.entity.NotificationTemplate;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.NotificationTemplateRepository;
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
 * QTHT #8 - Quản lý template thông báo.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationTemplateService {

    NotificationTemplateRepository templateRepository;
    NotificationChannelService notificationChannelService;

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getAll(String channelCode) {
        List<NotificationTemplate> templates = StringUtils.hasText(channelCode)
                ? templateRepository.findByChannelCode(channelCode.toUpperCase())
                : templateRepository.findAll();
        return templates.stream().map(this::toResponse).toList();
    }

    @Transactional
    public NotificationTemplateResponse create(NotificationTemplateRequest request) {
        String channelCode = request.getChannelCode().trim().toUpperCase();
        String code = request.getCode().trim().toUpperCase();
        if (templateRepository.existsByChannelCodeAndCode(channelCode, code)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        NotificationTemplate template = NotificationTemplate.builder()
                .id(generateId())
                .channelCode(channelCode)
                .code(code)
                .subject(request.getSubject().trim())
                .body(request.getBody())
                .status(resolveStatus(request.getStatus()))
                .createdAt(LocalDateTime.now())
                .build();
        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public NotificationTemplateResponse update(String id, NotificationTemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        String channelCode = request.getChannelCode().trim().toUpperCase();
        String code = request.getCode().trim().toUpperCase();
        if ((!template.getChannelCode().equals(channelCode) || !template.getCode().equals(code))
                && templateRepository.existsByChannelCodeAndCode(channelCode, code)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        template.setChannelCode(channelCode);
        template.setCode(code);
        template.setSubject(request.getSubject().trim());
        template.setBody(request.getBody());
        if (StringUtils.hasText(request.getStatus())) {
            template.setStatus(resolveStatus(request.getStatus()));
        }
        template.setUpdatedAt(LocalDateTime.now());
        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public void delete(String id) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        template.setStatus(NotificationChannelService.STATUS_INACTIVE);
        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.save(template);
    }

    private String resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return NotificationChannelService.STATUS_ACTIVE;
        }
        String s = status.trim().toUpperCase();
        if (!NotificationChannelService.STATUS_ACTIVE.equals(s)
                && !NotificationChannelService.STATUS_INACTIVE.equals(s)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return s;
    }

    private NotificationTemplateResponse toResponse(NotificationTemplate template) {
        return NotificationTemplateResponse.builder()
                .id(template.getId())
                .channelCode(template.getChannelCode())
                .code(template.getCode())
                .subject(template.getSubject())
                .body(template.getBody())
                .status(template.getStatus())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}