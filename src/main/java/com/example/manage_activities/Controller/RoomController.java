package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.RoomResponse;
import com.example.manage_activities.service.RoomService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomController {
    RoomService roomService;

    @GetMapping
    public APIResponse<List<RoomResponse>> getRooms() {
        return APIResponse.<List<RoomResponse>>builder()
                .result(roomService.getRooms())
                .build();
    }
}
