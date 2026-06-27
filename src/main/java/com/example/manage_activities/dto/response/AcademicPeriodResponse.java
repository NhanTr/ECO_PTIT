package com.example.manage_activities.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AcademicPeriodResponse {
    String id;
    String academicYear;
    Integer semester;
    LocalDateTime startDate;
    LocalDateTime endDate;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}