package com.example.manage_activities.repository;

import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.enums.ActivityStatus;
import com.example.manage_activities.repository.projection.ActivityStatusCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
            SELECT COUNT(a) > 0 FROM Activity a
            WHERE a.organizerId = :organizerId
              AND a.id <> :activityId
              AND a.status IN :statuses
              AND a.startTime IS NOT NULL
              AND a.endTime IS NOT NULL
              AND :startTime IS NOT NULL
              AND :endTime IS NOT NULL
              AND a.startTime < :endTime
              AND a.endTime > :startTime
            """)
    boolean existsOverlappingOrganizerActivity(
            @Param("organizerId") String organizerId,
            @Param("activityId") String activityId,
            @Param("statuses") Collection<ActivityStatus> statuses,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

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

    /**
     * Finds activities in approved/ongoing lifecycle states that share the same managed room
     * and have an overlapping time window with the given activity.
     */
    @Query("""
            SELECT a FROM Activity a
            WHERE a.status IN :statuses
              AND a.id <> :activityId
              AND a.roomId IS NOT NULL
              AND a.startTime IS NOT NULL
              AND a.endTime IS NOT NULL
              AND :startTime IS NOT NULL
              AND :endTime IS NOT NULL
              AND a.startTime < :endTime
              AND a.endTime > :startTime
              AND a.roomId = :roomId
            """)
    List<Activity> findScheduleConflicts(
            @Param("activityId") String activityId,
            @Param("statuses") Collection<ActivityStatus> statuses,
            @Param("roomId") String roomId,
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Activity a
            SET a.status = :ongoingStatus
            WHERE a.status = :approvedStatus
              AND a.startTime IS NOT NULL
              AND a.startTime <= :now
              AND (a.endTime IS NULL OR a.endTime > :now)
            """)
    int startDueActivities(
            @Param("approvedStatus") ActivityStatus approvedStatus,
            @Param("ongoingStatus") ActivityStatus ongoingStatus,
            @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Activity a
            SET a.status = :closedStatus
            WHERE a.status IN :activeStatuses
              AND a.endTime IS NOT NULL
              AND a.endTime <= :now
            """)
    int closeExpiredActivities(
            @Param("activeStatuses") Collection<ActivityStatus> activeStatuses,
            @Param("closedStatus") ActivityStatus closedStatus,
            @Param("now") LocalDateTime now);

    boolean existsById(String id);

    @Query("""
            SELECT a FROM Activity a
            WHERE (:fromTime IS NULL OR a.startTime >= :fromTime)
              AND (:toTime IS NULL OR a.startTime <= :toTime)
            ORDER BY a.startTime ASC, a.title ASC
            """)
    List<Activity> findForStatisticsReport(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime);

    @Query("""
            SELECT a.status AS status, COUNT(a) AS count
            FROM Activity a
            WHERE (:fromTime IS NULL OR a.startTime >= :fromTime)
              AND (:toTime IS NULL OR a.startTime <= :toTime)
            GROUP BY a.status
            """)
    List<ActivityStatusCountProjection> countActivitiesGroupByStatus(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime);

    /**
     * Đếm hoạt động bắt đầu trong khoảng thời gian (dùng cho thống kê theo học kỳ).
     */
    @Query("""
            SELECT COUNT(a) FROM Activity a
            WHERE (:fromTime IS NULL OR a.startTime >= :fromTime)
              AND (:toTime IS NULL OR a.startTime <= :toTime)
            """)
    long countActivitiesInPeriod(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime);
}
