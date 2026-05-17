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

import java.time.LocalDateTime;

@Entity
@Table(name = "system_configs")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemConfig {
    @Id
    @Column(name = "config_key", length = 100)
    String key;

    @Column(name = "config_value", nullable = false)
    String value;

    String valueType;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "updated_by", length = 10)
    String updatedBy;

    LocalDateTime updatedAt;
}
