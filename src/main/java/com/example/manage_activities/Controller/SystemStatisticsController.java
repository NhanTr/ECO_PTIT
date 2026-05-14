package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.SystemStatisticsResponse;
import com.example.manage_activities.service.SystemStatisticsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemStatisticsController {

    SystemStatisticsService systemStatisticsService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public APIResponse<SystemStatisticsResponse> getStatistics() {
        return APIResponse.<SystemStatisticsResponse>builder()
                .result(systemStatisticsService.getStatistics())
                .build();
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"system-statistics.xlsx\"");
        response.getOutputStream().write(systemStatisticsService.exportExcel());
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportPdf(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"system-statistics.pdf\"");
        response.getOutputStream().write(systemStatisticsService.exportPdf());
    }
}
