package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.NotificationCreateRequest;
import com.example.manage_activities.entity.Notification;
import com.example.manage_activities.entity.User;
import com.example.manage_activities.repository.NotificationRepository;
import com.example.manage_activities.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationService notificationService = new NotificationService(notificationRepository, userRepository);

    @Test
    void sendNotificationsToStudents_shouldSendToAllStudentsWhenStudentIdsEmpty() {
        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .title("Hoat dong moi")
                .content("Co hoat dong moi vua duoc dang")
                .type("Activity")
                .build();

        List<User> students = List.of(
                User.builder().id("std0000001").roleId(4).build(),
                User.builder().id("std0000002").roleId(4).build()
        );

        when(userRepository.findByRoleId(4)).thenReturn(students);
        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int sentCount = notificationService.sendNotificationsToStudents(request);

        assertEquals(2, sentCount);
        verify(userRepository).findByRoleId(4);
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void sendNotificationsToStudents_shouldSendOnlyToProvidedStudentIds() {
        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .title("Cap nhat")
                .content("Noi dung cap nhat")
                .type("System")
                .studentIds(List.of("std0000003"))
                .build();

        List<User> students = List.of(User.builder().id("std0000003").roleId(4).build());

        when(userRepository.findByRoleIdAndIdIn(4, List.of("std0000003"))).thenReturn(students);
        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int sentCount = notificationService.sendNotificationsToStudents(request);

        assertEquals(1, sentCount);
        verify(userRepository).findByRoleIdAndIdIn(4, List.of("std0000003"));
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void sendNotificationsToStudents_shouldReturnZeroWhenNoRecipients() {
        NotificationCreateRequest request = NotificationCreateRequest.builder()
                .title("Cap nhat")
                .content("No recipients")
                .type("System")
                .build();

        when(userRepository.findByRoleId(4)).thenReturn(List.of());

        int sentCount = notificationService.sendNotificationsToStudents(request);

        assertEquals(0, sentCount);
        verify(userRepository).findByRoleId(4);
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
}


