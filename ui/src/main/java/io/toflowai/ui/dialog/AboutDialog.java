package io.toflowai.ui.dialog;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * About Dialog - Shows application information, logo, and links.
 */
public class AboutDialog extends Dialog<Void> {

    private static final String APP_NAME = "ToFlowAI";
    private static final String APP_VERSION = "0.1.0-SNAPSHOT";
    private static final String AUTHOR = "Tolga Yilmaz";
    private static final String LICENSE = "MIT License";
    private static final String PROJECT_URL = "https://github.com/tolgayilmaz86/ToFlowAI";
    private static final String DESCRIPTION = "Visual Workflow Automation for Everyone";

    public AboutDialog() {
        setTitle("About " + APP_NAME);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);
        setResizable(false);

        // Main layout
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setStyle("-fx-background-color: #2e3440;");
        mainLayout.setPrefWidth(500);
        mainLayout.setPrefHeight(240);

        // Logo and title section
        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // Logo
        ImageView logoView = new ImageView();
        try {
            Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png")));
            logoView.setImage(logo);
            logoView.setFitWidth(80);
            logoView.setFitHeight(80);
            logoView.setPreserveRatio(true);
        } catch (Exception e) {
            // Fallback: use icon if logo not found
            try {
                Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icon.png")));
                logoView.setImage(icon);
                logoView.setFitWidth(64);
                logoView.setFitHeight(64);
                logoView.setPreserveRatio(true);
            } catch (Exception ex) {
                // No image available
            }
        }

        // Title and description
        VBox titleBox = new VBox(5);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label appNameLabel = new Label(APP_NAME);
        appNameLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        appNameLabel.setTextFill(Color.web("#88c0d0"));

        Label descriptionLabel = new Label(DESCRIPTION);
        descriptionLabel.setFont(Font.font("System", 14));
        descriptionLabel.setTextFill(Color.web("#d8dee9"));

        Label versionLabel = new Label("Version " + APP_VERSION);
        versionLabel.setFont(Font.font("System", 12));
        versionLabel.setTextFill(Color.web("#81a1c1"));

        titleBox.getChildren().addAll(appNameLabel, descriptionLabel, versionLabel);

        headerBox.getChildren().addAll(logoView, titleBox);

        // Information section
        VBox infoBox = new VBox(10);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label authorLabel = new Label("Created by: " + AUTHOR);
        authorLabel.setFont(Font.font("System", 14));
        authorLabel.setTextFill(Color.web("#d8dee9"));

        Label licenseLabel = new Label("License: " + LICENSE);
        licenseLabel.setFont(Font.font("System", 14));
        licenseLabel.setTextFill(Color.web("#d8dee9"));

        // Project link
        HBox linkBox = new HBox(5);
        linkBox.setAlignment(Pos.CENTER_LEFT);

        Label linkLabel = new Label("Project:");
        linkLabel.setFont(Font.font("System", 14));
        linkLabel.setTextFill(Color.web("#d8dee9"));

        Hyperlink projectLink = new Hyperlink(PROJECT_URL);
        projectLink.setFont(Font.font("System", 14));
        projectLink.setTextFill(Color.web("#88c0d0"));
        projectLink.setOnAction(e -> openUrl(PROJECT_URL));

        linkBox.getChildren().addAll(linkLabel, projectLink);

        infoBox.getChildren().addAll(authorLabel, licenseLabel, linkBox);

        // Footer with close button
        HBox footerBox = new HBox();
        footerBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footerBox.getChildren().addAll(spacer);

        // Add all sections to main layout
        mainLayout.getChildren().addAll(headerBox, infoBox, footerBox);

        // Set the dialog content
        getDialogPane().setContent(mainLayout);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Style the close button
        Button closeBtn = (Button) getDialogPane().lookupButton(ButtonType.CLOSE);
        closeBtn.setText("Close");
        closeBtn.setStyle("-fx-background-color: #88c0d0; -fx-text-fill: #2e3440;");

        // Set dialog icon
        try {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icon.png"))));
        } catch (Exception e) {
            // Icon not found, continue without it
        }
    }

    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (IOException | URISyntaxException e) {
            // Failed to open URL
        }
    }
}