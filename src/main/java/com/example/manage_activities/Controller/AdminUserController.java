package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.UserCreateRequest;
import com.example.manage_activities.dto.request.UserUpdateRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.dto.response.UserResponse;
import com.example.manage_activities.service.UserService;
import com.opencsv.CSVWriter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminUserController {

    UserService userService;

    @PostMapping
    public APIResponse<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("Admin create user: {}", request.getUsername());
        return APIResponse.response(userService.createUser(request));
    }

    @GetMapping
    public APIResponse<List<UserResponse>> searchUsers(
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q) {
        return APIResponse.<List<UserResponse>>builder()
                .result(userService.searchUsers(roleId, status, q))
                .build();
    }

    @GetMapping("/export/csv")
    public void exportUsersAsCsv(
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            HttpServletResponse response) throws IOException {
        log.info("Export users to CSV");
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"users_" + System.currentTimeMillis() + ".csv\"");

        List<UserResponse> users = userService.searchUsers(roleId, status, q);

        try (PrintWriter writer = response.getWriter();
             CSVWriter csvWriter = new CSVWriter(writer)) {
            csvWriter.writeNext(new String[]{"ID", "Username", "Email", "Role", "Status", "Created At"});
            users.forEach(user -> csvWriter.writeNext(new String[]{
                    user.getId(),
                    user.getUsername(),
                    user.getEmail() != null ? user.getEmail() : "",
                    user.getRoleName() != null ? user.getRoleName() : "",
                    user.getStatus() != null ? user.getStatus() : "",
                    user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
            }));
        }
    }

    @GetMapping("/{id}")
    public APIResponse<UserResponse> getUserById(@PathVariable String id) {
        return APIResponse.<UserResponse>builder()
                .result(userService.getUserById(id))
                .build();
    }

    @PutMapping("/{id}")
    public APIResponse<UserResponse> updateUser(
            @PathVariable String id,
            @RequestBody UserUpdateRequest request) {
        return APIResponse.response(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateUser(@PathVariable String id) {
        log.info("Admin deactivate user: {}", id);
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

}
