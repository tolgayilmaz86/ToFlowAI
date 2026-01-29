package io.toflowai.common.dto;

import java.time.Instant;

import io.toflowai.common.enums.SettingCategory;
import io.toflowai.common.enums.SettingType;

/**
 * Data Transfer Object for Settings.
 * Used for UI communication and API responses.
 */
public record SettingDTO(
        Long id,
        String key,
        String value,
        SettingCategory category,
        SettingType type,
        String label,
        String description,
        boolean visible,
        boolean requiresRestart,
        Integer displayOrder,
        String validationRules,
        Instant createdAt,
        Instant updatedAt) {
    /**
     * Create a minimal SettingDTO for simple key-value updates.
     */
    public static SettingDTO of(String key, String value) {
        return new SettingDTO(null, key, value, null, null, null, null, true, false, 0, null, null, null);
    }

    /**
     * Create a SettingDTO with category and type.
     */
    public static SettingDTO of(String key, String value, SettingCategory category, SettingType type) {
        return new SettingDTO(null, key, value, category, type, null, null, true, false, 0, null, null, null);
    }

    /**
     * Create a full SettingDTO with all metadata.
     */
    public static SettingDTO full(
            String key,
            String value,
            SettingCategory category,
            SettingType type,
            String label,
            String description,
            boolean visible,
            boolean requiresRestart,
            int displayOrder,
            String validationRules) {
        return new SettingDTO(null, key, value, category, type, label, description, visible, requiresRestart,
                displayOrder, validationRules, null, null);
    }

    /**
     * Get the value as boolean.
     */
    public boolean asBoolean() {
        return Boolean.parseBoolean(value);
    }

    /**
     * Get the value as integer.
     */
    public int asInt() {
        return value != null ? Integer.parseInt(value) : 0;
    }

    /**
     * Get the value as long.
     */
    public long asLong() {
        return value != null ? Long.parseLong(value) : 0L;
    }

    /**
     * Get the value as double.
     */
    public double asDouble() {
        return value != null ? Double.parseDouble(value) : 0.0;
    }

    /**
     * Get the value as string (or default if null).
     */
    public String asString(String defaultValue) {
        return value != null ? value : defaultValue;
    }
}
