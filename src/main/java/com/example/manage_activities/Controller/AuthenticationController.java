package com.example.manage_activities.Controller;

import com.example.manage_activities.dto.request.AuthenticationRequest;
import com.example.manage_activities.dto.request.ChangePasswordRequest;
import com.example.manage_activities.dto.request.IntrospectRequest;
import com.example.manage_activities.dto.request.RefreshTokenRequest;
import com.example.manage_activities.dto.response.AuthenticationResponse;
import com.example.manage_activities.dto.response.IntrospectResponse;
import com.example.manage_activities.dto.response.APIResponse;
import com.example.manage_activities.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.example.manage_activities.service.UserService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")  
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j  
public class AuthenticationController {

    AuthenticationService authenticationService;
    UserService userService;

    @PostMapping("/token")
    APIResponse<AuthenticationResponse>  authenticate(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse result = authenticationService.authenticate(request); 
        return APIResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/introspect")
    APIResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) {
        IntrospectResponse result = authenticationService.introspect(request); 
        return APIResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/refresh")
    APIResponse<AuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        AuthenticationResponse result = authenticationService.refreshToken(request);
        return APIResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    APIResponse<String> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authenticationService.logout(token);
        }
        return APIResponse.<String>builder()
                .result("Logout successful")
                .build();
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
