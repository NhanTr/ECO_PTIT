package com.example.manage_activities.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActivityCreateRequest {

    @NotBlank(message = "Title is required")
    String title;

    String description;

    String location;

    @NotNull(message = "Start time is required")
    LocalDateTime startTime;

    @NotNull(message = "End time is required")
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