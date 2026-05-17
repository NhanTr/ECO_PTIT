package com.example.manage_activities.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class AssignRoleRequest {
    @NotNull(message = "Role ID is required")
    @Min(value = 1, message = "Role ID must be between 1 and 4")
    @Max(value = 4, message = "Role ID must be between 1 and 4")
    Integer roleId;
}
