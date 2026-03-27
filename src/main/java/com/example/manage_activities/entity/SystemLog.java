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
@Table(name = "system_logs")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemLog {
    @Id
    @Column(length = 10)
    String id;

    @Column(name = "user_id", length = 10)
    String userId;

    String action;
    String tableAffected;
    
    @Column(columnDefinition = "TEXT")
    String oldValue;
    
    @Column(columnDefinition = "TEXT")
    String newValue;

    LocalDateTime createdAt;
}