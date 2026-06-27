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
public class NotificationTemplateResponse {
    String id;
    String channelCode;
    String code;
    String subject;
    String body;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}