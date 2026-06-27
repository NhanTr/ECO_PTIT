package com.example.manage_activities.entity;

import com.example.manage_activities.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Entity
@Table(name = "registrations")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Registration {
    @Id
    @Column(length = 10)
    String id;

    @Column(name = "activity_id", length = 10)
    String activityId;

    @Column(name = "student_id", length = 10)
    String studentId;

    @Column(name = "approved_by", length = 10)
    String approvedBy;

    RegistrationStatus status;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    String rejectReason;

    LocalDateTime approvedAt;
    LocalDateTime cancelledAt;
    LocalDateTime createdAt;
}
