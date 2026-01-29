package io.toflowai.common.dto;

import io.toflowai.common.enums.CredentialType;

import java.time.Instant;

/**
 * Data Transfer Object for Credentials.
 * Note: The actual credential data is never exposed in this DTO for security.
 *
 * @param id          Credential unique identifier
 * @param name        User-friendly name for the credential
 * @param type        Type of credential (API_KEY, HTTP_BASIC, etc.)
 * @param createdAt   Creation timestamp
 * @param updatedAt   Last update timestamp
 */
public record CredentialDTO(
        Long id,
        String name,
        CredentialType type,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Compact constructor with validation.
     */
    public CredentialDTO {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Credential name cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Credential type cannot be null");
        }
    }

    /**
     * Factory method for creating a new credential.
     */
    public static CredentialDTO create(String name, CredentialType type) {
        return new CredentialDTO(null, name, type, Instant.now(), Instant.now());
    }
}
