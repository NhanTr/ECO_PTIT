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
@Table(name = "notification_channels", uniqueConstraints = {
        @UniqueConstraint(columnNames = "code")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationChannel {
    @Id
    @Column(length = 10)
    String id;

    @Column(nullable = false, length = 64)
    String code; // IN_APP, EMAIL, SMS

    @Column(nullable = false, length = 128)
    String name;

    @Column(length = 500)
    String description;

    @Column(nullable = false, length = 16)
    String status; // ACTIVE | INACTIVE

    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column
    LocalDateTime updatedAt;
}