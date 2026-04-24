package com.example.manage_activities.mapper;

import com.example.manage_activities.dto.request.ProfileRequest;
import com.example.manage_activities.dto.response.ProfileResponse;
import com.example.manage_activities.entity.Profile;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    
    // Convert create request to Activity entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    Profile toEntity(ProfileRequest request);

    // Convert Activity entity to response DTO
    ProfileResponse toDTO(Profile profile);

}
