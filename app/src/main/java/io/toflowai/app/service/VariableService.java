package io.toflowai.app.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.toflowai.app.database.repository.VariableRepository;
import io.toflowai.app.entity.VariableEntity;
import io.toflowai.common.dto.VariableDTO;
import io.toflowai.common.dto.VariableDTO.VariableScope;
import io.toflowai.common.dto.VariableDTO.VariableType;
import io.toflowai.common.service.VariableServiceInterface;

/**
 * Service for managing workflow variables.
 */
@Service
@Transactional
public class VariableService implements VariableServiceInterface {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final Pattern VARIABLE_REFERENCE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private final VariableRepository variableRepository;
    private final SecretKey encryptionKey;

    public VariableService(VariableRepository variableRepository) {
        this.variableRepository = variableRepository;
        // In production, this should be loaded from secure configuration
        this.encryptionKey = generateKey();
    }

    @Override
    public List<VariableDTO> findAll() {
        return variableRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<VariableDTO> findGlobalVariables() {
        return variableRepository.findByScope(VariableScope.GLOBAL).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<VariableDTO> findByWorkflowId(Long workflowId) {
        return variableRepository.findByWorkflowId(workflowId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public Optional<VariableDTO> findById(Long id) {
        return variableRepository.findById(id)
                .map(this::toDTO);
    }

    @Override
    public Optional<VariableDTO> findByNameAndScope(String name, VariableScope scope, Long workflowId) {
        if (scope == VariableScope.GLOBAL) {
            return variableRepository.findByNameAndScopeAndWorkflowIdIsNull(name, scope)
                    .map(this::toDTO);
        } else {
            return variableRepository.findByNameAndScopeAndWorkflowId(name, scope, workflowId)
                    .map(this::toDTO);
        }
    }

    @Override
    public VariableDTO create(VariableDTO variable) {
        if (!isValidVariableName(variable.name())) {
            throw new IllegalArgumentException("Invalid variable name: " + variable.name());
        }

        // Check for duplicates
        if (variable.scope() == VariableScope.GLOBAL) {
            if (variableRepository.existsByNameAndScopeAndWorkflowId(variable.name(), VariableScope.GLOBAL, null)) {
                throw new IllegalArgumentException("Global variable already exists: " + variable.name());
            }
        } else {
            if (variableRepository.existsByNameAndScopeAndWorkflowId(variable.name(), variable.scope(),
                    variable.workflowId())) {
                throw new IllegalArgumentException("Variable already exists: " + variable.name());
            }
        }

        VariableEntity entity = new VariableEntity();
        entity.setName(variable.name());
        entity.setType(variable.type());
        entity.setScope(variable.scope());
        entity.setWorkflowId(variable.workflowId());
        entity.setDescription(variable.description());

        // Encrypt secret values
        if (variable.type() == VariableType.SECRET) {
            entity.setEncryptedValue(encrypt(variable.value()));
        } else {
            entity.setValue(variable.value());
        }

        VariableEntity saved = variableRepository.save(entity);
        return toDTO(saved);
    }

    @Override
    public VariableDTO update(Long id, VariableDTO variable) {
        VariableEntity entity = variableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Variable not found: " + id));

        if (!isValidVariableName(variable.name())) {
            throw new IllegalArgumentException("Invalid variable name: " + variable.name());
        }

        entity.setName(variable.name());
        entity.setType(variable.type());
        entity.setDescription(variable.description());

        // Handle value updates
        if (variable.value() != null && !variable.value().isBlank()) {
            if (variable.type() == VariableType.SECRET) {
                entity.setEncryptedValue(encrypt(variable.value()));
                entity.setValue(null);
            } else {
                entity.setValue(variable.value());
                entity.setEncryptedValue(null);
            }
        }

        VariableEntity saved = variableRepository.save(entity);
        return toDTO(saved);
    }

    @Override
    public void delete(Long id) {
        variableRepository.deleteById(id);
    }

    @Override
    public String getResolvedValue(Long id) {
        VariableEntity entity = variableRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Variable not found: " + id));

        if (entity.isEncrypted()) {
            return decrypt(entity.getEncryptedValue());
        }
        return entity.getValue();
    }

    @Override
    public Map<String, Object> getVariablesForWorkflow(Long workflowId) {
        Map<String, Object> variables = new LinkedHashMap<>();

        // Add global variables first
        for (VariableEntity entity : variableRepository.findByScope(VariableScope.GLOBAL)) {
            String value = entity.isEncrypted() ? decrypt(entity.getEncryptedValue()) : entity.getValue();
            variables.put(entity.getName(), parseValue(value, entity.getType()));
        }

        // Add workflow-specific variables (override globals)
        if (workflowId != null) {
            for (VariableEntity entity : variableRepository.findByWorkflowId(workflowId)) {
                String value = entity.isEncrypted() ? decrypt(entity.getEncryptedValue()) : entity.getValue();
                variables.put(entity.getName(), parseValue(value, entity.getType()));
            }
        }

        return variables;
    }

    @Override
    public String resolveVariables(String input, Long workflowId) {
        if (input == null || input.isBlank()) {
            return input;
        }

        Map<String, Object> variables = getVariablesForWorkflow(workflowId);
        Matcher matcher = VARIABLE_REFERENCE_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            Object value = variables.get(varName);
            String replacement = value != null ? value.toString() : matcher.group(0);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    @Override
    public boolean isValidVariableName(String name) {
        return name != null && VARIABLE_NAME_PATTERN.matcher(name).matches();
    }

    private Object parseValue(String value, VariableType type) {
        if (value == null)
            return null;

        return switch (type) {
            case NUMBER -> {
                try {
                    if (value.contains(".")) {
                        yield Double.parseDouble(value);
                    }
                    yield Long.parseLong(value);
                } catch (NumberFormatException e) {
                    yield value;
                }
            }
            case BOOLEAN -> Boolean.parseBoolean(value);
            case JSON, STRING, SECRET -> value;
        };
    }

    private VariableDTO toDTO(VariableEntity entity) {
        // Don't expose actual values for secrets
        String value = entity.isEncrypted() ? "********" : entity.getValue();

        return new VariableDTO(
                entity.getId(),
                entity.getName(),
                value,
                entity.getType(),
                entity.getScope(),
                entity.getWorkflowId(),
                entity.getDescription(),
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

            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt variable value", e);
        }
    }

    private String decrypt(String ciphertext) {
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);

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
            throw new RuntimeException("Failed to decrypt variable value", e);
        }
    }

    private SecretKey generateKey() {
        byte[] keyBytes = new byte[32]; // 256-bit key
        SECURE_RANDOM.nextBytes(keyBytes);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
