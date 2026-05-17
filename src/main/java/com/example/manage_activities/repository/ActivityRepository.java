package com.example.manage_activities.repository;

import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.enums.ActivityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, String> {
    List<Activity> findByOrganizerId(String organizerId);
    List<Activity> findByStatus(ActivityStatus status);

    @Query("""
            SELECT a FROM Activity a
            WHERE a.status IN :statuses
              AND (:keyword IS NULL
                   OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:location IS NULL OR LOWER(a.location) LIKE LOWER(CONCAT('%', :location, '%')))
              AND (:sponsor IS NULL OR LOWER(a.sponsor) LIKE LOWER(CONCAT('%', :sponsor, '%')))
              AND (:fromTime IS NULL OR a.startTime >= :fromTime)
              AND (:toTime IS NULL OR a.startTime <= :toTime)
            """)
    Page<Activity> searchActivities(
            @Param("statuses") Collection<ActivityStatus> statuses,
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("sponsor") String sponsor,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            Pageable pageable);

    @Query("""
            SELECT a FROM Activity a
            WHERE a.status = :status
              AND a.id <> :activityId
              AND a.startTime IS NOT NULL
              AND a.endTime IS NOT NULL
              AND :startTime IS NOT NULL
              AND :endTime IS NOT NULL
              AND a.startTime < :endTime
              AND a.endTime > :startTime
            """)
    List<Activity> findApprovedOverlappingActivities(
            @Param("activityId") String activityId,
            @Param("status") ActivityStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("""
            SELECT a FROM Activity a
            WHERE a.status IN :statuses
              AND (:keyword IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:location IS NULL OR LOWER(a.location) LIKE LOWER(CONCAT('%', :location, '%')))
              AND (:fromTime IS NULL OR a.startTime >= :fromTime)
              AND (:toTime IS NULL OR a.startTime <= :toTime)
            """)
    Page<Activity> searchAvailableActivities(
            @Param("statuses") Collection<ActivityStatus> statuses,
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            Pageable pageable);

    boolean existsById(String id);
}
