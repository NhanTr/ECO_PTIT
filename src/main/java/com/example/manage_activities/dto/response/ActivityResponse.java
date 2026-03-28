package com.example.manage_activities.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActivityResponse {
    
    String id;
    String title;
    String description;
    String location;
    LocalDateTime startTime;
    LocalDateTime endTime;
    BigDecimal budget;
    String sponsor;
    String targetAudience;
    String purpose;
    Integer trainingPoints;
    String status;
    String organizerId;
    String reviewerId;
    LocalDateTime createdAt;
}
