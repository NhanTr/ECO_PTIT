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
public class RegistrationResponse {

    String id;
    String activityId;
    String studentId;
    String studentName;
    String studentEmail;
    String studentCode;
    String className;
    String department;
    String approvedBy;
    String status;
    LocalDateTime approvedAt;
    LocalDateTime cancelledAt;
    LocalDateTime createdAt;
    String attendanceId;
    Boolean isPresent;
    LocalDateTime checkInTime;
    Integer earnedPoints;
}
