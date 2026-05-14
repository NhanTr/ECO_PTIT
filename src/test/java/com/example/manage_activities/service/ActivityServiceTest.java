package com.example.manage_activities.service;

import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.enums.ActivityStatus;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.mapper.ActivityMapper;
import com.example.manage_activities.repository.ActivityFileRepository;
import com.example.manage_activities.repository.ActivityRepository;
import com.example.manage_activities.repository.RegistrationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityServiceTest {

    private final ActivityRepository activityRepository = mock(ActivityRepository.class);
    private final ActivityMapper activityMapper = mock(ActivityMapper.class);
    private final RegistrationRepository registrationRepository = mock(RegistrationRepository.class);
    private final ActivityFileRepository activityFileRepository = mock(ActivityFileRepository.class);
    private final ActivityService activityService =
            new ActivityService(activityRepository, activityMapper, registrationRepository, activityFileRepository);

    @AfterEach
    void cleanupSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approveActivity_shouldApproveAndSetReviewer() {
        Activity activity = Activity.builder()
                .id("act1234567")
                .status(ActivityStatus.PENDING)
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
}

