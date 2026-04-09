package com.example.manage_activities.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RedisRefreshTokenService {

    RedisTemplate<String, String> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;

    /**
     * Save refresh token to Redis
     */
    public void saveRefreshToken(String userId, String refreshToken) {
        try {
            String key = REFRESH_TOKEN_PREFIX + userId;
            // Store token with 7 days expiry
            redisTemplate.opsForValue().set(key, refreshToken, REFRESH_TOKEN_EXPIRY_DAYS, TimeUnit.DAYS);
            log.info("Refresh token saved to Redis for userId: {}", userId);
        } catch (Exception e) {
            log.error("Error saving refresh token to Redis: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save refresh token: " + e.getMessage(), e);
        }
    }

    /**
     * Get refresh token from Redis
     */
    public String getRefreshToken(String userId) {
        try {
            String key = REFRESH_TOKEN_PREFIX + userId;
            String token = redisTemplate.opsForValue().get(key);
            if (token != null) {
                log.debug("Refresh token found in Redis for userId: {}", userId);
            } else {
                log.debug("Refresh token not found in Redis for userId: {}", userId);
            }
            return token;
        } catch (Exception e) {
            log.error("Error getting refresh token from Redis: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Validate refresh token - check if it exists and matches
     */
    public boolean validateRefreshToken(String userId, String refreshToken) {
        try {
            String storedToken = getRefreshToken(userId);
            if (storedToken != null && storedToken.equals(refreshToken)) {
                log.debug("Refresh token validated for userId: {}", userId);
                return true;
            }
            log.warn("Invalid refresh token for userId: {}", userId);
            return false;
        } catch (Exception e) {
            log.error("Error validating refresh token: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Revoke refresh token (logout)
     */
    public void revokeRefreshToken(String userId) {
        try {
            String key = REFRESH_TOKEN_PREFIX + userId;
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Refresh token revoked for userId: {}", userId);
            }
        } catch (Exception e) {
            log.error("Error revoking refresh token: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if refresh token exists
     */
    public boolean isRefreshTokenExists(String userId) {
        try {
            String key = REFRESH_TOKEN_PREFIX + userId;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Error checking refresh token existence: {}", e.getMessage(), e);
            return false;
        }
    }
}
