package com.example.manage_activities.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class AcademicPeriodRequest {
    @NotBlank
    String academicYear;

    @NotNull
    @Min(1) @Max(2)
    Integer semester;

    LocalDateTime startDate;
    LocalDateTime endDate;

    /** OPEN | CLOSED */
    String status;
}