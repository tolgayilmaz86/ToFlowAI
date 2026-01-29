package io.toflowai.common.service;

import java.util.List;
import java.util.Optional;

import io.toflowai.common.dto.CredentialDTO;
import io.toflowai.common.enums.CredentialType;

/**
 * Interface for credential management operations.
 * Implemented by app module, used by UI module.
 */
public interface CredentialServiceInterface {

    /**
     * Get all credentials (without sensitive data).
     */
    List<CredentialDTO> findAll();

    /**
     * Find credential by ID.
     */
    Optional<CredentialDTO> findById(Long id);

    /**
     * Find credential by name.
     */
    Optional<CredentialDTO> findByName(String name);

    /**
     * Find credentials by type.
     */
    List<CredentialDTO> findByType(CredentialType type);

    /**
     * Create a new credential.
     * 
     * @param dto  The credential metadata
     * @param data The sensitive credential data (will be encrypted)
     * @return The created credential (without sensitive data)
     */
    CredentialDTO create(CredentialDTO dto, String data);

    /**
     * Update an existing credential.
     * 
     * @param id   The credential ID
     * @param dto  The updated metadata
     * @param data The new sensitive data (null to keep existing)
     * @return The updated credential
     */
    CredentialDTO update(Long id, CredentialDTO dto, String data);

    /**
     * Delete a credential.
     */
    void delete(Long id);

    /**
     * Get decrypted credential data for use in node execution.
     * This method should only be called during workflow execution.
     * 
     * @param id The credential ID
     * @return The decrypted credential data
     */
    String getDecryptedData(Long id);

    /**
     * Test if a credential is valid (e.g., by making a test API call).
     * 
     * @param id The credential ID
     * @return true if the credential is valid
     */
    boolean testCredential(Long id);
}
