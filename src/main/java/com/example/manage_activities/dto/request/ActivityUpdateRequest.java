package com.example.manage_activities.dto.request;

import jakarta.validation.constraints.Min;
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
public class ActivityUpdateRequest {
    String title;
    String description;
    String roomId;
    String location;
    LocalDateTime startTime;
    LocalDateTime endTime;
    LocalDateTime registrationDeadline;

    @Min(value = 1, message = "Max participants must be at least 1")
    Integer maxParticipants;

    BigDecimal budget;
    String sponsor;
    String targetAudience;
    String purpose;
    Integer trainingPoints;
}
