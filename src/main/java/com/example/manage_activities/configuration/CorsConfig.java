package com.example.manage_activities.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // registry.addMapping("/**")
        //         .allowedOrigins("http://localhost:3000")
        //         .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        //         .allowedHeaders("*")
        //         .exposedHeaders("Authorization", "Content-Type")
        //         .allowCredentials(true)
        //         .maxAge(3600);
        
        registry.addMapping("/**")
                .allowedOrigins("http://127.0.0.1:5500")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
