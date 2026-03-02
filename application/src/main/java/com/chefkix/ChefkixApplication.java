package com.chefkix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ChefKix Modular Monolith — Single entry point.
 *
 * <p>Scans all {@code com.chefkix.*} packages across every module
 * (identity, culinary, social, notification, shared).</p>
 */
@SpringBootApplication(scanBasePackages = "com.chefkix")
@EnableScheduling
public class ChefkixApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChefkixApplication.class, args);
    }
}
