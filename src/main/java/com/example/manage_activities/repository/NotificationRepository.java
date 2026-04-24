package com.example.manage_activities.repository;

import org.springframework.stereotype.Repository;
import com.example.manage_activities.entity.Notification;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByReceiverId(String receiverId);
}
