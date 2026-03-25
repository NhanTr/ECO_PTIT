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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;



@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${jwt.secret-key}")
    private String JWT_SECRET_KEY;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(request -> request
                .requestMatchers("/auth/token").permitAll()
                .requestMatchers("/auth/introspect").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/users/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                .anyRequest()
                .authenticated());
        
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
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }
}