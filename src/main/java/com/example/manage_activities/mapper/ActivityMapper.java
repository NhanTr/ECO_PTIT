package com.example.manage_activities.mapper;

import com.example.manage_activities.dto.request.ActivityCreateRequest;
import com.example.manage_activities.dto.response.ActivityResponse;
import com.example.manage_activities.entity.Activity;
import com.example.manage_activities.enums.ActivityStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ActivityMapper {
    
    // Convert create request to Activity entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizerId", ignore = true)
    @Mapping(target = "reviewerId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentParticipants", ignore = true)
    @Mapping(target = "cancelReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Activity toEntity(ActivityCreateRequest request);
    
    // Convert Activity entity to response DTO
    ActivityResponse toDTO(Activity activity);

    default String map(ActivityStatus status) {
        return status == null ? null : status.getValue();
    }
}
