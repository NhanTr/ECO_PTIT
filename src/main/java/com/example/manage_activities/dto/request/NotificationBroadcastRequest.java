package com.example.manage_activities.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * QLHĐ_BM 1 — gửi thông báo thủ công theo bộ lọc đối tượng.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationBroadcastRequest {

    @NotBlank(message = "BAD_REQUEST")
    @Size(max = 255, message = "BAD_REQUEST")
    String title;

    @NotBlank(message = "BAD_REQUEST")
    String content;

    String className;
    String department;
    Integer roleId;
}
