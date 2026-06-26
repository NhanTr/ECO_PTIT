package com.example.manage_activities.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "rooms")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Room {
    @Id
    @Column(length = 10)
    String id;

    @Column(nullable = false, unique = true, length = 10)
    String code;

    @Column(nullable = false)
    String name;

    @Column(nullable = false, length = 1)
    String building;

    @Column(nullable = false)
    Integer floor;

    @Column(nullable = false)
    String status;
}
