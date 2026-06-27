package com.example.manage_activities.repository;

import com.example.manage_activities.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    List<Category> findByType(String type);
    List<Category> findByTypeAndStatus(String type, String status);
    Optional<Category> findByTypeAndCode(String type, String code);
    boolean existsByTypeAndCode(String type, String code);
}