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
@Table(name = "categories", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"type", "code"})
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Category {
    @Id
    @Column(length = 10)
    String id;

    /** DEPARTMENT | ACTIVITY_TYPE | POINT_TYPE | SPONSOR */
    @Column(nullable = false, length = 32)
    String type;

    @Column(nullable = false, length = 64)
    String code;

    @Column(nullable = false, length = 255)
    String name;

    @Column(length = 500)
    String description;

    @Column(nullable = false, length = 16)
    String status; // active | inactive

    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column
    LocalDateTime updatedAt;
}