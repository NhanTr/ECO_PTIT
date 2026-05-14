package com.example.manage_activities.entity;

import com.example.manage_activities.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "activity_files")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActivityFile {
    @Id
    @Column(length = 10)
    String id;

    @Column(name = "activity_id", length = 10)
    String activityId;

    @Column(name = "reviewer_id", length = 10)
    String reviewerId;

    ReportStatus reportStatus;
    String fileUrl;
    String fileType; // Plan, Report, Image
}
