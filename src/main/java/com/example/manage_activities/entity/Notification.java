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
@Table(name = "notifications")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {
    @Id
    @Column(length = 10)
    String id;

    @Column(name = "receiver_id", length = 10)
    String receiverId;

    String title;
    
    @Column(columnDefinition = "TEXT")
    String content;

    @Column(name = "is_read")
    @Builder.Default
    Boolean isRead = false;

    String type; // System, Activity
    LocalDateTime createdAt;
}