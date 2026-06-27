package com.example.manage_activities.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(name = "sender_id", length = 10)
    String senderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User sender;

    @Column(name = "target_label", columnDefinition = "TEXT")
    String targetLabel;

    String title;
    
    @Column(columnDefinition = "TEXT")
    String content;

    @Column(name = "is_read")
    @Builder.Default
    Boolean isRead = false;

    String type; // System, Activity
    LocalDateTime createdAt;
}
