package com.example.manage_activities.repository;

import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.enums.ActivityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, String> {
    List<Activity> findByOrganizerId(String organizerId);
    List<Activity> findByStatus(ActivityStatus status);

    boolean existsById(String id);

    

}
