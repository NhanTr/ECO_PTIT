package com.example.manage_activities.dto.request;

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
public class CategoryRequest {
    @NotBlank
    String type; // DEPARTMENT | ACTIVITY_TYPE | POINT_TYPE | SPONSOR

    @NotBlank
    String code;

    @NotBlank
    String name;

    String description;

    String status;
}