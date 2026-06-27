package com.example.manage_activities.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_templates", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"channel_code", "code"})
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationTemplate {
    @Id
    @Column(length = 10)
    String id;

    @Column(name = "channel_code", nullable = false, length = 64)
    String channelCode;

    @Column(nullable = false, length = 64)
    String code; // ACTIVITY_CREATED, ACTIVITY_APPROVED, ...

    @Column(nullable = false, length = 255)
    String subject;

    @Lob
    @Column(nullable = false)
    String body;

    @Column(nullable = false, length = 16)
    String status; // ACTIVE | INACTIVE

    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column
    LocalDateTime updatedAt;
}