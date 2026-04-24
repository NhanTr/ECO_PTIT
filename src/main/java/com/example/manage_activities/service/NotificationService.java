package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.NotificationCreateRequest;
import com.example.manage_activities.dto.response.NotificationResponse;
import com.example.manage_activities.entity.Notification;
import com.example.manage_activities.entity.User;
import com.example.manage_activities.repository.NotificationRepository;
import com.example.manage_activities.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationService {

	static final int STUDENT_ROLE_ID = 4;

	NotificationRepository notificationRepository;
	UserRepository userRepository;

	public List<NotificationResponse> getMyNotifications() {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		log.info("Getting notifications for user: {}", userId);

		return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public int sendNotificationsToStudents(NotificationCreateRequest request) {
		List<User> recipients = resolveRecipients(request.getStudentIds());
		if (recipients.isEmpty()) {
			log.info("No student recipients found for notification");
			return 0;
		}

		LocalDateTime now = LocalDateTime.now();
		List<Notification> notifications = recipients.stream()
				.map(user -> Notification.builder()
						.id(generateNotificationId())
						.receiverId(user.getId())
						.title(request.getTitle())
						.content(request.getContent())
						.type(request.getType())
						.isRead(false)
						.createdAt(now)
						.build())
				.toList();

		notificationRepository.saveAll(notifications);
		log.info("Saved {} notifications", notifications.size());
		return notifications.size();
	}

	private List<User> resolveRecipients(List<String> studentIds) {
		if (studentIds == null || studentIds.isEmpty()) {
			return userRepository.findByRoleId(STUDENT_ROLE_ID);
		}
		return userRepository.findByRoleIdAndIdIn(STUDENT_ROLE_ID, studentIds);
	}


	@Transactional
	public void sendParticipationRejectedNotification(String studentId, String activityId, String reason) {
		Notification notification = Notification.builder()
				.id(generateNotificationId())
				.receiverId(studentId)
				.title("Dang ky hoat dong bi tu choi")
				.content("Dang ky tham gia hoat dong " + activityId + " bi tu choi. Ly do: " + reason)
				.type("Activity")
				.isRead(false)
				.createdAt(LocalDateTime.now())
				.build();

		notificationRepository.save(notification);
		log.info("Sent rejection notification to student: {} for activity: {}", studentId, activityId);
	}

	private String generateNotificationId() {
		String id = UUID.randomUUID().toString().substring(0, 10);
		while (notificationRepository.existsById(id)) {
			id = UUID.randomUUID().toString().substring(0, 10);
		}
		return id;
	}

	private NotificationResponse toResponse(Notification notification) {
		return NotificationResponse.builder()
				.id(notification.getId())
				.receiverId(notification.getReceiverId())
				.title(notification.getTitle())
				.content(notification.getContent())
				.isRead(notification.getIsRead())
				.type(notification.getType())
				.createdAt(notification.getCreatedAt())
				.build();
	}
}
