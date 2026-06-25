package com.example.manage_activities.repository;

import com.example.manage_activities.entity.Attendance;
import com.example.manage_activities.repository.projection.ActivityRegistrationCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, String> {
    Optional<Attendance> findByRegistrationId(String registrationId);
    List<Attendance> findByRegistrationIdIn(Collection<String> registrationIds);

    @Query("""
            SELECT r.activityId AS activityId, COUNT(att) AS registrationCount
            FROM Attendance att
            INNER JOIN Registration r ON r.id = att.registrationId
            WHERE r.activityId IN :activityIds
              AND att.isPresent = true
            GROUP BY r.activityId
            """)
    List<ActivityRegistrationCountProjection> countPresentAttendeesByActivityIds(
            @Param("activityIds") Collection<String> activityIds);
}