package com.example.manage_activities.service;

import com.example.manage_activities.dto.response.RoomResponse;
import com.example.manage_activities.entity.Room;
import com.example.manage_activities.repository.RoomRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomService {
    RoomRepository roomRepository;

    public List<RoomResponse> getRooms() {
        return roomRepository.findAllByOrderByBuildingAscCodeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private RoomResponse toResponse(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .code(room.getCode())
                .name(room.getName())
                .building(room.getBuilding())
                .floor(room.getFloor())
                .status(room.getStatus())
                .build();
    }
}
