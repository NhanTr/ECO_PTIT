package com.example.manage_activities.mapper;

import com.example.manage_activities.dto.request.RegistrationRequest;
import com.example.manage_activities.dto.response.RegistrationResponse;
import com.example.manage_activities.entity.Registration;
import com.example.manage_activities.enums.RegistrationStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RegistrationMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "studentId", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approver", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "rejectReason", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Registration toEntity(RegistrationRequest request);
    
    @Mapping(target = "attendanceId", ignore = true)
    @Mapping(target = "isPresent", ignore = true)
    @Mapping(target = "checkInTime", ignore = true)
    @Mapping(target = "earnedPoints", ignore = true)
    @Mapping(target = "studentName", ignore = true)
    @Mapping(target = "studentEmail", ignore = true)
    @Mapping(target = "studentCode", ignore = true)
    @Mapping(target = "className", ignore = true)
    @Mapping(target = "department", ignore = true)
    RegistrationResponse toDTO(Registration registration);

    default String map(RegistrationStatus status) {
        return status == null ? null : status.getValue();
    }
}
