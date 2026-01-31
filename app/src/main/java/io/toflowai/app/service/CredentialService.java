package io.toflowai.app.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.toflowai.app.database.model.CredentialEntity;
import io.toflowai.app.database.repository.CredentialRepository;
import io.toflowai.common.dto.CredentialDTO;
import io.toflowai.common.enums.CredentialType;
import io.toflowai.common.service.CredentialServiceInterface;

/**
 * Service for managing credentials with encryption.
 */
@Service
@Transactional
public class CredentialService implements CredentialServiceInterface {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Path KEY_FILE = Paths.get("./data/encryption.key");

    private final CredentialRepository credentialRepository;
    private final SecretKey encryptionKey;

    public CredentialService(CredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
        this.encryptionKey = loadOrGenerateKey();
    }

    @Override
    public List<CredentialDTO> findAll() {
        return credentialRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public Optional<CredentialDTO> findById(Long id) {
        return credentialRepository.findById(id)
                .map(this::toDTO);
    }

    @Override
    public Optional<CredentialDTO> findByName(String name) {
        return credentialRepository.findByName(name)
                .map(this::toDTO);
    }

    @Override
    public List<CredentialDTO> findByType(CredentialType type) {
        return credentialRepository.findByType(type).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public CredentialDTO create(CredentialDTO dto, String data) {
        if (credentialRepository.findByName(dto.name()).isPresent()) {
            throw new IllegalArgumentException("Credential with name already exists: " + dto.name());
        }

        CredentialEntity entity = new CredentialEntity(dto.name(), dto.type(), encrypt(data));
        CredentialEntity saved = credentialRepository.save(entity);
        return toDTO(saved);
    }

    @Override
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

    @Override
    public void delete(Long id) {
        credentialRepository.deleteById(id);
    }

    /**
     * Decrypt and retrieve the credential data.
     * Use with caution - only for actual workflow execution.
     */
    @Override
    public String getDecryptedData(Long id) {
        CredentialEntity entity = credentialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found: " + id));
        return decrypt(entity.getDataEncrypted());
    }

    /**
     * Test if a credential is valid.
     * Currently just checks if it can be decrypted.
     */
    @Override
    public boolean testCredential(Long id) {
        try {
            String data = getDecryptedData(id);
            return data != null && !data.isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    private CredentialDTO toDTO(CredentialEntity entity) {
        return new CredentialDTO(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

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

    private SecretKey loadOrGenerateKey() {
        try {
            // Try to load existing key
            if (Files.exists(KEY_FILE)) {
                byte[] keyBytes = Base64.getDecoder().decode(Files.readString(KEY_FILE).trim());
                return new SecretKeySpec(keyBytes, "AES");
            }

            // Generate new key and save it
            byte[] keyBytes = new byte[32]; // 256-bit key
            SECURE_RANDOM.nextBytes(keyBytes);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");

            // Save key to file
            Files.createDirectories(KEY_FILE.getParent());
            Files.writeString(KEY_FILE, Base64.getEncoder().encodeToString(keyBytes));

            return key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load or generate encryption key", e);
        }
    }
}
