package com.example.manage_activities.Controller;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.AccessLevel;
import org.springframework.web.bind.annotation.*;

import com.example.manage_activities.dto.request.ProfileRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.ProfileResponse;
import com.example.manage_activities.service.ProfileService;




@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProfileController {

    ProfileService profileService;

    @GetMapping
    public APIResponse<ProfileResponse> getMyProfile() {
        return APIResponse.<ProfileResponse>builder()
                .result(profileService.getMyProfile())
                .build();
    }
    
    /**
     * Update user profile
     * PUT /api/v1/profile
     * Only authenticated users can update their profile
     */
    
    @PostMapping
    public APIResponse<Void> createProfile(@RequestBody ProfileRequest request) {
        profileService.createProfile(request);
        return APIResponse.response(null);
    }
    
    @PutMapping
    public APIResponse<Void> updateProfile(@RequestBody ProfileRequest request) {
        profileService.updateProfile(request);

        return APIResponse.<Void>builder()
                .result(null)
                .build();
    }
    
}
