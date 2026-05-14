package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.AttendanceRequest;
import com.example.manage_activities.dto.request.AwardPointsRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.AttendanceResponse;
import com.example.manage_activities.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizer/attendance")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AttendanceController {

    AttendanceService attendanceService;

    /**
     * Check in participant.
     * POST /api/organizer/attendance/check-in
     */
    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN','MANAGER')")
    public ResponseEntity<APIResponse<AttendanceResponse>> checkIn(
            @Valid @RequestBody AttendanceRequest request) {
        log.info("Check-in request received for registration: {}", request.getRegistrationId());
        AttendanceResponse attendance = attendanceService.checkIn(request);
        return ResponseEntity.ok(APIResponse.<AttendanceResponse>builder()
                .code(1000)
                .message("Diem danh thanh cong")
                .result(attendance)
                .build());
    }

    /**
     * Award points after activity is closed.
     * PATCH /api/organizer/attendance/points
     */
    @PatchMapping("/points")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN','MANAGER')")
    public ResponseEntity<APIResponse<AttendanceResponse>> awardPoints(
            @Valid @RequestBody AwardPointsRequest request) {
        log.info("Award points request received for registration: {}", request.getRegistrationId());
        AttendanceResponse attendance = attendanceService.awardPoints(request);
        return ResponseEntity.ok(APIResponse.<AttendanceResponse>builder()
                .code(1000)
                .message("Cap diem thanh cong")
                .result(attendance)
                .build());
    }
}