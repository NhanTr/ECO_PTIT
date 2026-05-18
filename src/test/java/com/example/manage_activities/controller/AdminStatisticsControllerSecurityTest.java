package com.example.manage_activities.controller;

import com.example.manage_activities.Controller.AdminStatisticsController;
import com.example.manage_activities.dto.response.ActivityStatisticsItemResponse;
import com.example.manage_activities.dto.response.ActivityStatisticsResponse;
import com.example.manage_activities.dto.response.StudentStatisticsItemResponse;
import com.example.manage_activities.dto.response.StudentStatisticsResponse;
import com.example.manage_activities.exception.GlobalExceptionHandler;
import com.example.manage_activities.service.StatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminStatisticsController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableMethodSecurity
@Import(GlobalExceptionHandler.class)
class AdminStatisticsControllerSecurityTest {

    private static final String ACTIVITIES_URL = "/api/admin/statistics/activities";
    private static final String STUDENTS_URL = "/api/admin/statistics/students";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    StatisticsService statisticsService;

    @Nested
    @DisplayName("ADMIN / MANAGER — authorized")
    class AuthorizedRoleTests {

        @Test
        @WithMockUser(roles = "MANAGER")
        void managerActivityStatistics_returnsOk() throws Exception {
            when(statisticsService.getActivityStatistics(isNull(), isNull()))
                    .thenReturn(sampleActivityStatistics());

            mockMvc.perform(get(ACTIVITIES_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.result.totalActivities").value(2))
                    .andExpect(jsonPath("$.result.approvedCount").value(1))
                    .andExpect(jsonPath("$.result.activities[0].title").value("Workshop A"));

            verify(statisticsService).getActivityStatistics(isNull(), isNull());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminStudentStatistics_returnsOk() throws Exception {
            when(statisticsService.getStudentStatistics(isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(sampleStudentStatistics());

            mockMvc.perform(get(STUDENTS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.result.students[0].studentCode").value("B20DCPT001"))
                    .andExpect(jsonPath("$.result.students[0].totalEarnedPoints").value(10));

            verify(statisticsService).getStudentStatistics(isNull(), isNull(), isNull(), isNull());
        }
    }

    @Nested
    @DisplayName("STUDENT / ORGANIZER — forbidden")
    class ForbiddenRoleTests {

        @Test
        @WithMockUser(roles = "STUDENT")
        void studentActivityStatistics_returnsForbidden() throws Exception {
            mockMvc.perform(get(ACTIVITIES_URL))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ORGANIZER")
        void organizerStudentStatistics_returnsForbidden() throws Exception {
            mockMvc.perform(get(STUDENTS_URL))
                    .andExpect(status().isForbidden());
        }
    }

    private ActivityStatisticsResponse sampleActivityStatistics() {
        return ActivityStatisticsResponse.builder()
                .fromTime(LocalDateTime.of(2026, 1, 1, 0, 0))
                .toTime(LocalDateTime.of(2026, 12, 31, 23, 59))
                .totalActivities(2L)
                .approvedCount(1L)
                .ongoingCount(1L)
                .activities(List.of(
                        ActivityStatisticsItemResponse.builder()
                                .activityId("ACT0000001")
                                .title("Workshop A")
                                .organizerName("CLB Eco")
                                .registeredStudentCount(30L)
                                .attendedStudentCount(25L)
                                .status("APPROVED")
                                .build()))
                .build();
    }

    private StudentStatisticsResponse sampleStudentStatistics() {
        return StudentStatisticsResponse.builder()
                .students(List.of(
                        StudentStatisticsItemResponse.builder()
                                .studentId("USR0000001")
                                .studentCode("B20DCPT001")
                                .fullName("Nguyen Van A")
                                .className("D20CQCN01-B")
                                .department("CNTT")
                                .participatedActivityCount(2L)
                                .totalEarnedPoints(10L)
                                .build()))
                .build();
    }
}
