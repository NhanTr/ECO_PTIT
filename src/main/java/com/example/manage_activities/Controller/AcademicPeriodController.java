package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.AcademicPeriodRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.AcademicPeriodResponse;
import com.example.manage_activities.service.AcademicPeriodService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * QTHT #9 - Quản lý năm học/học kỳ (QTHT_QĐ 5, QTHT_BM 4).
 */
@RestController
@RequestMapping("/api/admin/academic-periods")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class AcademicPeriodController {

    AcademicPeriodService academicPeriodService;

    @GetMapping
    public APIResponse<List<AcademicPeriodResponse>> list() {
        return APIResponse.<List<AcademicPeriodResponse>>builder()
                .result(academicPeriodService.getAll())
                .build();
    }

    @PostMapping
    public APIResponse<AcademicPeriodResponse> create(@Valid @RequestBody AcademicPeriodRequest request) {
        return APIResponse.<AcademicPeriodResponse>builder()
                .message("Đã tạo năm học/học kỳ")
                .result(academicPeriodService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    public APIResponse<AcademicPeriodResponse> update(
            @PathVariable String id,
            @Valid @RequestBody AcademicPeriodRequest request) {
        return APIResponse.<AcademicPeriodResponse>builder()
                .message("Đã cập nhật năm học/học kỳ")
                .result(academicPeriodService.update(id, request))
                .build();
    }

    @PatchMapping("/{id}/status")
    public APIResponse<AcademicPeriodResponse> setStatus(
            @PathVariable String id,
            @RequestParam String status) {
        return APIResponse.<AcademicPeriodResponse>builder()
                .message("Đã cập nhật trạng thái năm học/học kỳ")
                .result(academicPeriodService.setStatus(id, status))
                .build();
    }
}