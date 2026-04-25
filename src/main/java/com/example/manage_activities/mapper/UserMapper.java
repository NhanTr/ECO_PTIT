package com.example.manage_activities.mapper;

import com.example.manage_activities.dto.request.UserCreateRequest;
import com.example.manage_activities.dto.request.UserUpdateRequest;
import com.example.manage_activities.dto.response.UserResponse;
import com.example.manage_activities.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    
    // Convert create request to User entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "password", target = "passwordHash")
    User toEntity(UserCreateRequest request);
    
    // Convert User entity to response DTO
    UserResponse toDTO(User user);
    
    // Update user entity with data from update request
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget User user, UserUpdateRequest request);

    
}


