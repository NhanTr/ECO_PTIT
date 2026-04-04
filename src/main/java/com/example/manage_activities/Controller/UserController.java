package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.UserCreateRequest;
import com.example.manage_activities.dto.request.UserUpdateRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.UserResponse;
import com.example.manage_activities.service.UserService;
import com.example.manage_activities.dto.request.ChangePasswordRequest;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    
    UserService userService;
    
    /**
     * Create a new user
     * POST /api/v1/users
     * Only ADMIN can create users
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("Create user request received for username: {}", request.getUsername());
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get all users
     * GET /api/v1/users
     * Only ADMIN can view all users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public APIResponse<List<UserResponse>> getAllUsers() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("Authenticated user: {}", authentication.getName());
        log.info("Get all users request received {}", authentication.getAuthorities());

        List<UserResponse> users = userService.getAllUsers();
        return APIResponse.<List<UserResponse>>builder()
                .result(users)
                .build();
    }
    
    
    /**
     * Get user by ID
     * GET /api/v1/users/{id}
     * Only ADMIN can view user details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public APIResponse<UserResponse> getUserById(@PathVariable String id) {
        log.info("Get user request received for ID: {}", id);
        UserResponse user = userService.getUserById(id);
        return APIResponse.<UserResponse>builder()
                .result(user)
                .build();
    }
    
    /**
     * Update user
     * PUT /api/v1/users/{id}
     * Only ADMIN can update users
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable String id, @RequestBody UserUpdateRequest request) {
        log.info("Update user request received for ID: {}", id);
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete user
     * DELETE /api/v1/users/{id}
     * Only ADMIN can delete users
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        log.info("Delete user request received for ID: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Change user password
     * POST /api/v1/users/change-password
     * Any authenticated user can change their own password
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public APIResponse<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        log.info("Change password request received for user ID: {}", userId);

        userService.changePassword(userId, request);
        return APIResponse.<Void>builder()
                .result(null)
                .message("Password was changed successfully")
                .build();
    }
}
