package io.toflowai.app.service;

import io.toflowai.app.database.model.CredentialEntity;
import io.toflowai.app.database.repository.CredentialRepository;
import io.toflowai.common.dto.CredentialDTO;
import io.toflowai.common.enums.CredentialType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing credentials with encryption.
 */
@Service
@Transactional
public class CredentialService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final CredentialRepository credentialRepository;
    private final SecretKey encryptionKey;

    public CredentialService(CredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
        // In production, this should be loaded from secure configuration
        this.encryptionKey = generateKey();
    }

    public List<CredentialDTO> findAll() {
        return credentialRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public Optional<CredentialDTO> findById(Long id) {
        return credentialRepository.findById(id)
                .map(this::toDTO);
    }

    public Optional<CredentialDTO> findByName(String name) {
        return credentialRepository.findByName(name)
                .map(this::toDTO);
    }

    public List<CredentialDTO> findByType(CredentialType type) {
        return credentialRepository.findByType(type).stream()
                .map(this::toDTO)
                .toList();
    }

    public CredentialDTO create(CredentialDTO dto, String data) {
        if (credentialRepository.findByName(dto.name()).isPresent()) {
            throw new IllegalArgumentException("Credential with name already exists: " + dto.name());
        }

        CredentialEntity entity = new CredentialEntity(dto.name(), dto.type(), encrypt(data));
        CredentialEntity saved = credentialRepository.save(entity);
        return toDTO(saved);
    }

    public CredentialDTO update(Long id, CredentialDTO dto, String data) {
        CredentialEntity entity = credentialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found: " + id));

        entity.setName(dto.name());
        entity.setType(dto.type());
        if (data != null && !data.isBlank()) {
            entity.setDataEncrypted(encrypt(data));
        }

        CredentialEntity saved = credentialRepository.save(entity);
        return toDTO(saved);
    }

    public void delete(Long id) {
        credentialRepository.deleteById(id);
    }

    /**
     * Decrypt and retrieve the credential data.
     * Use with caution - only for actual workflow execution.
     */
    public String getDecryptedData(Long id) {
        CredentialEntity entity = credentialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found: " + id));
        return decrypt(entity.getDataEncrypted());
    }

    private CredentialDTO toDTO(CredentialEntity entity) {
        return new CredentialDTO(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, spec);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to encrypted data
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    private String decrypt(String ciphertext) {
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            // Extract IV from beginning
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, iv.length);

            byte[] encrypted = new byte[decoded.length - iv.length];
            System.arraycopy(decoded, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, spec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    private SecretKey generateKey() {
        // In production, load from secure configuration or key vault
        byte[] keyBytes = new byte[32]; // 256-bit key
        new SecureRandom().nextBytes(keyBytes);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
