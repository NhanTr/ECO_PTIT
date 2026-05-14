package com.example.manage_activities.repository;

import com.example.manage_activities.entity.Registration;
import com.example.manage_activities.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
    boolean existsByActivityIdAndStudentId(String activityId, String studentId);

    @Query("SELECT r.id FROM Registration r WHERE r.activityId = ?1 AND r.studentId = ?2")
    String findIdByActivityIdAndStudentId(String activityId, String studentId);
}