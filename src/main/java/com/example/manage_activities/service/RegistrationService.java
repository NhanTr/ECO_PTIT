package com.example.manage_activities.service;


import com.example.manage_activities.dto.response.RegistrationResponse;
import com.example.manage_activities.entity.Registration;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.mapper.RegistrationMapper;
import com.example.manage_activities.repository.RegistrationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RegistrationService {
    
    RegistrationRepository registrationRepository;
    RegistrationMapper registrationMapper;
    
    /**
     * Register user for activity
     */
    public void registerActivity(String activityId) {
        log.info("Registering user for activity: {}", activityId);
        
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Check if already registered
        boolean alreadyRegistered = registrationRepository.existsByActivityIdAndStudentId(activityId, userId);
        
        if (alreadyRegistered) {
            throw new AppException(ErrorCode.EXIST_REGISTRATIONS);
        }
        
        Registration registration = new Registration();
        registration.setId(generateRegistrationId());
        registration.setActivityId(activityId);
        registration.setStudentId(userId);
        registration.setStatus("Registered");
        registration.setCreatedAt(LocalDateTime.now());
        
        registrationRepository.save(registration);
    }
    
    /**
     * Unregister user from activity
     */
    public void unregisterActivity(String activityId) {
        log.info("Unregistering user from activity: {}", activityId);
        
        String studentId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("Searching registration - activityId: {}, studentId: {}", activityId, studentId);
        
        String registrationId = registrationRepository.findIdByActivityIdAndStudentId(activityId, studentId);
        
        if (registrationId == null) {
            log.warn("Registration not found for activityId: {}, studentId: {}", activityId, studentId);
            throw new AppException(ErrorCode.NO_REGISTRATIONS);
        }

        registrationRepository.deleteById(registrationId);

        log.info("User unregistered successfully from activity: {}", activityId);
    }
    
    /**
     * Get user's registrations
     */
    public List<RegistrationResponse> getUserRegistrations() {
        log.info("Getting user registrations");
        
        String studentId = SecurityContextHolder.getContext().getAuthentication().getName();
        
        List<Registration> registrations = registrationRepository.findByStudentId(studentId);

        if (registrations.isEmpty()) {
            log.info("No registrations found for user: {}", studentId);
            throw new AppException(ErrorCode.NO_REGISTRATIONS);
        } else {
            log.info("Found {} registrations for user: {}", registrations.size(), studentId);
        }

        return registrations.stream()
                .map(registrationMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get activity registrations
     */
    public List<RegistrationResponse> getActivityRegistrations(String activityId) {
        log.info("Getting registrations for activity: {}", activityId);
        
        return registrationRepository.findByActivityId(activityId)
                .stream()
                .map(registrationMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get registration count for activity
     */
    public Long getActivityRegistrationCount(String activityId) {
        log.info("Getting registration count for activity: {}", activityId);
        return registrationRepository.countByActivityId(activityId);
    }
    /**
     * Get user IDs for activityID 
     * @return
     */

    public List<String> getPaticipantID(String activityId) {
        List<Registration> registrations = registrationRepository.findByActivityId(activityId);
        
        List<String> userList = new ArrayList<>();
        for (Registration reg : registrations) {
            userList.add(reg.getStudentId());
        }
        return userList;
    }

    /**
     * Generate a unique ID for the registration
      */
    private String generateRegistrationId() {
        String id = UUID.randomUUID().toString().substring(0, 10);
        while (registrationRepository.existsById(id)) {
            id = UUID.randomUUID().toString().substring(0, 10);
        }
        return id;
    }
}
