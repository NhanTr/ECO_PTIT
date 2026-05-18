package com.example.manage_activities.repository;

import com.example.manage_activities.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, String> {

    Profile findByUserId(String userId);

    List<Profile> findByUserIdIn(Collection<String> userIds);

    boolean existsByStudentCode(String studentCode);
}
