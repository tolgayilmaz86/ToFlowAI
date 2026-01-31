package io.toflowai.app.api;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.toflowai.app.service.CredentialService;
import io.toflowai.common.dto.CredentialDTO;
import io.toflowai.common.enums.CredentialType;

/**
 * REST API controller for credential management.
 */
@RestController
@RequestMapping("/api/credentials")
public class CredentialController {

    private final CredentialService credentialService;

    public CredentialController(final CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @GetMapping
    public List<CredentialDTO> findAll() {
        return credentialService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CredentialDTO> findById(@PathVariable final Long id) {
        return credentialService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    public List<CredentialDTO> findByType(@PathVariable final CredentialType type) {
        return credentialService.findByType(type);
    }

    /**
     * Create a new credential.
     * Request body should contain:
     * - name: Credential name
     * - type: Credential type
     * - data: The actual credential data (will be encrypted)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialDTO create(@RequestBody final Map<String, Object> request) {
        final String name = (String) request.get("name");
        final CredentialType type = CredentialType.valueOf((String) request.get("type"));
        final String data = (String) request.get("data");

        final CredentialDTO dto = CredentialDTO.create(name, type);
        return credentialService.create(dto, data);
    }

    /**
     * Update a credential.
     * Data field is optional - if not provided, only metadata is updated.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CredentialDTO> update(
            @PathVariable final Long id,
            @RequestBody final Map<String, Object> request) {
        final String name = (String) request.get("name");
        final CredentialType type = CredentialType.valueOf((String) request.get("type"));
        final String data = (String) request.get("data");

        final CredentialDTO dto = CredentialDTO.create(name, type);
        return ResponseEntity.ok(credentialService.update(id, dto, data));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable final Long id) {
        credentialService.delete(id);
    }

    /**
     * Import credentials from a .env formatted file or text content.
     * Supports both file upload and raw text input.
     * Returns a summary of the import operation.
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importCredentials(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String content) {

        String envContent;
        if (file != null) {
            try {
                envContent = new String(file.getBytes());
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to read uploaded file: " + e.getMessage()));
            }
        } else if (content != null && !content.trim().isEmpty()) {
            envContent = content;
        } else {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Either 'file' or 'content' parameter must be provided"));
        }

        try {
            Map<String, String> envVars = parseEnvContent(envContent);
            Map<String, Object> result = importCredentialsFromMap(envVars);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to import credentials: " + e.getMessage()));
        }
    }

    /**
     * Parse .env formatted content into key-value pairs.
     */
    private Map<String, String> parseEnvContent(String content) {
        Map<String, String> envVars = new HashMap<>();
        String[] lines = content.split("\\r?\\n");

        for (String line : lines) {
            line = line.trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
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
        }

        return envVars;
    }

    /**
     * Import credentials from parsed key-value map.
     */
    private Map<String, Object> importCredentialsFromMap(Map<String, String> envVars) {
        int total = envVars.size();
        int created = 0;
        int skipped = 0;
        int errors = 0;
        List<String> messages = new java.util.ArrayList<>();

        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Skip empty values
            if (value == null || value.trim().isEmpty()) {
                skipped++;
                messages.add("Skipped '" + key + "': empty value");
                continue;
            }

            // Check if credential already exists
            if (credentialService.findByName(key).isPresent()) {
                skipped++;
                messages.add("Skipped '" + key + "': already exists");
                continue;
            }

            // Create new credential
            try {
                CredentialDTO credential = CredentialDTO.create(key, CredentialType.API_KEY);
                credentialService.create(credential, value);
                created++;
                messages.add("Created '" + key + "'");
            } catch (Exception e) {
                errors++;
                messages.add("Error creating '" + key + "': " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("errors", errors);
        result.put("messages", messages);

        return result;
    }
}
