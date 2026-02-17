package com.ubs.bigid.schedulerapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for SchedulerApp
 * Enables OAuth2 secured service-to-service communication with ControllerApp
 */
@SpringBootApplication
@EnableScheduling
public class SchedulerAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerAppApplication.class, args);
    }
}
