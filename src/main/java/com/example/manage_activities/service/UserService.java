package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.ChangePasswordRequest;
import com.example.manage_activities.dto.request.UserCreateRequest;
import com.example.manage_activities.dto.request.UserUpdateRequest;
import com.example.manage_activities.dto.response.UserResponse;
import com.example.manage_activities.entity.User;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.mapper.UserMapper;
import com.example.manage_activities.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SystemLogService systemLogService;
    
    /**
     * Create a new user
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating new user with username: {}", request.getUsername());
        
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        
        // Check if email already exists
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }
        
        // Create new user with ID and creation timestamp
        User user = userMapper.toEntity(request);
        user.setId(generateUserId());
        user.setCreatedAt(LocalDateTime.now());

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        
        User savedUser = userRepository.save(user);
        systemLogService.logAction(getCurrentUserId(), "CREATE_USER", "users", null,
                "userId=" + savedUser.getId() + ", roleId=" + savedUser.getRoleId());
        log.info("User created successfully with ID: {}", savedUser.getId());
        
        return userMapper.toDTO(savedUser);
    }
    
    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(String id) {
        log.info("Fetching user with ID: {}", id);

        if (!userRepository.existsById(id)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        return userRepository.findById(id)
                .map(userMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
    }
    
    /**
     * Get all users
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get user by username
     */
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByUsername(String username) {
        log.info("Fetching user with username: {}", username);
        return userRepository.findByUsername(username)
                .map(userMapper::toDTO);
    }
    
    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByEmail(String email) {
        log.info("Fetching user with email: {}", email);
        return userRepository.findByEmail(email)
                .map(userMapper::toDTO);
    }
    
    /**
     * Update user information
     */
    @Transactional
    public UserResponse updateUser(String id, UserUpdateRequest request) {
        log.info("Updating user with ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        
        // Check if email is being updated and is already taken
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new RuntimeException("Email already exists: " + request.getEmail());
            }
        }
        
        userMapper.updateEntity(user, request);
        User updatedUser = userRepository.save(user);
        systemLogService.logAction(getCurrentUserId(), "UPDATE_USER", "users",
                "userId=" + id,
                "email=" + updatedUser.getEmail() + ", status=" + updatedUser.getStatus() + ", roleId=" + updatedUser.getRoleId());
        log.info("User updated successfully with ID: {}", updatedUser.getId());
        
        return userMapper.toDTO(updatedUser);
    }

    @Transactional
    public UserResponse assignPrimaryRole(String userId, Integer roleId) {
        validateRoleId(roleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Integer oldRoleId = user.getRoleId();

        user.setRoleId(roleId);
        User savedUser = userRepository.save(user);
        systemLogService.logAction(getCurrentUserId(), "ASSIGN_PRIMARY_ROLE", "users",
                "userId=" + userId + ", roleId=" + oldRoleId,
                "userId=" + userId + ", roleId=" + roleId);
        return userMapper.toDTO(savedUser);
    }

    @Transactional
    public UserResponse revokePrimaryRole(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Integer oldRoleId = user.getRoleId();

        user.setRoleId(4);
        User savedUser = userRepository.save(user);
        systemLogService.logAction(getCurrentUserId(), "REVOKE_PRIMARY_ROLE", "users",
                "userId=" + userId + ", roleId=" + oldRoleId,
                "userId=" + userId + ", roleId=4");
        return userMapper.toDTO(savedUser);
    }
    
    /**
     * Delete user
     */
    @Transactional
    public void deleteUser(String id) {
        log.info("Deleting user with ID: {}", id);
        
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with ID: " + id);
        }
        
        userRepository.deleteById(id);
        systemLogService.logAction(getCurrentUserId(), "DELETE_USER", "users", "userId=" + id, null);
        log.info("User deleted successfully with ID: {}", id);
    }
    
    /**
     * Generate unique user ID (first 10 characters of UUID)
     */
    private String generateUserId() {
        String id;
        do {
            id = UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        } while (userRepository.existsById(id)); // Check for uniqueness
        return id;
    }

    /**
     * Change user password
    */

    @Transactional
    public void changePassword(String userId,ChangePasswordRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        log.info("Changing password for user: {}", username);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Check if old password matches
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Old password is incorrect");
        }

        // Check if new password and confirm password match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        systemLogService.logAction(userId, "CHANGE_PASSWORD", "users", "userId=" + userId, "passwordChanged=true");
        log.info("Password changed successfully for user: {}", username);
    }

    private void validateRoleId(Integer roleId) {
        if (roleId == null || roleId < 1 || roleId > 4) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
    }

    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}
