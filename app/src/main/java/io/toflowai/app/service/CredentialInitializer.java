package io.toflowai.app.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.toflowai.common.dto.CredentialDTO;
import io.toflowai.common.enums.CredentialType;
import jakarta.annotation.PostConstruct;

/**
 * Initializes credentials from .env file on application startup.
 * Automatically creates credentials for any API keys found in the .env file.
 */
@Component
public class CredentialInitializer {

    private static final Logger log = LoggerFactory.getLogger(CredentialInitializer.class);

    private final CredentialService credentialService;

    public CredentialInitializer(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @PostConstruct
    public void init() {
        loadCredentialsFromEnvFile();
    }

    /**
     * Load credentials from .env file in the project root.
     */
    private void loadCredentialsFromEnvFile() {
        // Try multiple possible locations for the .env file
        Path[] possiblePaths = {
            Paths.get(".env"),                    // Current directory
            Paths.get("..", ".env"),             // Parent directory
            Paths.get(System.getProperty("user.dir"), ".env"),  // User working directory
            Paths.get(System.getProperty("user.home"), ".env")  // User home (fallback)
        };

        Path envFile = null;
        for (Path path : possiblePaths) {
            if (Files.exists(path)) {
                envFile = path;
                break;
            }
        }

        if (envFile == null) {
            log.info("🔍 No .env file found in any of the expected locations, skipping credential initialization");
            return;
        }

        log.info("🔐 Found .env file at: {}", envFile.toAbsolutePath());

        try {
            log.info("🔐 Loading credentials from .env file...");

            Map<String, String> envVars = parseEnvFile(envFile);
            int created = 0;

            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                // Skip empty values
                if (value == null || value.trim().isEmpty()) {
                    continue;
                }

                // Check if credential already exists
                if (credentialService.findByName(key).isPresent()) {
                    log.debug("Credential '{}' already exists, skipping", key);
                    continue;
                }

                // Create new credential
                try {
                    CredentialDTO credential = CredentialDTO.create(key, CredentialType.API_KEY);
                    credentialService.create(credential, value);
                    created++;
                    log.info("✅ Created credential: {}", key);

                } catch (Exception e) {
                    log.error("❌ Failed to create credential '{}': {}", key, e.getMessage());
                }
            }

            if (created > 0) {
                log.info("🎉 Successfully created {} credentials from .env file", created);
            } else {
                log.info("ℹ️  No new credentials created (all already exist or .env is empty)");
            }

        } catch (Exception e) {
            log.error("❌ Failed to load credentials from .env file: {}", e.getMessage(), e);
        }
    }

    /**
     * Parse .env file into key-value pairs.
     * Supports basic .env format: KEY=value
     * Ignores comments (#) and empty lines.
     */
    private Map<String, String> parseEnvFile(Path envFile) throws IOException {
        Map<String, String> envVars = new HashMap<>();

        Files.lines(envFile).forEach(line -> {
            line = line.trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                return;
            }

            // Parse KEY=value format
            int equalsIndex = line.indexOf('=');
            if (equalsIndex > 0) {
                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();

                // Remove surrounding quotes if present
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }

                envVars.put(key, value);
            }
        });

        return envVars;
    }
}