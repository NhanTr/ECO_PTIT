package com.example.manage_activities.configuration;

import com.example.manage_activities.entity.Room;
import com.example.manage_activities.repository.RoomRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoomDataInitializer {

    @Bean
    ApplicationRunner roomApplicationRunner(RoomRepository roomRepository) {
        return args -> {
            for (String building : new String[]{"A", "B", "C", "D"}) {
                for (int number = 1; number <= 39; number++) {
                    final String currentBuilding = building;
                    final int floor = resolveFloor(number);
                    final String roomNumber = String.format("%02d", number);
                    final String code = currentBuilding + roomNumber;
                    roomRepository.findByCodeIgnoreCase(code).orElseGet(() ->
                            roomRepository.save(Room.builder()
                                    .id(code)
                                    .code(code)
                                    .name("Phong " + code)
                                    .building(currentBuilding)
                                    .floor(floor)
                                    .status("active")
                                    .build()));
                }
            }
        };
    }

    private int resolveFloor(int roomNumber) {
        if (roomNumber <= 9) {
            return 1;
        }
        if (roomNumber <= 19) {
            return 2;
        }
        if (roomNumber <= 29) {
            return 3;
        }
        return 4;
    }
}
