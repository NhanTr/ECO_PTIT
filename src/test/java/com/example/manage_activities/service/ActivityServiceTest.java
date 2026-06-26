package com.example.manage_activities.service;

import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.entity.ActivityFile;
import com.example.manage_activities.enums.ActivityStatus;
import com.example.manage_activities.enums.ReportStatus;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.mapper.ActivityMapper;
import com.example.manage_activities.repository.ActivityFileRepository;
import com.example.manage_activities.repository.ActivityRepository;
import com.example.manage_activities.repository.AttendanceRepository;
import com.example.manage_activities.repository.RegistrationRepository;
import com.example.manage_activities.repository.RoomRepository;
import com.example.manage_activities.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityServiceTest {

    private final ActivityRepository activityRepository = mock(ActivityRepository.class);
    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final RegistrationRepository registrationRepository = mock(RegistrationRepository.class);
    private final ActivityFileRepository activityFileRepository = mock(ActivityFileRepository.class);
    private final AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
    private final RoomRepository roomRepository = mock(RoomRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final SystemConfigService systemConfigService = mock(SystemConfigService.class);
    private final SystemLogService systemLogService = mock(SystemLogService.class);
    private final ActivityService activityService =
            new ActivityService(activityRepository, activityMapper, registrationRepository, activityFileRepository,
                    attendanceRepository, roomRepository, userRepository, notificationService, systemConfigService, systemLogService);

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approveActivity_shouldApproveAndSetReviewer() {
        Activity activity = Activity.builder()
                .id("act1234567")
                .status(ActivityStatus.REVIEWING)
                .build();
        ActivityResponse response = ActivityResponse.builder()
                .id("act1234567")
                .status("Approved")
                .reviewerId("manager01")
                .build();

        when(activityRepository.findById("act1234567")).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityMapper.toDTO(any(Activity.class))).thenReturn(response);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("manager01", null)
        );

        ActivityResponse result = activityService.approveActivity("act1234567");

        assertEquals("Approved", result.getStatus());
        assertEquals("manager01", activity.getReviewerId());
        verify(activityRepository).save(activity);
    }

    @Test
    void approveActivity_shouldThrowWhenActivityNotFound() {
        when(activityRepository.findById("missing01")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> activityService.approveActivity("missing01"));

        assertEquals(ErrorCode.ACTIVITY_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void approveActivity_shouldThrowWhenAlreadyApproved() {
        Activity activity = Activity.builder()
                .id("act1234567")
                .status(ActivityStatus.APPROVED)
                .build();

        when(activityRepository.findById("act1234567")).thenReturn(Optional.of(activity));

        AppException exception = assertThrows(AppException.class,
                () -> activityService.approveActivity("act1234567"));

        assertEquals(ErrorCode.ACTIVITY_ALREADY_APPROVED, exception.getErrorCode());
    }

    @Test
    void approveActivity_shouldThrowWhenOrganizerHasOverlappingActivity() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 11, 0);
        Activity activity = Activity.builder()
                .id("act1234567")
                .status(ActivityStatus.REVIEWING)
                .organizerId("org1234567")
                .startTime(start)
                .endTime(end)
                .build();

        when(activityRepository.findById("act1234567")).thenReturn(Optional.of(activity));
        when(activityRepository.existsOverlappingOrganizerActivity(
                eq("org1234567"),
                eq("act1234567"),
                any(),
                eq(start),
                eq(end)))
                .thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> activityService.approveActivity("act1234567"));

        assertEquals(ErrorCode.ORGANIZER_ACTIVITY_TIME_CONFLICT, exception.getErrorCode());
        assertEquals(ActivityStatus.REVIEWING, activity.getStatus());
    }

    @Test
    void startDueActivities_shouldStartApprovedActivitiesThatReachedStartTime() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 17, 15, 30);
        when(activityRepository.startDueActivities(
                eq(ActivityStatus.APPROVED),
                eq(ActivityStatus.ONGOING),
                eq(now))).thenReturn(3);

        int startedCount = activityService.startDueActivities(now);

        assertEquals(3, startedCount);
        verify(activityRepository).startDueActivities(
                eq(ActivityStatus.APPROVED),
                eq(ActivityStatus.ONGOING),
                eq(now));
    }

    @Test
    void closeExpiredActivities_shouldCloseApprovedAndOngoingActivities() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 17, 15, 30);
        when(activityRepository.closeExpiredActivities(
                any(),
                eq(ActivityStatus.CLOSED),
                eq(now))).thenReturn(2);

        int closedCount = activityService.closeExpiredActivities(now);

        assertEquals(2, closedCount);
        verify(activityRepository).closeExpiredActivities(
                eq(List.of(ActivityStatus.APPROVED, ActivityStatus.ONGOING)),
                eq(ActivityStatus.CLOSED),
                eq(now));
    }

    @Test
    void approveActivity_shouldThrowWhenStatusIsCancelled() {
        Activity activity = Activity.builder()
                .id("act1234567")
                .status(ActivityStatus.CANCELLED)
                .build();

        when(activityRepository.findById("act1234567")).thenReturn(Optional.of(activity));

        AppException exception = assertThrows(AppException.class,
                () -> activityService.approveActivity("act1234567"));

        assertEquals(ErrorCode.ACTIVITY_INVALID_STATUS_TRANSITION, exception.getErrorCode());
    }

    @Test
    void rejectActivity_shouldPersistRejectReason() {
        Activity activity = Activity.builder()
                .id("act1234567")
                .status(ActivityStatus.REVIEWING)
                .title("Test Activity")
                .organizerId("org1234567")
                .build();
        ActivityResponse response = ActivityResponse.builder()
                .id("act1234567")
                .status("Rejected")
                .rejectReason("Khong du dieu kien")
                .build();

        when(activityRepository.findById("act1234567")).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityMapper.toDTO(any(Activity.class))).thenReturn(response);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("manager01", null));

        activityService.rejectActivity("act1234567", "Khong du dieu kien");

        assertEquals("Khong du dieu kien", activity.getRejectReason());
        assertEquals(ActivityStatus.REJECTED, activity.getStatus());
        verify(activityRepository).save(activity);
    }

    @Test
    void submitForReview_shouldReturnScheduleWarningsWithoutBlocking() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 11, 0);
        Activity activity = Activity.builder()
                .id("act1234567")
                .status(ActivityStatus.DRAFT)
                .roomId("A01")
                .location("A01")
                .startTime(start)
                .endTime(end)
                .organizerId("org1234567")
                .build();
        Activity conflicting = Activity.builder()
                .id("act9999999")
                .title("Existing")
                .roomId("A01")
                .location("A01")
                .status(ActivityStatus.APPROVED)
                .startTime(start)
                .endTime(end)
                .build();

        when(activityRepository.findById("act1234567")).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityMapper.toDTO(any(Activity.class))).thenReturn(ActivityResponse.builder().id("act1234567").build());
        when(activityRepository.findScheduleConflicts(
                eq("act1234567"),
                any(),
                eq("A01"),
                eq(start),
                eq(end)))
                .thenReturn(List.of(conflicting));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("org1234567", null));

        var result = activityService.submitForReview("act1234567");

        assertEquals(ActivityStatus.PENDING, activity.getStatus());
        assertNotNull(result.getScheduleConflicts());
        assertEquals(1, result.getScheduleConflicts().size());
    }

    @Test
    void startReportReview_shouldMovePendingReportToReviewing() {
        ActivityFile report = ActivityFile.builder()
                .id("file123456")
                .activityId("act1234567")
                .fileType("Report")
                .reportStatus(ReportStatus.PENDING)
                .fileUrl("/uploads/activity-reports/file123456.xlsx")
                .build();

        when(activityFileRepository.findById("file123456")).thenReturn(Optional.of(report));
        when(activityFileRepository.save(any(ActivityFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("manager01", null));

        var result = activityService.startReportReview("file123456");

        assertEquals("Reviewing", result.getReportStatus());
        assertEquals(ReportStatus.REVIEWING, report.getReportStatus());
        assertEquals("manager01", report.getReviewerId());
        verify(activityFileRepository).save(report);
    }

    @Test
    void approveReport_shouldThrowWhenReportWasNotDownloaded() {
        ActivityFile report = ActivityFile.builder()
                .id("file123456")
                .activityId("act1234567")
                .fileType("Report")
                .reportStatus(ReportStatus.PENDING)
                .build();

        when(activityFileRepository.findById("file123456")).thenReturn(Optional.of(report));

        AppException exception = assertThrows(AppException.class,
                () -> activityService.approveReport("file123456"));

        assertEquals(ErrorCode.ACTIVITY_REPORT_NOT_DOWNLOADED, exception.getErrorCode());
    }

    @Test
    void approveReport_shouldApproveReviewingReport() {
        Activity activity = Activity.builder()
                .id("act1234567")
                .title("Test Activity")
                .organizerId("org1234567")
                .trainingPoints(5)
                .build();
        ActivityFile report = ActivityFile.builder()
                .id("file123456")
                .activityId("act1234567")
                .fileType("Report")
                .reportStatus(ReportStatus.REVIEWING)
                .build();

        when(activityFileRepository.findById("file123456")).thenReturn(Optional.of(report));
        when(activityRepository.findById("act1234567")).thenReturn(Optional.of(activity));
        when(activityFileRepository.save(any(ActivityFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registrationRepository.findByActivityId("act1234567")).thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("manager01", null));

        var result = activityService.approveReport("file123456");

        assertEquals("Approved", result.getReportStatus());
        assertEquals(ReportStatus.APPROVED, report.getReportStatus());
        assertEquals("manager01", report.getReviewerId());
        assertNotNull(report.getReviewedAt());
        verify(activityFileRepository).save(report);
    }
}

