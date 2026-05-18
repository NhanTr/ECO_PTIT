package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.ActivityStatisticsResponse;
import com.example.manage_activities.dto.response.StudentStatisticsResponse;
import com.example.manage_activities.service.StatisticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Module 3 — QLHĐ_BM 2 & QLHĐ_BM 3 activity and student reports.
 * System-wide counters/export remain on {@link SystemStatisticsController}.
 */
@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminStatisticsController {

    StatisticsService statisticsService;

    /**
     * QLHĐ_BM 2 — báo cáo thống kê hoạt động.
     * GET /api/admin/statistics/activities?fromTime=...&toTime=...
     */
    @GetMapping("/activities")
    public APIResponse<ActivityStatisticsResponse> getActivityStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime) {
        log.info("Activity statistics report fromTime={}, toTime={}", fromTime, toTime);
        return APIResponse.<ActivityStatisticsResponse>builder()
                .result(statisticsService.getActivityStatistics(fromTime, toTime))
                .build();
    }

    /**
     * QLHĐ_BM 3 — báo cáo điểm hoạt động sinh viên.
     * GET /api/admin/statistics/students?fromTime=...&toTime=...&className=...&department=...
     */
    @GetMapping("/students")
    public APIResponse<StudentStatisticsResponse> getStudentStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String department) {
        log.info("Student statistics report className={}, department={}, fromTime={}, toTime={}",
                className, department, fromTime, toTime);
        return APIResponse.<StudentStatisticsResponse>builder()
                .result(statisticsService.getStudentStatistics(fromTime, toTime, className, department))
                .build();
    }
}
