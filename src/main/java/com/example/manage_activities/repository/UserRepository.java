package com.example.manage_activities.repository;

import com.example.manage_activities.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    Optional<User> findById(String id);

    boolean existsById(String id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRoleId(Integer roleId);

    List<User> findByRoleIdAndIdIn(Integer roleId, List<String> ids);

    List<User> findByIdIn(List<String> ids);

    @Query("""
            SELECT u FROM User u
            WHERE (:roleId IS NULL OR u.roleId = :roleId)
              AND (:status IS NULL OR :status = '' OR LOWER(u.status) = LOWER(:status))
              AND (
                    :q IS NULL OR :q = ''
                    OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
            ORDER BY u.createdAt DESC
            """)
    List<User> searchUsers(
            @Param("roleId") Integer roleId,
            @Param("status") String status,
            @Param("q") String q);

    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN Profile p ON p.userId = u.id
            WHERE (u.status IS NULL OR LOWER(u.status) = 'active')
              AND (:roleId IS NULL OR u.roleId = :roleId)
              AND (:department IS NULL OR :department = ''
                   OR (p.department IS NOT NULL AND LOWER(TRIM(p.department)) = LOWER(TRIM(:department))))
              AND (:className IS NULL OR :className = ''
                   OR (p.className IS NOT NULL AND LOWER(TRIM(p.className)) = LOWER(TRIM(:className))))
            """)
    List<User> findBroadcastRecipients(
            @Param("roleId") Integer roleId,
            @Param("department") String department,
            @Param("className") String className);
}
