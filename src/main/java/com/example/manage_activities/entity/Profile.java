package com.example.manage_activities.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "profiles")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Profile {
    @Id
    @Column(length = 10)
    String id;

    @Column(name = "user_id", length = 10, unique = true)
    String userId;

    @Column(name = "full_name")
    String fullName;

    @Column(name = "student_code", unique = true)
    String studentCode;

    String department;

    @Column(name = "class_name")
    String className;

    String phone;

    @Column(name = "avatar_url")
    String avatarUrl;
}