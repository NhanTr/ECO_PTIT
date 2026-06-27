package com.example.manage_activities.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemStatisticsResponse {
    /** Tổng toàn hệ thống */
    Long totalUsers;
    Long totalActivities;
    Long totalRegistrations;
    Long totalAttendance;
    Long totalEarnedPoints;

    /** Số liệu trong kỳ đang chọn */
    Long periodActivities;
    Long periodRegistrations;
    Long periodAttendance;
    Long periodEarnedPoints;

    /** Số liệu kỳ trước (cùng loại học kỳ) để tính tăng/giảm */
    Long previousPeriodActivities;
    Long previousPeriodRegistrations;
    Long previousPeriodAttendance;
    Long previousPeriodEarnedPoints;

    /** Chuỗi mô tả kỳ đang xem, vd "HK1 2025-2026" */
    String periodLabel;
}