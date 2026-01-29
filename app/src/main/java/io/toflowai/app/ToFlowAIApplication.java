package io.toflowai.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.toflowai.ui.ToFlowAIUI;
import javafx.application.Application;

/**
 * Main entry point for ToFlowAI application.
 * Launches both Spring Boot backend and JavaFX frontend.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = { "io.toflowai" })
@EntityScan(basePackages = { "io.toflowai.app.database.model" })
@EnableJpaRepositories(basePackages = { "io.toflowai.app.database.repository" })
public class ToFlowAIApplication {

    public static void main(String[] args) {
        // First bootstrap Spring Boot
        ConfigurableApplicationContext context = SpringApplication.run(ToFlowAIApplication.class, args);

        // Set the context on the UI module
        ToFlowAIUI.setSpringContext(context);
        ToFlowAIUI.setApplicationClass(ToFlowAIApplication.class);

        // Launch JavaFX application
        Application.launch(ToFlowAIUI.class, args);
    }
}
