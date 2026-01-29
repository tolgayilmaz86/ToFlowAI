package io.toflowai.common.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.toflowai.common.dto.SettingDTO;
import io.toflowai.common.enums.SettingCategory;

/**
 * Service interface for managing application settings.
 */
public interface SettingsServiceInterface {

    /**
     * Get all settings.
     */
    List<SettingDTO> findAll();

    /**
     * Get all visible settings.
     */
    List<SettingDTO> findAllVisible();

    /**
     * Get settings by category.
     */
    List<SettingDTO> findByCategory(SettingCategory category);

    /**
     * Get a single setting by key.
     */
    Optional<SettingDTO> findByKey(String key);

    /**
     * Get setting value by key, or default if not found.
     */
    String getValue(String key, String defaultValue);

    /**
     * Get setting value as boolean.
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * Get setting value as integer.
     */
    int getInt(String key, int defaultValue);

    /**
     * Get setting value as long.
     */
    long getLong(String key, long defaultValue);

    /**
     * Get setting value as double.
     */
    double getDouble(String key, double defaultValue);

    /**
     * Set a single setting value.
     */
    SettingDTO setValue(String key, String value);

    /**
     * Set multiple settings at once.
     */
    List<SettingDTO> setValues(Map<String, String> settings);

    /**
     * Reset a setting to its default value.
     */
    SettingDTO resetToDefault(String key);

    /**
     * Reset all settings in a category to defaults.
     */
    List<SettingDTO> resetCategoryToDefaults(SettingCategory category);

    /**
     * Reset all settings to defaults.
     */
    void resetAllToDefaults();

    /**
     * Search settings by query.
     */
    List<SettingDTO> search(String query);

    /**
     * Export all settings as JSON.
     */
    String exportAsJson();

    /**
     * Import settings from JSON.
     */
    void importFromJson(String json);

    /**
     * Initialize default settings (run on startup).
     */
    void initializeDefaults();

    /**
     * Add a listener for setting changes.
     */
    void addChangeListener(SettingsChangeListener listener);

    /**
     * Remove a change listener.
     */
    void removeChangeListener(SettingsChangeListener listener);

    /**
     * Listener interface for setting changes.
     */
    interface SettingsChangeListener {
        void onSettingChanged(String key, String oldValue, String newValue);
    }
}
