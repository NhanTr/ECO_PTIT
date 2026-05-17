package com.example.manage_activities.repository;

import com.example.manage_activities.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, String> {
    Optional<Attendance> findByRegistrationId(String registrationId);
    List<Attendance> findByRegistrationIdIn(Collection<String> registrationIds);
}