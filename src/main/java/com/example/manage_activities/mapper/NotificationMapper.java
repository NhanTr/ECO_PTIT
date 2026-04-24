package com.example.manage_activities.mapper;

import com.example.manage_activities.dto.request.NotificationRequest;
import com.example.manage_activities.entity.Notification;
import com.example.manage_activities.dto.response.NotificationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "receiverId", ignore = true)
    @Mapping(target = "isRead", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Notification toEntity(NotificationRequest request); 

    NotificationResponse toDTO(Notification notification);
}
