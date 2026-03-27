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
@Table(name = "attendance")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Attendance {
    @Id
    @Column(length = 10)
    String id;

    @Column(name = "registration_id", length = 10)
    String registrationId;

    @Column(name = "check_in_time")
    LocalDateTime checkInTime;

    @Column(name = "is_present")
    @Builder.Default
    Boolean isPresent = false;

    Integer earnedPoints;
}