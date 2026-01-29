package io.toflowai.common.enums;

/**
 * Types of credentials supported by the application.
 */
public enum CredentialType {
    API_KEY("API Key", "Simple API key authentication"),
    HTTP_BASIC("HTTP Basic", "Username and password"),
    HTTP_BEARER("Bearer Token", "Bearer token authentication"),
    OAUTH2("OAuth 2.0", "OAuth 2.0 authentication flow"),
    CUSTOM_HEADER("Custom Header", "Custom HTTP header authentication");

    private final String displayName;
    private final String description;

    CredentialType(String displayName, String description) {
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
