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
public class StudentStatisticsItemResponse {

    String studentId;
    String studentCode;
    String fullName;
    String className;
    String department;
    Long participatedActivityCount;
    Long totalEarnedPoints;
}
