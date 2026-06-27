package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.SystemStatisticsResponse;
import com.example.manage_activities.service.SystemStatisticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * QTHT #5 - Thống kê dữ liệu toàn hệ thống + xuất Excel/PDF (QTHT_BM 1).
 */
@RestController
@RequestMapping("/api/admin/system-statistics")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class SystemStatisticsController {

    SystemStatisticsService systemStatisticsService;

    @GetMapping
    public APIResponse<SystemStatisticsResponse> getStatistics() {
        return APIResponse.<SystemStatisticsResponse>builder()
                .result(systemStatisticsService.getStatistics())
                .build();
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() {
        byte[] payload = systemStatisticsService.exportExcel();
        String filename = "system-statistics-" + LocalDate.now() + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(payload, headers, 200);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf() {
        byte[] payload = systemStatisticsService.exportPdf();
        String filename = "system-statistics-" + LocalDate.now() + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(payload, headers, 200);
    }
}