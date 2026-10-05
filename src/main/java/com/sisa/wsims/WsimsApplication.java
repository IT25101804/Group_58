package com.sisa.wsims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * WSIMS - Web-Based School Information Management System
 * SISA - South International School Academy
 *
 * Entry point. Each of the 8 core modules (see /docs and the branch prompts)
 * is built out on its own git branch and merged back into main sprint by sprint.
 */
@SpringBootApplication
public class WsimsApplication {
    public static void main(String[] args) {
        SpringApplication.run(WsimsApplication.class, args);
    }
}
