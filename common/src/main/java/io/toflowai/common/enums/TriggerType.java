package io.toflowai.common.enums;

/**
 * Types of workflow triggers.
 */
public enum TriggerType {
    MANUAL("Manual", "Triggered manually by user"),
    SCHEDULE("Schedule", "Triggered on a schedule (cron)"),
    WEBHOOK("Webhook", "Triggered by HTTP webhook"),
    EVENT("Event", "Triggered by application event");

    private final String displayName;
    private final String description;

    TriggerType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
