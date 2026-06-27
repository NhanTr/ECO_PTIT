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
public class NotificationTemplateRequest {
    @NotBlank
    String channelCode;

    @NotBlank
    String code;

    @NotBlank
    String subject;

    @NotBlank
    String body;

    /** ACTIVE | INACTIVE */
    String status;
}