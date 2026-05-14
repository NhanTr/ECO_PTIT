package com.example.manage_activities.entity;

import com.example.manage_activities.enums.ActivityStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.math.BigDecimal;



@Entity
@Table(name = "activities")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Activity {
    @Id
    @Column(length = 10)
    String id;

    @Column(name = "organizer_id", length = 10)
    String organizerId;

    @Column(name = "reviewer_id", length = 10)
    String reviewerId;

    @Column(nullable = false)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    String location;
    LocalDateTime startTime;
    LocalDateTime endTime;
    
    @Column(precision = 15, scale = 2)
    BigDecimal budget;
    
    String sponsor;
    String targetAudience;
    
    @Column(columnDefinition = "TEXT")
    String purpose;
    
    Integer trainingPoints;
    ActivityStatus status;
    
    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;
}
