package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.ChangePasswordRequest;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public user self-service endpoints. Admin user management moved to {@link AdminUserController}.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {

    UserService userService;

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public APIResponse<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Change password request for user ID: {}", userId);
        userService.changePassword(userId, request);
        return APIResponse.<Void>builder()
                .result(null)
                .message("Password was changed successfully")
                .build();
    }
}
