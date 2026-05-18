package com.example.manage_activities.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

/**
 * QLHĐ_BM 3 — báo cáo điểm hoạt động sinh viên.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentStatisticsResponse {

    LocalDateTime fromTime;
    LocalDateTime toTime;
    String className;
    String department;

    List<StudentStatisticsItemResponse> students;
}
