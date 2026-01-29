package io.toflowai.app.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.toflowai.app.database.model.SettingEntity;
import io.toflowai.app.database.repository.SettingRepository;
import io.toflowai.common.dto.SettingDTO;
import io.toflowai.common.enums.SettingCategory;
import io.toflowai.common.enums.SettingType;
import io.toflowai.common.service.SettingsServiceInterface;
import jakarta.annotation.PostConstruct;

/**
 * Service for managing application settings.
 * Provides caching, validation, and change notification.
 */
@Service
@Transactional
public class SettingsService implements SettingsServiceInterface {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    private final SettingRepository settingRepository;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final List<SettingsChangeListener> changeListeners = new CopyOnWriteArrayList<>();

    // In-memory cache for fast access
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public SettingsService(SettingRepository settingRepository,
            EncryptionService encryptionService,
            ObjectMapper objectMapper) {
        this.settingRepository = settingRepository;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        initializeDefaults();
        loadCache();
    }

    /**
     * Load all settings into cache.
     */
    private void loadCache() {
        cache.clear();
        settingRepository.findAll().forEach(entity -> {
            String value = entity.getType() == SettingType.PASSWORD
                    ? decryptValue(entity.getValue())
                    : entity.getValue();
            cache.put(entity.getKey(), value);
        });
        log.info("Loaded {} settings into cache", cache.size());
    }

