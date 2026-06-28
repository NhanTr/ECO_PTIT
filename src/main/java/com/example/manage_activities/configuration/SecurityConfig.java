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

                // Admin Module 1 - user & role management
            .requestMatchers("/api/admin/users/**").hasAnyRole("ADMIN", "MANAGER")
            .requestMatchers("/api/admin/roles/**").hasAnyRole("ADMIN", "MANAGER")
            .requestMatchers("/api/admin/activities/**").hasAnyRole("ADMIN", "MANAGER")

                // Module 4 — system administration
            .requestMatchers("/api/admin/notifications/**").hasAnyRole("ADMIN", "MANAGER")
            .requestMatchers("/api/admin/backups/**").hasRole("ADMIN")
            .requestMatchers("/api/admin/system-configs/**").hasRole("ADMIN")
            .requestMatchers("/api/admin/system-logs/**").hasRole("ADMIN")
                
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
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(new ScopesAuthoritiesConverter());
        return authenticationConverter;
    }

    static class ScopesAuthoritiesConverter implements org.springframework.core.convert.converter.Converter<org.springframework.security.oauth2.jwt.Jwt, java.util.Collection<org.springframework.security.core.GrantedAuthority>> {
        private final JwtGrantedAuthoritiesConverter delegate = new JwtGrantedAuthoritiesConverter();

        ScopesAuthoritiesConverter() {
            delegate.setAuthoritiesClaimName("scopes");
            delegate.setAuthorityPrefix("ROLE_");
        }

        @Override
        public java.util.Collection<org.springframework.security.core.GrantedAuthority> convert(org.springframework.security.oauth2.jwt.Jwt jwt) {
            Object scopes = jwt.getClaims().get("scopes");
            java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();
            if (scopes instanceof String s && !s.isBlank()) {
                for (String part : s.split("[\\s,]+")) {
                    if (!part.isBlank()) {
                        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + part));
                    }
                }
            } else if (scopes instanceof java.util.Collection<?> col) {
                for (Object o : col) {
                    if (o != null) {
                        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + o.toString()));
                    }
                }
            }
            if (authorities.isEmpty()) {
                authorities.addAll(delegate.convert(jwt));
            }
            return authorities;
        }
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
