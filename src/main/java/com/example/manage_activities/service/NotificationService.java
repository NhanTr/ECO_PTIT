package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.NotificationBroadcastRequest;
import com.example.manage_activities.dto.request.NotificationCreateRequest;
import com.example.manage_activities.dto.response.NotificationBroadcastResponse;
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
	SystemLogService systemLogService;

	public List<NotificationResponse> getMyNotifications() {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		log.info("Getting notifications for user: {}", userId);

		return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	public List<NotificationResponse> getSentNotifications() {
		String userId = getCurrentUserId();
		log.info("Getting sent notifications for user: {}", userId);

		return notificationRepository.findBySenderIdOrderByCreatedAtDesc(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	public NotificationResponse getNotificationDetail(String notificationId) {
		String userId = getCurrentUserId();
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

		if (!userId.equals(notification.getReceiverId()) && !userId.equals(notification.getSenderId())) {
			throw new AppException(ErrorCode.UNAUTHORIZED);
		}

		return toResponse(notification);
	}

	@Transactional
	public int sendNotificationsToStudents(NotificationCreateRequest request) {
		return sendNotifications(request);
	}

	@Transactional
	public NotificationBroadcastResponse broadcastNotifications(NotificationBroadcastRequest request) {
		Integer roleId = request.getRoleId();
		String department = normalizeFilter(request.getDepartment());
		String className = normalizeFilter(request.getClassName());
		String senderId = getCurrentUserId();
		String targetLabel = buildBroadcastTargetLabel(roleId, department, className);

		List<User> recipients = userRepository.findBroadcastRecipients(roleId, department, className);
		if (recipients.isEmpty()) {
			log.info("No recipients matched broadcast filters");
			logBroadcast(request, 0);
			return NotificationBroadcastResponse.builder()
					.sentCount(0)
					.roleId(roleId)
					.className(className)
					.department(department)
					.build();
		}

		LocalDateTime now = LocalDateTime.now();
		List<Notification> notifications = recipients.stream()
				.map(user -> Notification.builder()
						.id(generateNotificationId())
						.receiverId(user.getId())
						.senderId(senderId)
						.targetLabel(targetLabel)
						.title(request.getTitle())
						.content(request.getContent())
						.type("System")
						.isRead(false)
						.createdAt(now)
						.build())
				.toList();

		notificationRepository.saveAll(notifications);
		log.info("Broadcast {} notifications to {} recipients", notifications.size(), recipients.size());
		logBroadcast(request, notifications.size());

		return NotificationBroadcastResponse.builder()
				.sentCount(notifications.size())
				.roleId(roleId)
				.className(className)
				.department(department)
				.build();
	}

	@Transactional
	public void sendNotificationToUser(String receiverId, String title, String content, String type) {
		if (receiverId == null || receiverId.isBlank()) {
			log.info("Skip notification because receiverId is blank");
			return;
		}

		Notification notification = Notification.builder()
				.id(generateNotificationId())
				.receiverId(receiverId)
				.senderId(getCurrentUserId())
				.targetLabel(receiverId)
				.title(title)
				.content(content)
				.type(type)
				.isRead(false)
				.createdAt(LocalDateTime.now())
				.build();

		notificationRepository.save(notification);
		log.info("Sent notification to user: {}", receiverId);
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
		String senderId = getCurrentUserId();
		String targetLabel = buildManualTargetLabel(request.getRecipientIds());
		List<Notification> notifications = recipients.stream()
				.map(user -> Notification.builder()
						.id(generateNotificationId())
						.receiverId(user.getId())
						.senderId(senderId)
						.targetLabel(targetLabel)
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

		if (recipientIds == null || recipientIds.isEmpty()) {
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

		boolean canSendSystemNotification = authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
						|| "ROLE_MANAGER".equals(authority.getAuthority()));

		if (!canSendSystemNotification) {
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
	public void sendParticipationApprovedNotification(String studentId, String activityTitle) {
		Notification notification = Notification.builder()
				.id(generateNotificationId())
				.receiverId(studentId)
				.senderId(getCurrentUserId())
				.targetLabel(studentId)
				.title("Đăng ký hoạt động đã được duyệt")
				.content("Đăng ký tham gia hoạt động " + activityTitle + " đã được duyệt.")
				.type("Activity")
				.isRead(false)
				.createdAt(LocalDateTime.now())
				.build();

		notificationRepository.save(notification);
		log.info("Sent approval notification to student: {} for activity: {}", studentId, activityTitle);
	}

	@Transactional
	public void sendParticipationRejectedNotification(String studentId, String activityTitle, String reason) {
		Notification notification = Notification.builder()
				.id(generateNotificationId())
				.receiverId(studentId)
				.senderId(getCurrentUserId())
				.targetLabel(studentId)
				.title("Đăng ký hoạt động bị từ chối")
				.content("Đăng ký tham gia hoạt động " + activityTitle + " bị từ chối. Lý do: " + reason)
				.type("Activity")
				.isRead(false)
				.createdAt(LocalDateTime.now())
				.build();

		notificationRepository.save(notification);
		log.info("Sent rejection notification to student: {} for activity: {}", studentId, activityTitle);
	}

	private String generateNotificationId() {
		String id = UUID.randomUUID().toString().substring(0, 10);
		while (notificationRepository.existsById(id)) {
			id = UUID.randomUUID().toString().substring(0, 10);
		}
		return id;
	}

	private void logBroadcast(NotificationBroadcastRequest request, int sentCount) {
		String filters = "roleId=" + request.getRoleId()
				+ ", className=" + request.getClassName()
				+ ", department=" + request.getDepartment();
		systemLogService.logAction(
				getCurrentUserId(),
				"BROADCAST_NOTIFICATION",
				"notifications",
				filters,
				"title=" + request.getTitle() + ", sentCount=" + sentCount);
	}

	private String getCurrentUserId() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication == null ? "system" : authentication.getName();
	}

	private String normalizeFilter(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private NotificationResponse toResponse(Notification notification) {
		return NotificationResponse.builder()
				.id(notification.getId())
				.receiverId(notification.getReceiverId())
				.receiverName(resolveUserName(notification.getReceiverId()))
				.senderId(notification.getSenderId())
				.senderName(resolveUserName(notification.getSenderId()))
				.targetLabel(localizeLegacyNotificationText(notification.getTargetLabel()))
				.title(localizeLegacyNotificationText(notification.getTitle()))
				.content(localizeLegacyNotificationText(notification.getContent()))
				.isRead(notification.getIsRead())
				.type(notification.getType())
				.createdAt(notification.getCreatedAt())
				.build();
	}

	private String localizeLegacyNotificationText(String value) {
		if (value == null || value.isBlank()) {
			return value;
		}
		return value
				.replace("Tat ca nguoi dung", "Tất cả người dùng")
				.replace("Hoat dong da duoc phan cong nguoi duyet", "Hoạt động đã được phân công người duyệt")
				.replace("Hoat dong da duoc duyet", "Hoạt động đã được duyệt")
				.replace("Hoat dong bi tu choi", "Hoạt động bị từ chối")
				.replace("Hoat dong da bi huy", "Hoạt động đã bị hủy")
				.replace("Yeu cau huy hoat dong da duoc duyet", "Yêu cầu hủy hoạt động đã được duyệt")
				.replace("Yeu cau huy hoat dong bi tu choi", "Yêu cầu hủy hoạt động bị từ chối")
				.replace("Bao cao sau hoat dong da duoc duyet", "Báo cáo sau hoạt động đã được duyệt")
				.replace("Bao cao sau hoat dong bi tu choi", "Báo cáo sau hoạt động bị từ chối")
				.replace("Dang ky hoat dong da duoc duyet", "Đăng ký hoạt động đã được duyệt")
				.replace("Dang ky hoat dong bi tu choi", "Đăng ký hoạt động bị từ chối")
				.replace("Hoat dong \"", "Hoạt động \"")
				.replace("Yeu cau huy hoat dong \"", "Yêu cầu hủy hoạt động \"")
				.replace("Bao cao cua hoat dong \"", "Báo cáo của hoạt động \"")
				.replace("Dang ky tham gia hoat dong ", "Đăng ký tham gia hoạt động ")
				.replace("\" da duoc phan cong nguoi phu trach kiem duyet.", "\" đã được phân công người phụ trách kiểm duyệt.")
				.replace("\" da duoc duyet.", "\" đã được duyệt.")
				.replace("\" da duoc chap nhan.", "\" đã được chấp nhận.")
				.replace("\" da bi huy. Ly do: ", "\" đã bị hủy. Lý do: ")
				.replace("\" bi tu choi. Ly do: ", "\" bị từ chối. Lý do: ")
				.replace("\" bi tu choi.", "\" bị từ chối.")
				.replace(" da duoc duyet.", " đã được duyệt.")
				.replace(" bi tu choi. Ly do: ", " bị từ chối. Lý do: ")
				.replace("Diem hoat dong da duoc xac nhan.", "Điểm hoạt động đã được xác nhận.");
	}

	private String resolveUserName(String userId) {
		if (userId == null || userId.isBlank()) {
			return "System";
		}
		if ("system".equalsIgnoreCase(userId)) {
			return "System";
		}
		return userRepository.findById(userId)
				.map(user -> {
					if (user.getUsername() != null && !user.getUsername().isBlank()) {
						return user.getUsername();
					}
					if (user.getEmail() != null && !user.getEmail().isBlank()) {
						return user.getEmail();
					}
					return user.getId();
				})
				.orElse(userId);
	}

	private String buildManualTargetLabel(List<String> recipientIds) {
		if (recipientIds == null || recipientIds.isEmpty()) {
			return "Tất cả người dùng";
		}
		return String.join(", ", recipientIds);
	}

	private String buildBroadcastTargetLabel(Integer roleId, String department, String className) {
		return roleLabel(roleId);
	}

	private String roleLabel(Integer roleId) {
		if (roleId == null) {
			return "Tất cả vai trò";
		}
		return switch (roleId) {
			case 1 -> "admin";
			case 2 -> "manager";
			case 3 -> "organizer";
			case 4 -> "student";
			default -> "Role " + roleId;
		};
	}
}
