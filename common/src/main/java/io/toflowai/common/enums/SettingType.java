package io.toflowai.common.enums;

/**
 * Data types for setting values.
 */
public enum SettingType {
    STRING,
    INTEGER,
    LONG,
    DOUBLE,
    BOOLEAN,
    PASSWORD, // Encrypted string
    PATH, // File/directory path
    JSON, // Complex nested object as JSON
    ENUM // Enumeration value stored as string
}
