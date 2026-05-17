package com.example.manage_activities.repository;

import com.example.manage_activities.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, String> {
    @Query("""
            SELECT l FROM SystemLog l
            WHERE (:userId IS NULL OR l.userId = :userId)
              AND (:action IS NULL OR LOWER(l.action) LIKE LOWER(CONCAT('%', :action, '%')))
              AND (:tableAffected IS NULL OR LOWER(l.tableAffected) LIKE LOWER(CONCAT('%', :tableAffected, '%')))
              AND (:fromTime IS NULL OR l.createdAt >= :fromTime)
              AND (:toTime IS NULL OR l.createdAt <= :toTime)
            """)
    Page<SystemLog> searchLogs(
            @Param("userId") String userId,
            @Param("action") String action,
            @Param("tableAffected") String tableAffected,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            Pageable pageable);
}
