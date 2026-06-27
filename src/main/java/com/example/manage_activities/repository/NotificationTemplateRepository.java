package com.example.manage_activities.repository;

import com.example.manage_activities.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {
    List<NotificationTemplate> findByChannelCode(String channelCode);
    List<NotificationTemplate> findByStatus(String status);
    Optional<NotificationTemplate> findByChannelCodeAndCode(String channelCode, String code);
    boolean existsByChannelCodeAndCode(String channelCode, String code);
}