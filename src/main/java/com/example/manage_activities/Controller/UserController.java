package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.UserCreateRequest;
import com.example.manage_activities.dto.request.UserUpdateRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.UserResponse;
import com.example.manage_activities.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.opencsv.CSVWriter;


import java.io.IOException;
import java.io.PrintWriter;
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
    public APIResponse<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("Create user request received for username: {}", request.getUsername());
        UserResponse response = userService.createUser(request);
        return APIResponse.response(response);
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

    

    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public void getAllUsersAsCSV(HttpServletResponse response) throws IOException {
        log.info("Export users to CSV");
        
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", 
            "attachment; filename=\"users_" + System.currentTimeMillis() + ".csv\"");
        
        List<UserResponse> users = userService.getAllUsers();
        
        try (PrintWriter writer = response.getWriter();
            CSVWriter csvWriter = new CSVWriter(writer)) {
            
            // Header
            csvWriter.writeNext(new String[]{"ID", "Username", "Email", "Full Name", "Created Date"});
            
            // Data
            users.forEach(user -> csvWriter.writeNext(new String[]{
                user.getId(),
                user.getUsername(),
                user.getEmail()
            }));
        }
    }
}
