package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.AuthenticationRequest;
import com.example.manage_activities.dto.request.IntrospectRequest;
import com.example.manage_activities.dto.request.RefreshTokenRequest;
import com.example.manage_activities.dto.response.AuthenticationResponse;
import com.example.manage_activities.dto.response.IntrospectResponse;
import com.example.manage_activities.entity.User;
import com.example.manage_activities.exception.ErrorCode;
import com.example.manage_activities.exception.AppException;
import com.example.manage_activities.repository.UserRepository;
import com.example.manage_activities.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationService {

    UserRepository userRepository;
    JwtUtil jwtUtil;
    PasswordEncoder passwordEncoder;
    RedisRefreshTokenService redisRefreshTokenService;

    /**
     * Authenticate user with username and password
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.info("Authenticating user: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid password for user: {}", request.getUsername());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Check if user is active
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            log.warn("User is not active: {}", request.getUsername());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Generate JWT token and refresh token
        String token = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        
        // Save refresh token to Redis
        redisRefreshTokenService.saveRefreshToken(user.getId(), refreshToken);
        
        log.info("Token and refresh token generated successfully for user: {}", request.getUsername());

        return AuthenticationResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    /**
     * Introspect token - verify if token is valid
     */
    public IntrospectResponse introspect(IntrospectRequest request) {
        log.info("Introspecting token");

        String userId = jwtUtil.verifyToken(request.getToken());

        if (userId == null) {
            return IntrospectResponse.builder()
                    .valid(false)
                    .build();
        }

        // Get user info from database
        Optional<User> user = userRepository.findById(userId);

        return IntrospectResponse.builder()
                .valid(true)
                .username(user.map(User::getUsername).orElse(null))
                .scopes(user.map(User::getRoleId).orElse(null))
                .build();
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refreshing token");

        String refreshToken = request.getRefreshToken();
        
        // Verify refresh token validity (signature & expiry)
        String userId = jwtUtil.verifyRefreshToken(refreshToken);
        if (userId == null) {
            log.warn("Invalid or expired refresh token");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Validate against Redis stored token
        if (!redisRefreshTokenService.validateRefreshToken(userId, refreshToken)) {
            log.warn("Refresh token not found or mismatch in Redis for userId: {}", userId);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Get user info
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for userId: {}", userId);
                    return new AppException(ErrorCode.USER_NOT_EXISTED);
                });

        // Check if user is still active
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            log.warn("User is not active: {}", user.getUsername());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Generate new access token
        String newToken = jwtUtil.generateToken(user);
        log.info("New access token generated for user: {}", user.getUsername());

        return AuthenticationResponse.builder()
                .token(newToken)
                .refreshToken(refreshToken) // Return the same refresh token
                .authenticated(true)
                .build();
    }

    /**
     * Revoke refresh token (logout functionality)
     */
    public void revokeRefreshToken(String userId) {
        try {
            redisRefreshTokenService.revokeRefreshToken(userId);
            log.info("Refresh token revoked for userId: {}", userId);
        } catch (Exception e) {
            log.error("Error revoking refresh token: {}", e.getMessage(), e);
        }
    }

    /**
     * Logout user - revoke refresh token
     */
    public void logout(String accessToken) {
        try {
            // Extract userId from access token
            String userId = jwtUtil.verifyToken(accessToken);
            
            if (userId != null) {
                // Revoke refresh token in Redis
                redisRefreshTokenService.revokeRefreshToken(userId);
                log.info("User logged out successfully. Refresh token revoked for userId: {}", userId);
            } else {
                log.warn("Invalid access token provided for logout");
            }
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage(), e);
            // Don't throw exception - logout should succeed even if something goes wrong
        }
    }
}
