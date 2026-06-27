package com.example.manage_activities.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "academic_periods", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"academic_year", "semester"})
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AcademicPeriod {
    @Id
    @Column(length = 10)
    String id;

    /** Ví dụ: "2025-2026" */
    @Column(name = "academic_year", nullable = false, length = 16)
    String academicYear;

    /** 1 hoặc 2 */
    @Column(nullable = false)
    Integer semester;

    @Column(name = "start_date")
    LocalDateTime startDate;

    @Column(name = "end_date")
    LocalDateTime endDate;

    @Column(nullable = false, length = 16)
    String status; // OPEN | CLOSED

    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column
    LocalDateTime updatedAt;
}