package com.example.manage_activities.Controller;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import org.springframework.web.bind.annotation.*;

import com.example.manage_activities.service.NotificationService;
import com.example.manage_activities.dto.request.NotificationRequest;
import com.example.manage_activities.dto.response.NotificationResponse;
import com.example.manage_activities.dto.response.APIResponse;




@RestController
@RequestMapping("/auth")  
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j 
public class NotificationController {

    NotificationService notificationService;

    /**
     * Send notification for an activity
     * POST /api/v1/notifications/{activityId}?message=Your+message+here
     * @param activityId
     * @param message
     */

    @PostMapping("/{activityId}")
    public APIResponse<NotificationResponse> sendNotification(@PathVariable String activityId, @RequestBody NotificationRequest request) {
        notificationService.sendNotification(activityId, request);

        return APIResponse.response(null);
    }


}
