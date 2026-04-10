package com.example.manage_activities.repository;

import com.example.manage_activities.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
