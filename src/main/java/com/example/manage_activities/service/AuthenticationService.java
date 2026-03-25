package com.example.manage_activities.service;

import com.example.manage_activities.dto.request.AuthenticationRequest;
import com.example.manage_activities.dto.request.IntrospectRequest;
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

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId());
        log.info("Token generated successfully for user: {}", request.getUsername());

        return AuthenticationResponse.builder()
                .token(token)
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
                .build();
    }
}
