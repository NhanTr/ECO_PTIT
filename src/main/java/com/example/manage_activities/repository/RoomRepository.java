package com.example.manage_activities.repository;

import com.example.manage_activities.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {
    List<Room> findAllByOrderByBuildingAscCodeAsc();

    Optional<Room> findByCodeIgnoreCase(String code);
}
