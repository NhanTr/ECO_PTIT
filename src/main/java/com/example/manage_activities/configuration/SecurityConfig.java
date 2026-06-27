package com.example.manage_activities.configuration;


import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;



@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${jwt.secret-key}")
    private String JWT_SECRET_KEY;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(request -> request
                // Public endpoints - no authentication required
                .requestMatchers("/auth/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/activities/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/rooms/**").permitAll()
                
                // Self-service user endpoints
            .requestMatchers(HttpMethod.POST, "/api/v1/users/change-password").authenticated()

                // Admin Module 1 - user & role management (chỉ QTHT - QTHT_QĐ 1)
            .requestMatchers("/api/admin/users/**").hasRole("ADMIN")
            .requestMatchers("/api/admin/roles/**").hasRole("ADMIN")
            .requestMatchers("/api/admin/activities/**").hasAnyRole("ADMIN", "MANAGER")

                // Module 4 — system administration (chỉ QTHT)
            .requestMatchers("/api/admin/notifications/**").hasAnyRole("ADMIN", "MANAGER")
            .requestMatchers("/api/admin/backups/**").hasRole("ADMIN")
            .requestMatchers("/api/admin/system-configs/**").hasRole("ADMIN")
            .requestMatchers("/api/admin/system-logs/**").hasRole("ADMIN")
                // QTHT #5 — system statistics (admin only)
            .requestMatchers("/api/admin/system-statistics/**").hasRole("ADMIN")
                // QTHT #2 — admin role assignment endpoints (chỉ ADMIN)
            .requestMatchers("/api/admin/users/*/assign-role", "/api/admin/users/*/revoke-role").hasRole("ADMIN")
                // QTHT #7 — categories management (admin only)
            .requestMatchers("/api/admin/categories/**").hasRole("ADMIN")
                // QTHT #8 — notification channels & templates (admin only)
            .requestMatchers("/api/admin/notification-channels/**", "/api/admin/notification-templates/**").hasRole("ADMIN")
                // QTHT #9 — academic periods (admin only)
            .requestMatchers("/api/admin/academic-periods/**").hasRole("ADMIN")
                // QTHT #10 — dynamic permissions (admin only)
            .requestMatchers("/api/admin/permissions/**").hasRole("ADMIN")
                
                // Activity endpoints - ORGANIZER, ADMIN can create/edit
            .requestMatchers(HttpMethod.POST, "/api/v1/activities").hasAnyRole("ORGANIZER", "ADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/v1/activities/**").hasAnyRole("ORGANIZER", "ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/v1/activities/**").hasAnyRole("ORGANIZER", "ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/manager/activities/**").hasRole("MANAGER")
            .requestMatchers(HttpMethod.GET, "/api/notifications").authenticated()
            .requestMatchers(HttpMethod.PATCH, "/api/notifications/**").authenticated()
            .requestMatchers(HttpMethod.POST, "/api/notifications").hasAnyRole("ADMIN", "MANAGER", "ORGANIZER")

                // Registration endpoints - all authenticated users
            .requestMatchers(HttpMethod.POST, "/api/v1/registrations/**").authenticated()
            .requestMatchers(HttpMethod.DELETE, "/api/v1/registrations/**").authenticated()
            .requestMatchers(HttpMethod.GET, "/api/v1/registrations/**").authenticated()
                

                // All other authenticated requests require authentication
                .anyRequest().authenticated()
        );
        
        // Apply oauth2ResourceServer with JWT
        httpSecurity.oauth2ResourceServer(oauth2 -> 
                oauth2.jwt(jwtConfigurer -> jwtConfigurer.decoder(jwtDecoder())
                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(jwtAuthenticationEntryPoint())
        );

        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        return httpSecurity.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("scopes");
        converter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
        return authenticationConverter;
    }

    

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(JWT_SECRET_KEY.getBytes(), "HmacSHA256");
        return NimbusJwtDecoder
            .withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }

    @Bean
    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }
}
