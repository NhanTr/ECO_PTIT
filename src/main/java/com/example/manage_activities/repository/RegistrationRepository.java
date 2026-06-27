package com.example.manage_activities.repository;

import com.example.manage_activities.entity.Registration;
import com.example.manage_activities.enums.ActivityStatus;
import com.example.manage_activities.enums.RegistrationStatus;
import com.example.manage_activities.repository.projection.ActivityRegistrationCountProjection;
import com.example.manage_activities.repository.projection.StudentStatisticsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, String> {
    List<Registration> findByActivityId(String activityId);
    List<Registration> findByStudentId(String studentId);
    List<Registration> findByStudentIdOrderByCreatedAtDesc(String studentId);
    Optional<Registration> findByActivityIdAndStudentId(String activityId, String studentId);
    Long countByActivityId(String activityId);
    Long countByActivityIdAndStatusIn(String activityId, Collection<RegistrationStatus> statuses);
    List<Registration> findByActivityIdIn(Collection<String> activityIds);
    boolean existsByActivityIdAndStudentId(String activityId, String studentId);

    @Query("SELECT r.id FROM Registration r WHERE r.activityId = ?1 AND r.studentId = ?2")
    String findIdByActivityIdAndStudentId(String activityId, String studentId);

    @Query("""
            SELECT r.activityId AS activityId, COUNT(r) AS registrationCount
            FROM Registration r
            WHERE r.activityId IN :activityIds
              AND r.status IN :statuses
            GROUP BY r.activityId
            """)
    List<ActivityRegistrationCountProjection> countRegistrationsByActivityIds(
            @Param("activityIds") Collection<String> activityIds,
            @Param("statuses") Collection<RegistrationStatus> statuses);

    @Query("""
            SELECT r
            FROM Registration r, Activity a
            WHERE r.studentId = :studentId
              AND a.id = r.activityId
              AND r.activityId <> :activityId
              AND r.status IN :registrationStatuses
              AND a.status IN :activityStatuses
              AND a.startTime IS NOT NULL
              AND a.endTime IS NOT NULL
              AND :startTime IS NOT NULL
              AND :endTime IS NOT NULL
              AND a.startTime < :endTime
              AND a.endTime > :startTime
            """)
    List<Registration> findStudentScheduleConflicts(
            @Param("studentId") String studentId,
            @Param("activityId") String activityId,
            @Param("registrationStatuses") Collection<RegistrationStatus> registrationStatuses,
            @Param("activityStatuses") Collection<ActivityStatus> activityStatuses,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("""
            SELECT u.id AS studentId,
                   p.studentCode AS studentCode,
                   p.fullName AS fullName,
                   p.className AS className,
                   p.department AS department,
                   COUNT(DISTINCT CASE WHEN att.isPresent = true THEN r.activityId END) AS participatedActivityCount,
                   COALESCE(SUM(CASE WHEN att.isPresent = true THEN att.earnedPoints ELSE 0 END), 0) AS totalEarnedPoints
            FROM User u
            INNER JOIN Profile p ON p.userId = u.id
            INNER JOIN Registration r ON r.studentId = u.id AND r.status = com.example.manage_activities.enums.RegistrationStatus.APPROVED
            INNER JOIN Activity a ON a.id = r.activityId
            LEFT JOIN Attendance att ON att.registrationId = r.id
            WHERE u.roleId = :studentRoleId
              AND (:fromTime IS NULL OR a.startTime >= :fromTime)
              AND (:toTime IS NULL OR a.startTime <= :toTime)
              AND (:department IS NULL OR :department = '' OR LOWER(TRIM(p.department)) = LOWER(TRIM(:department)))
              AND (:className IS NULL OR :className = '' OR LOWER(TRIM(p.className)) = LOWER(TRIM(:className)))
            GROUP BY u.id, p.studentCode, p.fullName, p.className, p.department
            HAVING COUNT(DISTINCT CASE WHEN att.isPresent = true THEN r.activityId END) > 0
            ORDER BY p.studentCode ASC, p.fullName ASC
            """)
    List<StudentStatisticsProjection> aggregateStudentStatistics(
            @Param("studentRoleId") Integer studentRoleId,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("department") String department,
            @Param("className") String className);
}
