package com.example.manage_activities;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ManageActivitiesApplication {

	public static void main(String[] args) {
		SpringApplication.run(ManageActivitiesApplication.class, args);
	}

}
