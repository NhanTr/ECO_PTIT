package com.example.manage_activities.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.manage_activities.enums.ActivityStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User reviewer;

    @Column(nullable = false)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "room_id", length = 10)
    String roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Room room;

    @Column(name = "room_code", length = 10)
    String roomCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_code", referencedColumnName = "code", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Room roomByCode;

    String location;
    LocalDateTime startTime;
    LocalDateTime endTime;
    LocalDateTime registrationDeadline;

    @Column(precision = 15, scale = 2)
    BigDecimal budget;

    String sponsor;
    String targetAudience;

    @Column(columnDefinition = "TEXT")
    String purpose;

    Integer trainingPoints;
    Integer maxParticipants;

    @Builder.Default
    Integer currentParticipants = 0;

    ActivityStatus status;

    @Column(columnDefinition = "TEXT")
    String cancelReason;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    String rejectReason;

    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;
}
