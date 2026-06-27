package com.example.manage_activities.controller;

import com.example.manage_activities.Controller.AdminActivityController;
import com.example.manage_activities.dto.request.AssignActivityRequest;
import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.dto.response.ActivityScheduleConflictResponse;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.exception.GlobalExceptionHandler;
import com.example.manage_activities.service.ActivityService;
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
import org.springframework.data.domain.Pageable;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminActivityController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class AdminActivityControllerSecurityTest {

    private static final String BASE_URL = "/api/admin/activities";
    private static final String ACTIVITY_ID = "ACT1234567";
    private static final String REVIEWER_ID = "MAN1234567";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ActivityService activityService;

    @Nested
    @DisplayName("ADMIN / MANAGER authorized")
    class AuthorizedRoleTests {

        @Test
        @WithMockUser(username = "manager-caller", roles = "MANAGER")
        void managerSearchActivities_returnsOkWithPage() throws Exception {
            Page<ActivityResponse> page = new PageImpl<>(
                    List.of(sampleActivity(ACTIVITY_ID)),
                    PageRequest.of(0, 20),
                    1);
            when(activityService.searchActivitiesForManager(
                    isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.result.content").isArray())
                    .andExpect(jsonPath("$.result.content[0].id").value(ACTIVITY_ID))
                    .andExpect(jsonPath("$.result.content[0].status").value("Pending"));

            verify(activityService).searchActivitiesForManager(
                    isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @WithMockUser(username = "admin-caller", roles = "ADMIN")
        void adminScheduleConflicts_returnsOkWithList() throws Exception {
            List<ActivityScheduleConflictResponse> conflicts = List.of(
                    ActivityScheduleConflictResponse.builder()
                            .activityId("CONF000001")
                            .title("Existing event")
                            .location("Phong A101")
                            .sameLocation(true)
                            .overlappingTime(true)
                            .warning("Trung phong va trung khung gio")
                            .build());

            when(activityService.getScheduleConflicts(ACTIVITY_ID)).thenReturn(conflicts);

            mockMvc.perform(get(BASE_URL + "/" + ACTIVITY_ID + "/schedule-conflicts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.result").isArray())
                    .andExpect(jsonPath("$.result[0].activityId").value("CONF000001"))
                    .andExpect(jsonPath("$.result[0].sameLocation").value(true));

            verify(activityService).getScheduleConflicts(ACTIVITY_ID);
        }

        @Test
        @WithMockUser(username = "admin-caller", roles = "ADMIN")
        void adminAssignActivity_returnsOk() throws Exception {
            AssignActivityRequest request = AssignActivityRequest.builder()
                    .reviewerId(REVIEWER_ID)
                    .build();
            ActivityResponse activity = sampleActivity(ACTIVITY_ID);
            activity.setReviewerId(REVIEWER_ID);

            when(activityService.assignReviewer(ACTIVITY_ID, REVIEWER_ID)).thenReturn(activity);

            mockMvc.perform(put(BASE_URL + "/" + ACTIVITY_ID + "/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Da phan cong nguoi phu trach hoat dong"))
                    .andExpect(jsonPath("$.result.reviewerId").value(REVIEWER_ID));

            verify(activityService).assignReviewer(ACTIVITY_ID, REVIEWER_ID);
        }
    }

    @Nested
    @DisplayName("STUDENT / ORGANIZER forbidden")
    class UnauthorizedRoleTests {

        @Test
        @WithMockUser(username = "student-caller", roles = "STUDENT")
        void studentSearchActivities_returnsForbidden() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()));
        }

        @Test
        @WithMockUser(username = "organizer-caller", roles = "ORGANIZER")
        void organizerSearchActivities_returnsForbidden() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
        }
    }

    private static ActivityResponse sampleActivity(String id) {
        return ActivityResponse.builder()
                .id(id)
                .title("Sample Activity")
                .location("Phong A101")
                .status("Pending")
                .organizerId("ORG1234567")
                .startTime(LocalDateTime.of(2026, 6, 1, 9, 0))
                .endTime(LocalDateTime.of(2026, 6, 1, 11, 0))
                .createdAt(LocalDateTime.of(2026, 5, 18, 10, 0))
                .build();
    }
}
