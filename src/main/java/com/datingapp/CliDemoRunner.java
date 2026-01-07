package com.datingapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.datingapp.infrastructure.persistence.jpa.SpringDataMatchRepository;
import com.datingapp.infrastructure.persistence.jpa.SpringDataUserRepository;

/**
 * CLI demo runner - prints database stats to terminal.
 */
@Component
@Profile("cli")
public class CliDemoRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CliDemoRunner.class);
    private static final String SEPARATOR = "═══════════════════════════════════════════════════════════════";

    private final SpringDataUserRepository userRepository;
    private final SpringDataMatchRepository matchRepository;

    public CliDemoRunner(SpringDataUserRepository userRepository,
            SpringDataMatchRepository matchRepository) {
        this.userRepository = userRepository;
        this.matchRepository = matchRepository;
    }

    @Override
    public void run(String... args) {
        logger.info("");
        logger.info(SEPARATOR);
        logger.info("                    DATING APP - CLI MODE                      ");
        logger.info(SEPARATOR);
        logger.info("");

        long userCount = userRepository.count();
        long matchCount = matchRepository.count();

        logger.info("DATABASE STATISTICS:");
        logger.info("   Users:   {}", userCount);
        logger.info("   Matches: {}", matchCount);
        logger.info("");

        if (userCount == 0) {
            logger.info("Database is empty.");
        } else {
            logger.info("USERS:");
            userRepository.findAll().forEach(user -> logger.info("   - {}", user.getUsername()));
        }

        logger.info("");
        logger.info("Application started successfully!");
        logger.info(SEPARATOR);
    }
}
