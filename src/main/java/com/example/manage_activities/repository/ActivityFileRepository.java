package com.example.manage_activities.repository;

import com.example.manage_activities.entity.ActivityFile;
import com.example.manage_activities.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityFileRepository extends JpaRepository<ActivityFile, String> {
    List<ActivityFile> findByActivityId(String activityId);
    Optional<ActivityFile> findFirstByActivityIdAndFileTypeOrderByUploadedAtDesc(String activityId, String fileType);

    @Query("""
            SELECT f FROM ActivityFile f
            WHERE f.fileType = 'Report'
              AND (:activityId IS NULL OR f.activityId = :activityId)
              AND (:reportStatus IS NULL OR f.reportStatus = :reportStatus)
            ORDER BY f.uploadedAt DESC
            """)
    List<ActivityFile> searchReports(
            @Param("activityId") String activityId,
            @Param("reportStatus") ReportStatus reportStatus);
}
