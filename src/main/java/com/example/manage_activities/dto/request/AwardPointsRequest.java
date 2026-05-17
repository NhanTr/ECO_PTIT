package com.example.manage_activities.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class AwardPointsRequest {
    @NotBlank(message = "Registration id must not be blank")
    String registrationId;

    @Min(value = 0, message = "Points must be at least 0")
    Integer earnedPoints;
}