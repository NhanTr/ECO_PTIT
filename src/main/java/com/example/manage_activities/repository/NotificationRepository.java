package com.example.manage_activities.repository;

import com.example.manage_activities.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
	List<Notification> findByReceiverIdOrderByCreatedAtDesc(String receiverId);
}

