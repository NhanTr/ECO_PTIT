package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.AcademicPeriodRequest;
import com.example.manage_activities.dto.response.AcademicPeriodResponse;
import com.example.manage_activities.entity.AcademicPeriod;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.repository.AcademicPeriodRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * QTHT #9 - Quản lý năm học/học kỳ (QTHT_QĐ 5, QTHT_BM 4).
 * Chỉ một học kỳ được OPEN tại một thời điểm.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AcademicPeriodService {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CLOSED = "CLOSED";

    AcademicPeriodRepository academicPeriodRepository;

    @Transactional(readOnly = true)
    public List<AcademicPeriodResponse> getAll() {
        return academicPeriodRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AcademicPeriodResponse create(AcademicPeriodRequest request) {
        validateRange(request);
        if (academicPeriodRepository.existsByAcademicYearAndSemester(
                request.getAcademicYear(), request.getSemester())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        String status = resolveStatus(request.getStatus());
        if (STATUS_OPEN.equals(status)) {
            closeAllOpenPeriods();
        }
        AcademicPeriod period = AcademicPeriod.builder()
                .id(generateId())
                .academicYear(request.getAcademicYear().trim())
                .semester(request.getSemester())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
        return toResponse(academicPeriodRepository.save(period));
    }

    @Transactional
    public AcademicPeriodResponse update(String id, AcademicPeriodRequest request) {
        validateRange(request);
        AcademicPeriod period = academicPeriodRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        if (!period.getAcademicYear().equals(request.getAcademicYear())
                || !period.getSemester().equals(request.getSemester())) {
            if (academicPeriodRepository.existsByAcademicYearAndSemester(
                    request.getAcademicYear(), request.getSemester())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }
        String status = resolveStatus(request.getStatus());
        if (STATUS_OPEN.equals(status)) {
            closeAllOpenPeriodsExcluding(id);
        }
        period.setAcademicYear(request.getAcademicYear().trim());
        period.setSemester(request.getSemester());
        period.setStartDate(request.getStartDate());
        period.setEndDate(request.getEndDate());
        period.setStatus(status);
        period.setUpdatedAt(LocalDateTime.now());
        return toResponse(academicPeriodRepository.save(period));
    }

    @Transactional
    public AcademicPeriodResponse setStatus(String id, String status) {
        String resolved = resolveStatus(status);
        AcademicPeriod period = academicPeriodRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        if (STATUS_OPEN.equals(resolved)) {
            closeAllOpenPeriodsExcluding(id);
        }
        period.setStatus(resolved);
        period.setUpdatedAt(LocalDateTime.now());
        return toResponse(academicPeriodRepository.save(period));
    }

    private void validateRange(AcademicPeriodRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null
                && !request.getEndDate().isAfter(request.getStartDate())) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private String resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return STATUS_CLOSED;
        }
        String normalized = status.trim().toUpperCase();
        if (!STATUS_OPEN.equals(normalized) && !STATUS_CLOSED.equals(normalized)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private void closeAllOpenPeriods() {
        List<AcademicPeriod> openPeriods = academicPeriodRepository.findByStatus(STATUS_OPEN);
        LocalDateTime now = LocalDateTime.now();
        for (AcademicPeriod p : openPeriods) {
            p.setStatus(STATUS_CLOSED);
            p.setUpdatedAt(now);
        }
        academicPeriodRepository.saveAll(openPeriods);
    }

    private void closeAllOpenPeriodsExcluding(String excludeId) {
        List<AcademicPeriod> openPeriods = academicPeriodRepository.findByStatus(STATUS_OPEN);
        LocalDateTime now = LocalDateTime.now();
        for (AcademicPeriod p : openPeriods) {
            if (p.getId().equals(excludeId)) continue;
            p.setStatus(STATUS_CLOSED);
            p.setUpdatedAt(now);
        }
        academicPeriodRepository.saveAll(openPeriods);
    }

    private AcademicPeriodResponse toResponse(AcademicPeriod period) {
        return AcademicPeriodResponse.builder()
                .id(period.getId())
                .academicYear(period.getAcademicYear())
                .semester(period.getSemester())
                .startDate(period.getStartDate())
                .endDate(period.getEndDate())
                .status(period.getStatus())
                .createdAt(period.getCreatedAt())
                .updatedAt(period.getUpdatedAt())
                .build();
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}