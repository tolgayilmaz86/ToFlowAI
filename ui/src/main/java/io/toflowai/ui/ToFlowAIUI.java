package io.toflowai.ui;

import io.toflowai.ui.console.ExecutionConsoleAppender;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

import org.springframework.context.ConfigurableApplicationContext;

import atlantafx.base.theme.NordDark;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;

/**
 * JavaFX Application entry point.
 * Receives Spring Boot context from the main application.
 */
public class ToFlowAIUI extends Application {

    private static ConfigurableApplicationContext springContext;
    private static Class<?> applicationClass;

    /**
     * Set the Spring context and application class before launching.
     * Called from the main application module.
     */
    public static void setSpringContext(ConfigurableApplicationContext context) {
        springContext = context;
    }

    public static void setApplicationClass(Class<?> appClass) {
        applicationClass = appClass;
    }

    @Override
    public void init() {
        // Context is set externally by the app module
        if (springContext == null) {
            throw new IllegalStateException("Spring context must be set before launching UI");
        }
        if (applicationClass == null) {
            System.out.println("⚠️  Warning: Application class not set, using default behavior");
        }
    }

    @Override
    public void start(Stage primaryStage) {
        // Log application startup with class info
        System.out.println(
                "🚀 Starting " + (applicationClass != null ? applicationClass.getSimpleName() : "JavaFX") + " UI");

        // Apply AtlantaFX theme
        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());

        // Get FxWeaver for Spring-managed controllers
        FxWeaver fxWeaver = springContext.getBean(FxWeaver.class);

        // Load main view
        Scene scene = new Scene(
                fxWeaver.loadView(io.toflowai.ui.controller.MainController.class),
                1400, 900);

        // Load application CSS, without this left menu would look sparse
        scene.getStylesheets().add(getClass().getResource("/io/toflowai/ui/styles/main.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/io/toflowai/ui/styles/console.css").toExternalForm());

        // Programmatically add the ExecutionConsoleAppender
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ExecutionConsoleAppender appender = new ExecutionConsoleAppender();
        appender.setContext(root.getLoggerContext());
        appender.start();
        root.addAppender(appender);

        // Configure stage
        String appName = applicationClass != null ? applicationClass.getSimpleName().replace("Application", "")
                : "ToFlowAI";
        primaryStage.setTitle(appName + " - Workflow Automation");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(700);

        // Load application icon
        try {
            primaryStage.getIcons().add(new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream("/images/icon.png"))));
        } catch (Exception e) {
            // Icon not found, continue without it
        }

        // Start maximized on primary monitor
        primaryStage.setMaximized(true);
        primaryStage.show();
    }


    @Override
    public void stop() {
        // Shutdown Spring context
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
    }
}
