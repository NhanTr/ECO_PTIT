package com.example.manage_activities.repository;

import com.example.manage_activities.entity.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, String> {
    Optional<NotificationChannel> findByCode(String code);
    boolean existsByCode(String code);
    List<NotificationChannel> findByStatus(String status);
}