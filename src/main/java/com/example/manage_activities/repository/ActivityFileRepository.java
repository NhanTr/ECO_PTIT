package com.example.manage_activities.repository;

import com.example.manage_activities.entity.ActivityFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityFileRepository extends JpaRepository<ActivityFile, String> {
    List<ActivityFile> findByActivityId(String activityId);
    Optional<ActivityFile> findFirstByActivityIdAndFileTypeOrderByUploadedAtDesc(String activityId, String fileType);
}