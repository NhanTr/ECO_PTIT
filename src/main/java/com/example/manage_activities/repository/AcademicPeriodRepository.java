package com.example.manage_activities.repository;

import com.example.manage_activities.entity.AcademicPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, String> {
    List<AcademicPeriod> findByAcademicYear(String academicYear);
    List<AcademicPeriod> findByStatus(String status);
    Optional<AcademicPeriod> findByAcademicYearAndSemester(String academicYear, Integer semester);
    boolean existsByAcademicYearAndSemester(String academicYear, Integer semester);
}