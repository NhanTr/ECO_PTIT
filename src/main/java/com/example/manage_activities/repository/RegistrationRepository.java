package com.example.manage_activities.repository;

import com.example.manage_activities.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, String> {
    List<Registration> findByActivityId(String activityId);
    List<Registration> findByStudentId(String studentId);
    Optional<Registration> findByActivityIdAndStudentId(String activityId, String studentId);
    Long countByActivityId(String activityId);
    boolean existsByActivityIdAndStudentId(String activityId, String studentId);
}
