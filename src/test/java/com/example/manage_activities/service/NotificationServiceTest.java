package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.NotificationCreateRequest;
import com.example.manage_activities.dto.response.NotificationResponse;
import com.example.manage_activities.entity.Notification;
import com.example.manage_activities.entity.User;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.NotificationRepository;
import com.example.manage_activities.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationService notificationService = new NotificationService(notificationRepository, userRepository);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sendNotificationsToStudents_shouldSendToAllUsersWhenRecipientIdsEmpty() {
        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .title("Hoat dong moi")
                .content("Co hoat dong moi vua duoc dang")
                .type("Activity")
                .build();

        List<User> users = List.of(
                User.builder().id("std0000001").roleId(4).build(),
                User.builder().id("mng0000001").roleId(2).build()
        );

        when(userRepository.findAll()).thenReturn(users);
        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int sentCount = notificationService.sendNotificationsToStudents(request);

        assertEquals(2, sentCount);
        verify(userRepository).findAll();
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void sendNotificationsToStudents_shouldSendOnlyToProvidedStudentIds() {
        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .title("Cap nhat")
                .content("Noi dung cap nhat")
                .type("Activity")
                .userIds(List.of("adm0000001"))
                .build();

        List<User> users = List.of(User.builder().id("adm0000001").roleId(1).build());

        when(userRepository.findByIdIn(List.of("adm0000001"))).thenReturn(users);
        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int sentCount = notificationService.sendNotificationsToStudents(request);

        assertEquals(1, sentCount);
        verify(userRepository).findByIdIn(List.of("adm0000001"));
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void sendNotificationsToStudents_shouldSendToAllUsersWhenTypeIsSystem() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "adm0000001",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .title("Thong bao he thong")
                .content("Bao tri he thong")
                .type("System")
                .userIds(List.of("std0000001"))
                .build();

        List<User> users = List.of(
                User.builder().id("std0000001").roleId(4).build(),
                User.builder().id("org0000001").roleId(3).build()
        );

        when(userRepository.findAll()).thenReturn(users);
        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int sentCount = notificationService.sendNotificationsToStudents(request);

        assertEquals(2, sentCount);
        verify(userRepository).findAll();
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void sendNotificationsToStudents_shouldRejectSystemTypeWhenSenderIsNotAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "mng0000001",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
                )
        );

        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .title("Thong bao he thong")
                .content("Bao tri he thong")
                .type("System")
                .build();

        AppException exception = assertThrows(AppException.class,
                () -> notificationService.sendNotificationsToStudents(request));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void sendNotificationsToStudents_shouldReturnZeroWhenNoRecipients() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "adm0000001",
                        "N/A",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .title("Cap nhat")
                .content("No recipients")
                .type("System")
                .build();

        when(userRepository.findAll()).thenReturn(List.of());

        int sentCount = notificationService.sendNotificationsToStudents(request);

        assertEquals(0, sentCount);
        verify(userRepository).findAll();
    }

    @Test
    void sendParticipationRejectedNotification_shouldSaveOneNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.sendParticipationRejectedNotification(
                "std0000001",
                "act0000001",
                "Thieu thong tin kinh phi"
        );

        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals("std0000001", saved.getReceiverId());
        assertEquals("Activity", saved.getType());
        assertTrue(saved.getContent().contains("Ly do: Thieu thong tin kinh phi"));
    }

    @Test
    void getMyNotifications_shouldReturnNotificationsOfAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("std0000001", "N/A")
        );

        List<Notification> notifications = List.of(
                Notification.builder()
                        .id("noti000001")
                        .receiverId("std0000001")
                        .title("Thong bao")
                        .content("Noi dung")
                        .type("System")
                        .isRead(false)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc("std0000001")).thenReturn(notifications);

        List<NotificationResponse> result = notificationService.getMyNotifications();

        assertEquals(1, result.size());
        assertEquals("noti000001", result.get(0).getId());
        assertEquals("std0000001", result.get(0).getReceiverId());
        verify(notificationRepository).findByReceiverIdOrderByCreatedAtDesc("std0000001");
    }
}


