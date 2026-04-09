package com.example.manage_activities.util;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import com.example.manage_activities.entity.User;
import com.example.manage_activities.enums.Roles;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.valid-duration:3600}")
    private long validDuration;

    @Value("${jwt.refresh-duration:604800}")
    private long refreshDuration;

    /**
     * Generate JWT token for user using HS256 algorithm
     */
    public String generateToken(User user) {
        try {
            log.debug("Starting JWT token generation for userId: {}", user.getId());

            String key = secretKey;

            log.debug("Using secret key with length: {}", key.length());

            // Create JWT claims
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getId())  // Add subject for authentication
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + validDuration * 1000))
                    .claim("userId", user.getId()) // You can add more claims as needed
                    .claim("scopes", buildScopes(user))
                    .build();

            // Create JWS header
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256).build();
            
            // Create payload
            Payload payload = new Payload(claimsSet.toJSONObject());
            
            // Create JWS object
            JWSObject jwsObject = new JWSObject(header, payload);

            // Sign with MACSigner
            jwsObject.sign(new MACSigner(key.getBytes(StandardCharsets.UTF_8)));

            String token = jwsObject.serialize();
            log.info("JWT token generated successfully for user: {} (token length: {})", user.getId(), token.length());
            return token;
        } catch (JOSEException e) {
            log.error("JOSE Exception while generating JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate token: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Exception while generating JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate token: " + e.getMessage(), e);
        }
    }

    /**
     * Build scopes string from user roles
     */
    private String buildScopes(User user) {
        // Map roleId to role name using Roles enum: 1=ADMIN, 2=ORGANIZER, 3=MANAGER, 4=STUDENT
        try {
            return Roles.getNameById(user.getRoleId());
        } catch (Exception e) {
            log.warn("Error getting role name for roleId: {}, defaulting to STUDENT", user.getRoleId(), e);
            return "STUDENT";
        }
    }

    /**
     * Verify JWT token and return user ID
     */
    public String verifyToken(String token) {
        try {

            String key = secretKey;

            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier verifier = new MACVerifier(key.getBytes(StandardCharsets.UTF_8));
            if (!signedJWT.verify(verifier)) {
                log.warn("JWT signature verification failed");
                return null;
            }

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            // Check expiration
            if (claimsSet.getExpirationTime().before(new Date ())) {
                log.warn("JWT token expired");
                return null;
            }

            return claimsSet.getClaim("userId").toString();
        } catch (ParseException e) {
            log.error("Error parsing JWT token", e);
            return null;
        } catch (JOSEException e) {
            log.error("Error verifying JWT token", e);
            return null;
        }
    }

    /**
     * Introspect token - check if token is valid
     */
    public boolean introspectToken(String token) {
        return verifyToken(token) != null;
    }

    /**
     * Generate refresh token for user
     */
    public String generateRefreshToken(User user) {
        try {
            log.debug("Starting refresh token generation for userId: {}", user.getId());

            String key = secretKey;

            // Create JWT claims for refresh token
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getId())
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + refreshDuration * 1000))
                    .claim("userId", user.getId())
                    .claim("type", "refresh")
                    .build();

            // Create JWS header
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256).build();
            
            // Create payload
            Payload payload = new Payload(claimsSet.toJSONObject());
            
            // Create JWS object
            JWSObject jwsObject = new JWSObject(header, payload);

            // Sign with MACSigner
            jwsObject.sign(new MACSigner(key.getBytes(StandardCharsets.UTF_8)));

            String token = jwsObject.serialize();
            log.info("Refresh token generated successfully for user: {}", user.getId());
            return token;
        } catch (JOSEException e) {
            log.error("JOSE Exception while generating refresh token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate refresh token: " + e.getMessage(), e);
        }
    }

    /**
     * Verify refresh token and return user ID
     */
    public String verifyRefreshToken(String token) {
        try {
            String key = secretKey;

            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier verifier = new MACVerifier(key.getBytes(StandardCharsets.UTF_8));
            if (!signedJWT.verify(verifier)) {
                log.warn("Refresh token signature verification failed");
                return null;
            }

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            // Check expiration
            if (claimsSet.getExpirationTime().before(new Date())) {
                log.warn("Refresh token expired");
                return null;
            }

            // Verify token type
            Object typeObj = claimsSet.getClaim("type");
            if (typeObj == null || !"refresh".equals(typeObj.toString())) {
                log.warn("Invalid token type for refresh token");
                return null;
            }

            return claimsSet.getClaim("userId").toString();
        } catch (ParseException e) {
            log.error("Error parsing refresh token", e);
            return null;
        } catch (JOSEException e) {
            log.error("Error verifying refresh token", e);
            return null;
        }
    }

    /**
     * Extract user ID from token (without full verification)
     */
    public String extractUserIdFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            Object userIdObj = claimsSet.getClaim("userId");
            return userIdObj != null ? userIdObj.toString() : null;
        } catch (ParseException e) {
            log.error("Error extracting user ID from token", e);
            return null;
        }
    }
}