    @Override
    public void initializeDefaults() {
        List<SettingDTO> defaults = SettingsDefaults.getAll();
        int created = 0;

        for (SettingDTO def : defaults) {
            if (!settingRepository.existsByKey(def.key())) {
                SettingEntity entity = new SettingEntity(
                        def.key(),
                        def.type() == SettingType.PASSWORD ? encryptValue(def.value()) : def.value(),
                        def.category(),
                        def.type());
                entity.setLabel(def.label());
                entity.setDescription(def.description());
                entity.setVisible(def.visible());
                entity.setRequiresRestart(def.requiresRestart());
                entity.setDisplayOrder(def.displayOrder());
                entity.setValidationRules(def.validationRules());
                settingRepository.save(entity);
                created++;
            }
        }

        if (created > 0) {
            log.info("Initialized {} default settings", created);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettingDTO> findAll() {
        return settingRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettingDTO> findAllVisible() {
        return settingRepository.findByVisibleTrueOrderByCategoryAscDisplayOrderAsc().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettingDTO> findByCategory(SettingCategory category) {
        return settingRepository.findByCategoryAndVisibleTrueOrderByDisplayOrderAsc(category).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SettingDTO> findByKey(String key) {
        return settingRepository.findByKey(key)
                .map(this::toDTO);
    }

    @Override
    public String getValue(String key, String defaultValue) {
        String cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        return settingRepository.findByKey(key)
                .map(entity -> {
                    String value = entity.getType() == SettingType.PASSWORD
                            ? decryptValue(entity.getValue())
                            : entity.getValue();
                    cache.put(key, value);
                    return value;
                })
                .orElse(defaultValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getValue(key, null);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = getValue(key, null);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @Override
    public long getLong(String key, long defaultValue) {
        String value = getValue(key, null);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        String value = getValue(key, null);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @Override
    public SettingDTO setValue(String key, String value) {
        SettingEntity entity = settingRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Unknown setting: " + key));

        // Validate value
        validateValue(entity, value);

        String oldValue = entity.getType() == SettingType.PASSWORD
                ? decryptValue(entity.getValue())
                : entity.getValue();

        // Encrypt if password type
        String storedValue = entity.getType() == SettingType.PASSWORD
                ? encryptValue(value)
                : value;

        entity.setValue(storedValue);
        settingRepository.save(entity);

        // Update cache
        cache.put(key, value);

        // Notify listeners
        notifyChange(key, oldValue, value);

        log.debug("Setting '{}' updated", key);
        return toDTO(entity);
    }

    @Override
    public List<SettingDTO> setValues(Map<String, String> settings) {
        return settings.entrySet().stream()
                .map(entry -> setValue(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public SettingDTO resetToDefault(String key) {
        String defaultValue = SettingsDefaults.getDefault(key);
        if (defaultValue == null) {
            throw new IllegalArgumentException("No default for setting: " + key);
        }
        return setValue(key, defaultValue);
    }

    @Override
    public List<SettingDTO> resetCategoryToDefaults(SettingCategory category) {
        return SettingsDefaults.getByCategory(category).stream()
                .map(def -> setValue(def.key(), def.value()))
                .toList();
    }

    @Override
    public void resetAllToDefaults() {
        SettingsDefaults.getAll().forEach(def -> setValue(def.key(), def.value()));
        log.info("All settings reset to defaults");
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettingDTO> search(String query) {
        return settingRepository.searchSettings(query).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportAsJson() {
        try {
            Map<String, String> settings = new java.util.HashMap<>();
            for (SettingEntity entity : settingRepository.findAll()) {
                // Don't export password values in plain text
                if (entity.getType() != SettingType.PASSWORD) {
                    settings.put(entity.getKey(), entity.getValue());
                }
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(settings);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to export settings", e);
        }
    }

    @Override
    public void importFromJson(String json) {
        try {
            Map<String, String> settings = objectMapper.readValue(json, new TypeReference<>() {
            });
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                try {
                    setValue(entry.getKey(), entry.getValue());
                } catch (IllegalArgumentException e) {
                    log.warn("Skipping unknown setting during import: {}", entry.getKey());
                }
            }
            log.info("Imported {} settings", settings.size());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to import settings", e);
        }
    }

    @Override
    public void addChangeListener(SettingsChangeListener listener) {
        changeListeners.add(listener);
    }

    @Override
    public void removeChangeListener(SettingsChangeListener listener) {
        changeListeners.remove(listener);
    }

    private void notifyChange(String key, String oldValue, String newValue) {
        for (SettingsChangeListener listener : changeListeners) {
            try {
                listener.onSettingChanged(key, oldValue, newValue);
            } catch (Exception e) {
                log.warn("Error notifying settings change listener", e);
            }
        }
    }

    private void validateValue(SettingEntity entity, String value) {
        if (value == null) {
            return; // Null allowed for optional settings
        }

        String rules = entity.getValidationRules();
        if (rules == null || rules.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> validation = objectMapper.readValue(rules, new TypeReference<>() {
            });

            // Validate number range
            if (validation.containsKey("min") || validation.containsKey("max")) {
                double numValue = Double.parseDouble(value);
                if (validation.containsKey("min")) {
                    double min = ((Number) validation.get("min")).doubleValue();
                    if (numValue < min) {
                        throw new IllegalArgumentException(
                                String.format("Value for '%s' must be at least %s", entity.getKey(), min));
                    }
                }
                if (validation.containsKey("max")) {
                    double max = ((Number) validation.get("max")).doubleValue();
                    if (numValue > max) {
                        throw new IllegalArgumentException(
                                String.format("Value for '%s' must be at most %s", entity.getKey(), max));
                    }
                }
            }

            // Validate enum options
            if (validation.containsKey("options")) {
                @SuppressWarnings("unchecked")
                List<String> options = (List<String>) validation.get("options");
                if (!options.contains(value)) {
                    throw new IllegalArgumentException(
                            String.format("Invalid value for '%s'. Must be one of: %s", entity.getKey(), options));
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Invalid validation rules for setting: {}", entity.getKey());
        }
    }

    private String encryptValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return encryptionService.encrypt(value);
    }

    private String decryptValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            return encryptionService.decrypt(value);
        } catch (Exception e) {
            log.warn("Failed to decrypt setting value, returning empty");
            return "";
        }
    }

    private SettingDTO toDTO(SettingEntity entity) {
        String value = entity.getType() == SettingType.PASSWORD
                ? "********" // Mask password values in DTO
                : entity.getValue();

        return new SettingDTO(
                entity.getId(),
                entity.getKey(),
                value,
                entity.getCategory(),
                entity.getType(),
                entity.getLabel(),
                entity.getDescription(),
                entity.isVisible(),
                entity.isRequiresRestart(),
                entity.getDisplayOrder(),
                entity.getValidationRules(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
