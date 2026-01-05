package com.datingapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Dating Application.
 *
 * @SpringBootApplication combines:
 *                        - @Configuration: Marks this class as a source of bean
 *                        definitions
 *                        - @EnableAutoConfiguration: Auto-configures Spring
 *                        based on dependencies
 *                        - @ComponentScan: Scans this package and sub-packages
 *                        for Spring components
 */
@SpringBootApplication
public class DatingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatingApplication.class, args);
    }
}
