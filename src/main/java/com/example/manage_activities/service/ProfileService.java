package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.ProfileRequest;
import com.example.manage_activities.dto.response.ProfileResponse;
import com.example.manage_activities.repository.ProfileRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.manage_activities.entity.Profile;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProfileService {
    
    ProfileRepository profileRepository;

    public ProfileResponse getMyProfile() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Getting profile for user: {}", userId);

        Profile profile = profileRepository.findByUserId(userId);
        if (profile == null) {
            throw new AppException(ErrorCode.DONT_EXIST_PROFILE);
        }

        return toResponse(profile);
    }

    public void createProfile(ProfileRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Creating profile for user: {}", userId);

        // Check if profile already exists for this user
        Profile existingProfile = profileRepository.findByUserId(userId);
        
        if (existingProfile != null) {
            log.info("Profile already exists for user: {}", userId);
            throw new AppException(ErrorCode.EXIST_PROFILE);
        }

        Profile profile = new Profile();
        profile.setId(generateProfileId());
        profile.setUserId(userId);
        profile.setFullName(request.getFullName());
        profile.setPhone(request.getPhone());
        profile.setDepartment(request.getDepartment());
        profile.setStudentCode(request.getStudentCode());
        profile.setAvatarUrl(request.getAvatarUrl());

        // Save new profile
        profileRepository.save(profile);
        log.info("Profile created successfully for user: {}", userId);
    }


    public void updateProfile(ProfileRequest request) {
        
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Updating profile for user: {}", userId);

        // Tìm profile theo userId, nếu không có thì tạo mới
        Profile existingProfile = profileRepository.findByUserId(userId);
        
        if (existingProfile == null) {
            log.info("Profile not found for user: {}, creating new one", userId);
            throw new AppException(ErrorCode.DONT_EXIST_PROFILE);
        }

        existingProfile.setFullName(request.getFullName());
        existingProfile.setPhone(request.getPhone());
        existingProfile.setDepartment(request.getDepartment());
        existingProfile.setStudentCode(request.getStudentCode());
        existingProfile.setAvatarUrl(request.getAvatarUrl());
        
        // Save updated profile
        profileRepository.save(existingProfile);
        log.info("Profile saved successfully for user: {}", userId);
    }
    

    /**
     * Generate profile ID 10 characters
     */

    private String generateProfileId() {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        while (profileRepository.existsById(id)) {
            id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        }
        return id;
    }

    private ProfileResponse toResponse(Profile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .studentCode(profile.getStudentCode())
                .department(profile.getDepartment())
                .phone(profile.getPhone())
                .avatarUrl(profile.getAvatarUrl())
                .build();
    }
}
