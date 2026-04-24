package com.example.manage_activities.service;

import java.util.List;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;

import com.example.manage_activities.dto.request.NotificationRequest;
import com.example.manage_activities.entity.Notification;
import com.example.manage_activities.repository.NotificationRepository;
import com.example.manage_activities.mapper.NotificationMapper;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationService {

    NotificationRepository notificationRepository;
    RegistrationService registrationService;
    NotificationMapper notificationMapper;

    public void sendNotification(String activityId, NotificationRequest request) {
        Notification notification = notificationMapper.toEntity(request);

        List<String> participantIds = registrationService.getPaticipantID(activityId);

        for (String participantId : participantIds) {
            notification.setReceiverId(participantId);
            notificationRepository.save(notification);
        }
        log.info("Notification sent to {} participants for activity {}", participantIds.size(), activityId);
    }

    public List<Notification> getNotificationsForUser(String userId) {
        return notificationRepository.findByReceiverId(userId);
    }

    public String generateNotificationId() {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        while (notificationRepository.existsById(id)) {
            id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        }
        return id;
    }
}
