package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.NotificationCreateRequest;
import com.example.manage_activities.dto.response.NotificationResponse;
import com.example.manage_activities.entity.Notification;
import com.example.manage_activities.entity.User;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.NotificationRepository;
import com.example.manage_activities.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
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
		return sendNotifications(request);
	}

	@Transactional
	public int sendNotifications(NotificationCreateRequest request) {
		validateSystemNotificationPermission(request);

		List<User> recipients = resolveRecipients(request);
		if (recipients.isEmpty()) {
			log.info("No recipients found for notification");
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

	private List<User> resolveRecipients(NotificationCreateRequest request) {
		List<String> recipientIds = request.getRecipientIds();

		if ("System".equalsIgnoreCase(request.getType()) || recipientIds == null || recipientIds.isEmpty()) {
			return userRepository.findAll();
		}
		return userRepository.findByIdIn(recipientIds);
	}

	private void validateSystemNotificationPermission(NotificationCreateRequest request) {
		if (!"System".equalsIgnoreCase(request.getType())) {
			return;
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AppException(ErrorCode.UNAUTHENTICATED);
		}

		boolean isAdmin = authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

		if (!isAdmin) {
			throw new AppException(ErrorCode.UNAUTHORIZED);
		}
	}

	@Transactional
	public NotificationResponse markNotificationReadStatus(String notificationId, boolean isRead) {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

		if (!userId.equals(notification.getReceiverId())) {
			throw new AppException(ErrorCode.UNAUTHORIZED);
		}

		notification.setIsRead(isRead);
		return toResponse(notificationRepository.save(notification));
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
