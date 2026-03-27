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

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.valid-duration:3600}")
    private long validDuration;

    /**
     * Generate JWT token for user using HS256 algorithm
     */
    public String generateToken(String userId) {
        try {
            log.debug("Starting JWT token generation for userId: {}", userId);
            
            String key = secretKey;

            log.debug("Using secret key with length: {}", key.length());

            // Create JWT claims
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + validDuration * 1000))
                    .claim("userId", userId) // You can add more claims as needed
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
            log.info("JWT token generated successfully for user: {} (token length: {})", userId, token.length());
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
}
