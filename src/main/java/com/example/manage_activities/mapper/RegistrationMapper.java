package com.example.manage_activities.mapper;

import com.example.manage_activities.dto.request.RegistrationRequest;
import com.example.manage_activities.dto.response.RegistrationResponse;
import com.example.manage_activities.entity.Registration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RegistrationMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "studentId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Registration toEntity(RegistrationRequest request);
    
    RegistrationResponse toDTO(Registration registration);
}
