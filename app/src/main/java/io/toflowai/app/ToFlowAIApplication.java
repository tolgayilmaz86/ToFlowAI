package io.toflowai.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.toflowai.ui.ToFlowAIUI;
import javafx.application.Application;

/**
 * Main entry point for ToFlowAI application.
 * Launches both Spring Boot backend and JavaFX frontend.
 */
@SpringBootApplication(scanBasePackages = "io.toflowai")
@EnableScheduling
@EnableJpaRepositories(basePackages = "io.toflowai.app.database.repository")
public class ToFlowAIApplication {

    private static final Logger log = LoggerFactory.getLogger(ToFlowAIApplication.class);

    public static void main(final String[] args) {
        log.info("🚀 ToFlowAI Application Starting...");

        // First bootstrap Spring Boot
        final ConfigurableApplicationContext context = SpringApplication.run(ToFlowAIApplication.class, args);

        log.info("✅ Spring Boot initialized successfully");

        // Set the context on the UI module
        ToFlowAIUI.setSpringContext(context);
        ToFlowAIUI.setApplicationClass(ToFlowAIApplication.class);

        log.info("🎨 Launching JavaFX UI...");

        // Launch JavaFX application
        Application.launch(ToFlowAIUI.class, args);
    }
}
