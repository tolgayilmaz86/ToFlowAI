package io.toflowai.app.database.model;

import java.time.Instant;

import io.toflowai.common.enums.SettingCategory;
import io.toflowai.common.enums.SettingType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * JPA Entity for Application Settings.
 * Stores user preferences and application configuration as key-value pairs.
 * Sensitive values (type=PASSWORD) are stored encrypted.
 */
@Entity
@Table(name = "settings", indexes = {
        @Index(name = "idx_settings_key", columnList = "setting_key", unique = true),
        @Index(name = "idx_settings_category", columnList = "category")
})
public class SettingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique key identifying the setting (e.g., "general.theme",
     * "ai.openai.apiKey").
     */
    @Column(name = "setting_key", nullable = false, unique = true, length = 255)
    private String key;

    /**
     * The setting value (stored as string, converted based on type).
     * For PASSWORD type, this is encrypted.
     */
    @Column(name = "setting_value", columnDefinition = "CLOB")
    private String value;

    /**
     * Category for organizing settings in the UI.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettingCategory category;

    /**
     * Data type for proper parsing and validation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "setting_type", nullable = false)
    private SettingType type;

    /**
     * Human-readable label for display.
     */
    @Column(length = 255)
    private String label;

    /**
     * Description/help text for the setting.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Whether this setting is visible in the UI.
     */
    @Column(name = "is_visible", nullable = false)
    private boolean visible = true;

    /**
     * Whether this setting requires application restart.
     */
    @Column(name = "requires_restart", nullable = false)
    private boolean requiresRestart = false;

    /**
     * Order for display within category (lower = first).
     */
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    /**
     * Validation rules as JSON (e.g., {"min": 1, "max": 100}).
     */
    @Column(name = "validation_rules", columnDefinition = "TEXT")
    private String validationRules;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Default constructor for JPA
    protected SettingEntity() {
    }

    public SettingEntity(String key, String value, SettingCategory category, SettingType type) {
        this.key = key;
        this.value = value;
        this.category = category;
        this.type = type;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public SettingCategory getCategory() {
        return category;
    }

    public void setCategory(SettingCategory category) {
        this.category = category;
    }

    public SettingType getType() {
        return type;
    }

    public void setType(SettingType type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isRequiresRestart() {
        return requiresRestart;
    }

    public void setRequiresRestart(boolean requiresRestart) {
        this.requiresRestart = requiresRestart;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getValidationRules() {
        return validationRules;
    }

    public void setValidationRules(String validationRules) {
        this.validationRules = validationRules;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
