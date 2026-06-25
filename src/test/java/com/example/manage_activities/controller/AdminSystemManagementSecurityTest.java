package com.example.manage_activities.controller;

import com.example.manage_activities.Controller.AdminNotificationController;
import com.example.manage_activities.Controller.BackupController;
import com.example.manage_activities.Controller.SystemConfigController;
import com.example.manage_activities.Controller.SystemLogController;
import com.example.manage_activities.dto.request.NotificationBroadcastRequest;
import com.example.manage_activities.dto.request.SystemConfigBulkUpdateRequest;
import com.example.manage_activities.dto.request.SystemConfigUpdateItem;
import com.example.manage_activities.dto.response.BackupFileResponse;
import com.example.manage_activities.dto.response.NotificationBroadcastResponse;
import com.example.manage_activities.dto.response.SystemConfigResponse;
import com.example.manage_activities.dto.response.SystemLogResponse;
import com.example.manage_activities.exception.GlobalExceptionHandler;
import com.example.manage_activities.service.BackupService;
import com.example.manage_activities.service.NotificationService;
import com.example.manage_activities.service.SystemConfigService;
import com.example.manage_activities.service.SystemLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AdminNotificationController.class,
        SystemConfigController.class,
        BackupController.class,
        SystemLogController.class
})
@AutoConfigureMockMvc(addFilters = false)
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class AdminSystemManagementSecurityTest {

    private static final String BROADCAST_URL = "/api/admin/notifications/broadcast";
    private static final String CONFIGS_URL = "/api/admin/system-configs";
    private static final String BACKUP_EXPORT_URL = "/api/admin/backups/export";
    private static final String BACKUP_RESTORE_URL = "/api/admin/backups/restore";
    private static final String SYSTEM_LOGS_URL = "/api/admin/system-logs";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    NotificationService notificationService;

    @MockBean
    SystemConfigService systemConfigService;

    @MockBean
    BackupService backupService;

    @MockBean
    SystemLogService systemLogService;

    @Nested
    @DisplayName("ADMIN — full system management access")
    class AdminAccessTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminBroadcast_returnsOk() throws Exception {
            when(notificationService.broadcastNotifications(any(NotificationBroadcastRequest.class)))
                    .thenReturn(NotificationBroadcastResponse.builder().sentCount(3).build());

            mockMvc.perform(post(BROADCAST_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleBroadcastRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.sentCount").value(3));

            verify(notificationService).broadcastNotifications(any(NotificationBroadcastRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminGetConfigs_returnsOk() throws Exception {
            when(systemConfigService.getAllConfigs()).thenReturn(List.of(sampleConfig()));

            mockMvc.perform(get(CONFIGS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result[0].key").value("DEFAULT_TRAINING_POINTS"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminUpdateConfigs_returnsOk() throws Exception {
            when(systemConfigService.updateConfigs(any())).thenReturn(List.of(sampleConfig()));

            mockMvc.perform(put(CONFIGS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleBulkUpdate())))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminExportBackup_returnsOk() throws Exception {
            when(backupService.createManualBackup())
                    .thenReturn(BackupFileResponse.builder().fileName("backup-manual.zip").build());

            mockMvc.perform(post(BACKUP_EXPORT_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.fileName").value("backup-manual.zip"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminSearchLogs_returnsOk() throws Exception {
            Page<SystemLogResponse> page = new PageImpl<>(
                    List.of(SystemLogResponse.builder()
                            .id("LOG0000001")
                            .action("CREATE_USER")
                            .userId("adm0000001")
                            .build()),
                    PageRequest.of(0, 20),
                    1);
            when(systemLogService.searchLogs(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(page);

            mockMvc.perform(get(SYSTEM_LOGS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.content[0].action").value("CREATE_USER"));
        }
    }

    @Nested
    @DisplayName("MANAGER — broadcast only")
    class ManagerAccessTests {

        @Test
        @WithMockUser(roles = "MANAGER")
        void managerBroadcast_returnsOk() throws Exception {
            when(notificationService.broadcastNotifications(any(NotificationBroadcastRequest.class)))
                    .thenReturn(NotificationBroadcastResponse.builder().sentCount(1).build());

            mockMvc.perform(post(BROADCAST_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleBroadcastRequest())))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        void managerConfigs_returnsForbidden() throws Exception {
            mockMvc.perform(get(CONFIGS_URL))
                    .andExpect(status().isForbidden());

            mockMvc.perform(put(CONFIGS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleBulkUpdate())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        void managerBackup_returnsForbidden() throws Exception {
            mockMvc.perform(post(BACKUP_EXPORT_URL))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post(BACKUP_RESTORE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fileName\":\"backup.zip\",\"confirmation\":\"RESTORE\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        void managerSystemLogs_returnsForbidden() throws Exception {
            mockMvc.perform(get(SYSTEM_LOGS_URL))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("STUDENT / ORGANIZER — denied on all Module 4 admin endpoints")
    class ForbiddenRoleTests {

        @Test
        @WithMockUser(roles = "STUDENT")
        void studentAllEndpoints_returnsForbidden() throws Exception {
            mockMvc.perform(post(BROADCAST_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleBroadcastRequest())))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get(CONFIGS_URL)).andExpect(status().isForbidden());
            mockMvc.perform(post(BACKUP_EXPORT_URL)).andExpect(status().isForbidden());
            mockMvc.perform(get(SYSTEM_LOGS_URL)).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ORGANIZER")
        void organizerAllEndpoints_returnsForbidden() throws Exception {
            mockMvc.perform(post(BROADCAST_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleBroadcastRequest())))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get(CONFIGS_URL)).andExpect(status().isForbidden());
            mockMvc.perform(post(BACKUP_EXPORT_URL)).andExpect(status().isForbidden());
            mockMvc.perform(get(SYSTEM_LOGS_URL)).andExpect(status().isForbidden());
        }
    }

    private NotificationBroadcastRequest sampleBroadcastRequest() {
        return NotificationBroadcastRequest.builder()
                .title("Thong bao he thong")
                .content("Noi dung thong bao gui sinh vien")
                .roleId(4)
                .className("D20CQCN01-B")
                .department("CNTT")
                .build();
    }

    private SystemConfigBulkUpdateRequest sampleBulkUpdate() {
        return SystemConfigBulkUpdateRequest.builder()
                .configs(List.of(
                        SystemConfigUpdateItem.builder()
                                .key("DEFAULT_TRAINING_POINTS")
                                .value("10")
                                .build()))
                .build();
    }

    private SystemConfigResponse sampleConfig() {
        return SystemConfigResponse.builder()
                .key("DEFAULT_TRAINING_POINTS")
                .value("5")
                .valueType("INTEGER")
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
