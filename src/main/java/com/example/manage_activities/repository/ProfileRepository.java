package com.example.manage_activities.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.manage_activities.entity.Profile;
import org.springframework.stereotype.Repository;


@Repository
public interface ProfileRepository extends JpaRepository<Profile, String> {

    Profile findByUserId(String userId);

    boolean existsByStudentCode(String studentCode);
}
