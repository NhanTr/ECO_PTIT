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
public class StudentActivityHistoryResponse {

    String registrationId;
    String activityId;
    String activityTitle;
    String location;
    LocalDateTime startTime;
    LocalDateTime endTime;
    String activityStatus;
    String registrationStatus;
    Boolean isPresent;
    Integer earnedPoints;
    LocalDateTime registeredAt;
}