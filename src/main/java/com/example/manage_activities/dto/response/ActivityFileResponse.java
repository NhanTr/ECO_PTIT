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
public class ActivityFileResponse {
    String id;
    String activityId;
    String reviewerId;
    String uploadedBy;
    String reportStatus;
    String fileUrl;
    String fileType;
    String originalFileName;
    String contentType;
    Long fileSize;
    LocalDateTime uploadedAt;
    LocalDateTime reviewedAt;
    String reviewNote;
}