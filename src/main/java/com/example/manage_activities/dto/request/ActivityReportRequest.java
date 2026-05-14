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
public class ActivityReportRequest {
    @NotBlank(message = "File URL must not be blank")
    String fileUrl;

    String originalFileName;
    String contentType;
    Long fileSize;
    String reviewNote;
}