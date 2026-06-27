package com.example.manage_activities.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.manage_activities.enums.ReportStatus;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User reviewer;

    @Column(name = "uploaded_by", length = 10)
    String uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User uploader;

    ReportStatus reportStatus;
    String fileUrl;
    String fileType; // Plan, Report, Image
    String originalFileName;
    String contentType;
    Long fileSize;
    LocalDateTime uploadedAt;
    LocalDateTime reviewedAt;

    @Column(columnDefinition = "TEXT")
    String reviewNote;
}
