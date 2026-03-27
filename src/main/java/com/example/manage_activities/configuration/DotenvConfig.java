package com.example.manage_activities.configuration;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to load environment variables from .env file
 */
@Configuration
public class DotenvConfig {
    static {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        
        // Load all variables from .env into System properties
        dotenv.entries()
                .forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
    }
}
