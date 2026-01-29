package io.toflowai.app.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for encrypting and decrypting sensitive data.
 * Uses AES-GCM for authenticated encryption.
 */
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey encryptionKey;

    public EncryptionService(@Value("${toflowai.encryption.key:default-dev-key-change-in-prod}") String keyString) {
        this.encryptionKey = deriveKey(keyString);
    }

    /**
     * Encrypts a plaintext string.
     * 
     * @param plaintext The text to encrypt
     * @return Base64-encoded ciphertext with IV prepended
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, spec);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt value", e);
        }
    }

    /**
     * Decrypts a Base64-encoded ciphertext.
     * 
     * @param ciphertext The Base64-encoded ciphertext with IV prepended
     * @return The decrypted plaintext
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, iv.length);

            // Extract ciphertext
            byte[] encrypted = new byte[decoded.length - iv.length];
            System.arraycopy(decoded, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, spec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt value", e);
        }
    }

    /**
     * Checks if a value appears to be encrypted.
     * Encrypted values are Base64 strings with a minimum length.
     */
    public boolean isEncrypted(String value) {
        if (value == null || value.length() < 20) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length >= GCM_IV_LENGTH + 16; // IV + at least one AES block
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Derives a 256-bit AES key from a string.
     */
    private SecretKey deriveKey(String keyString) {
        // Pad or hash the key to get exactly 32 bytes
        byte[] keyBytes = new byte[32];
        byte[] inputBytes = keyString.getBytes(StandardCharsets.UTF_8);

        // Simple key derivation - in production, use PBKDF2 or similar
        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] = inputBytes[i % inputBytes.length];
            // XOR with position to add variation
            keyBytes[i] ^= (byte) (i * 31);
        }

        return new SecretKeySpec(keyBytes, "AES");
    }
}
