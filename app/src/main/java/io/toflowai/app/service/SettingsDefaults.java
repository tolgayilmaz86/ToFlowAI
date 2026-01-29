package io.toflowai.app.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.toflowai.common.dto.SettingDTO;
import io.toflowai.common.enums.SettingCategory;
import io.toflowai.common.enums.SettingType;

/**
 * Default settings definitions for the application.
 * Contains all setting keys, default values, and metadata.
 */
public final class SettingsDefaults {

    private SettingsDefaults() {
        // Utility class
    }

    // ========================================
    // Setting Key Constants
    // ========================================

    // General Settings
    public static final String GENERAL_THEME = "general.theme";
    public static final String GENERAL_LANGUAGE = "general.language";
    public static final String GENERAL_AUTO_SAVE = "general.autoSave";
    public static final String GENERAL_AUTO_SAVE_INTERVAL = "general.autoSaveInterval";
    public static final String GENERAL_SHOW_WELCOME = "general.showWelcome";
    public static final String GENERAL_RECENT_LIMIT = "general.recentLimit";
    public static final String GENERAL_CONFIRM_DELETE = "general.confirmDelete";
    public static final String GENERAL_CHECK_UPDATES = "general.checkUpdates";

    // Editor Settings
    public static final String EDITOR_GRID_SIZE = "editor.gridSize";
    public static final String EDITOR_SNAP_TO_GRID = "editor.snapToGrid";
    public static final String EDITOR_SHOW_GRID = "editor.showGrid";
    public static final String EDITOR_SHOW_MINIMAP = "editor.showMinimap";
    public static final String EDITOR_DEFAULT_ZOOM = "editor.defaultZoom";
    public static final String EDITOR_ANIMATION_SPEED = "editor.animationSpeed";
    public static final String EDITOR_NODE_SPACING = "editor.nodeSpacing";
    public static final String EDITOR_CONNECTION_STYLE = "editor.connectionStyle";

    // Execution Settings
    public static final String EXECUTION_DEFAULT_TIMEOUT = "execution.defaultTimeout";
    public static final String EXECUTION_MAX_PARALLEL = "execution.maxParallel";
    public static final String EXECUTION_RETRY_ATTEMPTS = "execution.retryAttempts";
    public static final String EXECUTION_RETRY_DELAY = "execution.retryDelay";
    public static final String EXECUTION_LOG_LEVEL = "execution.logLevel";
    public static final String EXECUTION_HISTORY_LIMIT = "execution.historyLimit";
    public static final String EXECUTION_HISTORY_RETENTION = "execution.historyRetention";
    public static final String EXECUTION_SHOW_CONSOLE = "execution.showConsole";

    // AI Provider Settings - OpenAI
    public static final String AI_OPENAI_API_KEY = "ai.openai.apiKey";
    public static final String AI_OPENAI_ORG_ID = "ai.openai.orgId";
    public static final String AI_OPENAI_BASE_URL = "ai.openai.baseUrl";
    public static final String AI_OPENAI_DEFAULT_MODEL = "ai.openai.defaultModel";

    // AI Provider Settings - Anthropic
    public static final String AI_ANTHROPIC_API_KEY = "ai.anthropic.apiKey";
    public static final String AI_ANTHROPIC_BASE_URL = "ai.anthropic.baseUrl";
    public static final String AI_ANTHROPIC_DEFAULT_MODEL = "ai.anthropic.defaultModel";

    // AI Provider Settings - Ollama
    public static final String AI_OLLAMA_BASE_URL = "ai.ollama.baseUrl";
    public static final String AI_OLLAMA_DEFAULT_MODEL = "ai.ollama.defaultModel";

    // AI Provider Settings - Azure OpenAI
    public static final String AI_AZURE_API_KEY = "ai.azure.apiKey";
    public static final String AI_AZURE_ENDPOINT = "ai.azure.endpoint";
    public static final String AI_AZURE_DEPLOYMENT = "ai.azure.deployment";
    public static final String AI_AZURE_API_VERSION = "ai.azure.apiVersion";

    // HTTP/Network Settings
    public static final String HTTP_USER_AGENT = "http.userAgent";
    public static final String HTTP_CONNECT_TIMEOUT = "http.connectTimeout";
    public static final String HTTP_READ_TIMEOUT = "http.readTimeout";
    public static final String HTTP_FOLLOW_REDIRECTS = "http.followRedirects";
    public static final String HTTP_MAX_REDIRECTS = "http.maxRedirects";
    public static final String HTTP_PROXY_ENABLED = "http.proxy.enabled";
    public static final String HTTP_PROXY_HOST = "http.proxy.host";
    public static final String HTTP_PROXY_PORT = "http.proxy.port";
    public static final String HTTP_PROXY_AUTH = "http.proxy.auth";
    public static final String HTTP_PROXY_USERNAME = "http.proxy.username";
    public static final String HTTP_PROXY_PASSWORD = "http.proxy.password";

    // Database & Storage Settings
    public static final String STORAGE_DATABASE_PATH = "storage.databasePath";
    public static final String STORAGE_BACKUP_ENABLED = "storage.backup.enabled";
    public static final String STORAGE_BACKUP_INTERVAL = "storage.backup.interval";
    public static final String STORAGE_BACKUP_RETENTION = "storage.backup.retention";
    public static final String STORAGE_BACKUP_PATH = "storage.backup.path";

    // Webhook & Server Settings
    public static final String SERVER_PORT = "server.port";
    public static final String SERVER_WEBHOOK_BASE_URL = "server.webhookBaseUrl";
    public static final String SERVER_WEBHOOKS_ENABLED = "server.webhooksEnabled";
    public static final String SERVER_WEBHOOK_SECRET = "server.webhookSecret";
    public static final String SERVER_CORS_ENABLED = "server.cors.enabled";
    public static final String SERVER_CORS_ORIGINS = "server.cors.origins";

    // Notification Settings
    public static final String NOTIFY_ENABLED = "notify.enabled";
    public static final String NOTIFY_ON_SUCCESS = "notify.onSuccess";
    public static final String NOTIFY_ON_FAILURE = "notify.onFailure";
    public static final String NOTIFY_SOUND_ENABLED = "notify.soundEnabled";
    public static final String NOTIFY_EMAIL_ENABLED = "notify.email.enabled";
    public static final String NOTIFY_EMAIL_SMTP_HOST = "notify.email.smtpHost";
    public static final String NOTIFY_EMAIL_SMTP_PORT = "notify.email.smtpPort";
    public static final String NOTIFY_EMAIL_FROM = "notify.email.from";
    public static final String NOTIFY_EMAIL_TO = "notify.email.to";

    // Advanced Settings
    public static final String ADVANCED_DEV_MODE = "advanced.devMode";
    public static final String ADVANCED_SHOW_INTERNAL_NODES = "advanced.showInternalNodes";
    public static final String ADVANCED_SCRIPT_TIMEOUT = "advanced.scriptTimeout";
    public static final String ADVANCED_MAX_EXPRESSION_DEPTH = "advanced.maxExpressionDepth";
    public static final String ADVANCED_TELEMETRY = "advanced.telemetry";
    public static final String ADVANCED_LOG_PATH = "advanced.logPath";
    public static final String ADVANCED_LOG_MAX_SIZE = "advanced.logMaxSize";
    public static final String ADVANCED_LOG_ROTATION = "advanced.logRotation";

    // ========================================
    // Default Settings List
    // ========================================

    private static final List<SettingDTO> DEFAULTS = new ArrayList<>();

    static {
        int order;

        // --- General Settings ---
        order = 0;
        DEFAULTS.add(setting(GENERAL_THEME, "nord-dark", SettingCategory.GENERAL, SettingType.ENUM,
                "Theme", "Application color theme", order++, false,
                "{\"options\":[\"nord-dark\",\"nord-light\",\"system\"]}"));
        DEFAULTS.add(setting(GENERAL_LANGUAGE, "en", SettingCategory.GENERAL, SettingType.ENUM,
                "Language", "UI language", order++, true, "{\"options\":[\"en\",\"de\",\"fr\",\"es\"]}"));
        DEFAULTS.add(setting(GENERAL_AUTO_SAVE, "true", SettingCategory.GENERAL, SettingType.BOOLEAN,
                "Auto-save", "Automatically save workflows", order++, false, null));
        DEFAULTS.add(setting(GENERAL_AUTO_SAVE_INTERVAL, "30", SettingCategory.GENERAL, SettingType.INTEGER,
                "Auto-save interval", "Seconds between auto-saves", order++, false, "{\"min\":5,\"max\":300}"));
        DEFAULTS.add(setting(GENERAL_SHOW_WELCOME, "true", SettingCategory.GENERAL, SettingType.BOOLEAN,
                "Show welcome screen", "Show welcome screen on startup", order++, false, null));
        DEFAULTS.add(setting(GENERAL_RECENT_LIMIT, "10", SettingCategory.GENERAL, SettingType.INTEGER,
                "Recent workflows limit", "Maximum recent workflows to show", order++, false,
                "{\"min\":1,\"max\":50}"));
        DEFAULTS.add(setting(GENERAL_CONFIRM_DELETE, "true", SettingCategory.GENERAL, SettingType.BOOLEAN,
                "Confirm on delete", "Ask before deleting workflows", order++, false, null));
        DEFAULTS.add(setting(GENERAL_CHECK_UPDATES, "true", SettingCategory.GENERAL, SettingType.BOOLEAN,
                "Check for updates", "Automatically check for updates", order++, false, null));

        // --- Editor Settings ---
        order = 0;
        DEFAULTS.add(setting(EDITOR_GRID_SIZE, "20", SettingCategory.EDITOR, SettingType.INTEGER,
                "Grid size", "Canvas grid size in pixels", order++, false, "{\"min\":10,\"max\":100}"));
        DEFAULTS.add(setting(EDITOR_SNAP_TO_GRID, "true", SettingCategory.EDITOR, SettingType.BOOLEAN,
                "Snap to grid", "Snap nodes to grid when moving", order++, false, null));
        DEFAULTS.add(setting(EDITOR_SHOW_GRID, "true", SettingCategory.EDITOR, SettingType.BOOLEAN,
                "Show grid", "Display grid lines on canvas", order++, false, null));
        DEFAULTS.add(setting(EDITOR_SHOW_MINIMAP, "true", SettingCategory.EDITOR, SettingType.BOOLEAN,
                "Show minimap", "Show minimap in canvas corner", order++, false, null));
        DEFAULTS.add(setting(EDITOR_DEFAULT_ZOOM, "100", SettingCategory.EDITOR, SettingType.INTEGER,
                "Default zoom", "Initial zoom level (%)", order++, false, "{\"min\":25,\"max\":200}"));
        DEFAULTS.add(setting(EDITOR_ANIMATION_SPEED, "normal", SettingCategory.EDITOR, SettingType.ENUM,
                "Animation speed", "UI animation speed", order++, false,
                "{\"options\":[\"slow\",\"normal\",\"fast\",\"none\"]}"));
        DEFAULTS.add(setting(EDITOR_NODE_SPACING, "50", SettingCategory.EDITOR, SettingType.INTEGER,
                "Node spacing", "Default spacing for auto-layout", order++, false, "{\"min\":20,\"max\":200}"));
        DEFAULTS.add(setting(EDITOR_CONNECTION_STYLE, "bezier", SettingCategory.EDITOR, SettingType.ENUM,
                "Connection style", "Line style for connections", order++, false,
                "{\"options\":[\"bezier\",\"straight\",\"step\"]}"));

        // --- Execution Settings ---
        order = 0;
        DEFAULTS.add(setting(EXECUTION_DEFAULT_TIMEOUT, "30000", SettingCategory.EXECUTION, SettingType.LONG,
                "Default timeout", "Default node timeout in ms", order++, false, "{\"min\":1000,\"max\":300000}"));
        DEFAULTS.add(setting(EXECUTION_MAX_PARALLEL, "10", SettingCategory.EXECUTION, SettingType.INTEGER,
                "Max parallel nodes", "Maximum concurrent node executions", order++, false, "{\"min\":1,\"max\":100}"));
        DEFAULTS.add(setting(EXECUTION_RETRY_ATTEMPTS, "3", SettingCategory.EXECUTION, SettingType.INTEGER,
                "Retry attempts", "Default retry count for failed nodes", order++, false, "{\"min\":0,\"max\":10}"));
        DEFAULTS.add(setting(EXECUTION_RETRY_DELAY, "1000", SettingCategory.EXECUTION, SettingType.LONG,
                "Retry delay", "Default delay between retries (ms)", order++, false, "{\"min\":100,\"max\":60000}"));
        DEFAULTS.add(setting(EXECUTION_LOG_LEVEL, "INFO", SettingCategory.EXECUTION, SettingType.ENUM,
                "Log level", "Minimum execution log level", order++, false,
                "{\"options\":[\"TRACE\",\"DEBUG\",\"INFO\",\"WARN\",\"ERROR\"]}"));
        DEFAULTS.add(setting(EXECUTION_HISTORY_LIMIT, "100", SettingCategory.EXECUTION, SettingType.INTEGER,
                "History limit", "Max executions per workflow", order++, false, "{\"min\":10,\"max\":1000}"));
        DEFAULTS.add(setting(EXECUTION_HISTORY_RETENTION, "30", SettingCategory.EXECUTION, SettingType.INTEGER,
                "History retention", "Days to keep execution history", order++, false, "{\"min\":1,\"max\":365}"));
        DEFAULTS.add(setting(EXECUTION_SHOW_CONSOLE, "false", SettingCategory.EXECUTION, SettingType.BOOLEAN,
                "Show console on run", "Auto-open console when running", order++, false, null));

        // --- AI Provider Settings ---
        order = 0;
        // OpenAI
        DEFAULTS.add(setting(AI_OPENAI_API_KEY, "", SettingCategory.AI_PROVIDERS, SettingType.PASSWORD,
                "OpenAI API Key", "Your OpenAI API key", order++, false, null));
        DEFAULTS.add(setting(AI_OPENAI_ORG_ID, "", SettingCategory.AI_PROVIDERS, SettingType.STRING,
                "OpenAI Organization ID", "Optional organization ID", order++, false, null));
        DEFAULTS.add(setting(AI_OPENAI_BASE_URL, "https://api.openai.com/v1", SettingCategory.AI_PROVIDERS,
                SettingType.STRING,
                "OpenAI Base URL", "API endpoint (for proxies)", order++, false, null));
        DEFAULTS.add(setting(AI_OPENAI_DEFAULT_MODEL, "gpt-4o", SettingCategory.AI_PROVIDERS, SettingType.STRING,
                "OpenAI Default Model", "Default model for LLM nodes", order++, false, null));
        // Anthropic
        DEFAULTS.add(setting(AI_ANTHROPIC_API_KEY, "", SettingCategory.AI_PROVIDERS, SettingType.PASSWORD,
                "Anthropic API Key", "Your Anthropic API key", order++, false, null));
        DEFAULTS.add(setting(AI_ANTHROPIC_BASE_URL, "https://api.anthropic.com", SettingCategory.AI_PROVIDERS,
                SettingType.STRING,
                "Anthropic Base URL", "API endpoint", order++, false, null));
        DEFAULTS.add(setting(AI_ANTHROPIC_DEFAULT_MODEL, "claude-sonnet-4-20250514", SettingCategory.AI_PROVIDERS,
                SettingType.STRING,
                "Anthropic Default Model", "Default model", order++, false, null));
        // Ollama
        DEFAULTS.add(
                setting(AI_OLLAMA_BASE_URL, "http://localhost:11434", SettingCategory.AI_PROVIDERS, SettingType.STRING,
                        "Ollama Base URL", "Ollama server URL", order++, false, null));
        DEFAULTS.add(setting(AI_OLLAMA_DEFAULT_MODEL, "llama3.2", SettingCategory.AI_PROVIDERS, SettingType.STRING,
                "Ollama Default Model", "Default local model", order++, false, null));
        // Azure OpenAI
        DEFAULTS.add(setting(AI_AZURE_API_KEY, "", SettingCategory.AI_PROVIDERS, SettingType.PASSWORD,
                "Azure API Key", "Azure OpenAI API key", order++, false, null));
        DEFAULTS.add(setting(AI_AZURE_ENDPOINT, "", SettingCategory.AI_PROVIDERS, SettingType.STRING,
                "Azure Endpoint", "Azure OpenAI endpoint URL", order++, false, null));
        DEFAULTS.add(setting(AI_AZURE_DEPLOYMENT, "", SettingCategory.AI_PROVIDERS, SettingType.STRING,
                "Azure Deployment", "Model deployment name", order++, false, null));
        DEFAULTS.add(setting(AI_AZURE_API_VERSION, "2024-02-15", SettingCategory.AI_PROVIDERS, SettingType.STRING,
                "Azure API Version", "API version string", order++, false, null));

        // --- HTTP/Network Settings ---
        order = 0;
        DEFAULTS.add(setting(HTTP_USER_AGENT, "ToFlowAI/1.0", SettingCategory.HTTP_NETWORK, SettingType.STRING,
                "User Agent", "HTTP User-Agent header", order++, false, null));
        DEFAULTS.add(setting(HTTP_CONNECT_TIMEOUT, "10000", SettingCategory.HTTP_NETWORK, SettingType.LONG,
                "Connection timeout", "Connection timeout (ms)", order++, false, "{\"min\":1000,\"max\":60000}"));
        DEFAULTS.add(setting(HTTP_READ_TIMEOUT, "30000", SettingCategory.HTTP_NETWORK, SettingType.LONG,
                "Read timeout", "Read timeout (ms)", order++, false, "{\"min\":1000,\"max\":300000}"));
        DEFAULTS.add(setting(HTTP_FOLLOW_REDIRECTS, "true", SettingCategory.HTTP_NETWORK, SettingType.BOOLEAN,
                "Follow redirects", "Automatically follow redirects", order++, false, null));
        DEFAULTS.add(setting(HTTP_MAX_REDIRECTS, "5", SettingCategory.HTTP_NETWORK, SettingType.INTEGER,
                "Max redirects", "Maximum redirect hops", order++, false, "{\"min\":1,\"max\":20}"));
        DEFAULTS.add(setting(HTTP_PROXY_ENABLED, "false", SettingCategory.HTTP_NETWORK, SettingType.BOOLEAN,
                "Use proxy", "Use proxy for HTTP requests", order++, false, null));
        DEFAULTS.add(setting(HTTP_PROXY_HOST, "", SettingCategory.HTTP_NETWORK, SettingType.STRING,
                "Proxy host", "Proxy server hostname", order++, false, null));
        DEFAULTS.add(setting(HTTP_PROXY_PORT, "8080", SettingCategory.HTTP_NETWORK, SettingType.INTEGER,
                "Proxy port", "Proxy server port", order++, false, "{\"min\":1,\"max\":65535}"));
        DEFAULTS.add(setting(HTTP_PROXY_AUTH, "false", SettingCategory.HTTP_NETWORK, SettingType.BOOLEAN,
                "Proxy authentication", "Proxy requires authentication", order++, false, null));
        DEFAULTS.add(setting(HTTP_PROXY_USERNAME, "", SettingCategory.HTTP_NETWORK, SettingType.STRING,
                "Proxy username", "Proxy username", order++, false, null));
        DEFAULTS.add(setting(HTTP_PROXY_PASSWORD, "", SettingCategory.HTTP_NETWORK, SettingType.PASSWORD,
                "Proxy password", "Proxy password", order++, false, null));

        // --- Database & Storage Settings ---
        order = 0;
        DEFAULTS.add(setting(STORAGE_DATABASE_PATH, "./data", SettingCategory.DATABASE_STORAGE, SettingType.PATH,
                "Database location", "Data directory path", order++, true, null));
        DEFAULTS.add(setting(STORAGE_BACKUP_ENABLED, "false", SettingCategory.DATABASE_STORAGE, SettingType.BOOLEAN,
                "Automatic backups", "Enable automatic backups", order++, false, null));
        DEFAULTS.add(setting(STORAGE_BACKUP_INTERVAL, "daily", SettingCategory.DATABASE_STORAGE, SettingType.ENUM,
                "Backup interval", "Backup frequency", order++, false,
                "{\"options\":[\"hourly\",\"daily\",\"weekly\"]}"));
        DEFAULTS.add(setting(STORAGE_BACKUP_RETENTION, "7", SettingCategory.DATABASE_STORAGE, SettingType.INTEGER,
                "Backup retention", "Days to keep backups", order++, false, "{\"min\":1,\"max\":365}"));
        DEFAULTS.add(setting(STORAGE_BACKUP_PATH, "./backups", SettingCategory.DATABASE_STORAGE, SettingType.PATH,
                "Backup location", "Backup directory", order++, false, null));

        // --- Webhook & Server Settings ---
        order = 0;
        DEFAULTS.add(setting(SERVER_PORT, "8080", SettingCategory.WEBHOOK_SERVER, SettingType.INTEGER,
                "Server port", "HTTP server port", order++, true, "{\"min\":1,\"max\":65535}"));
        DEFAULTS.add(setting(SERVER_WEBHOOK_BASE_URL, "http://localhost:8080", SettingCategory.WEBHOOK_SERVER,
                SettingType.STRING,
                "Webhook base URL", "External webhook URL", order++, false, null));
        DEFAULTS.add(setting(SERVER_WEBHOOKS_ENABLED, "true", SettingCategory.WEBHOOK_SERVER, SettingType.BOOLEAN,
                "Enable webhooks", "Accept incoming webhooks", order++, false, null));
        DEFAULTS.add(setting(SERVER_WEBHOOK_SECRET, "", SettingCategory.WEBHOOK_SERVER, SettingType.PASSWORD,
                "Webhook secret", "HMAC secret for validation", order++, false, null));
        DEFAULTS.add(setting(SERVER_CORS_ENABLED, "false", SettingCategory.WEBHOOK_SERVER, SettingType.BOOLEAN,
                "Enable CORS", "Allow cross-origin requests", order++, false, null));
        DEFAULTS.add(setting(SERVER_CORS_ORIGINS, "*", SettingCategory.WEBHOOK_SERVER, SettingType.STRING,
                "CORS origins", "Allowed origins (comma-separated)", order++, false, null));

        // --- Notification Settings ---
        order = 0;
        DEFAULTS.add(setting(NOTIFY_ENABLED, "true", SettingCategory.NOTIFICATIONS, SettingType.BOOLEAN,
                "Enable notifications", "Show system notifications", order++, false, null));
        DEFAULTS.add(setting(NOTIFY_ON_SUCCESS, "false", SettingCategory.NOTIFICATIONS, SettingType.BOOLEAN,
                "Notify on success", "Notify when workflow completes", order++, false, null));
        DEFAULTS.add(setting(NOTIFY_ON_FAILURE, "true", SettingCategory.NOTIFICATIONS, SettingType.BOOLEAN,
                "Notify on failure", "Notify when workflow fails", order++, false, null));
        DEFAULTS.add(setting(NOTIFY_SOUND_ENABLED, "false", SettingCategory.NOTIFICATIONS, SettingType.BOOLEAN,
                "Sound enabled", "Play sound with notifications", order++, false, null));
        DEFAULTS.add(setting(NOTIFY_EMAIL_ENABLED, "false", SettingCategory.NOTIFICATIONS, SettingType.BOOLEAN,
                "Email notifications", "Send email notifications", order++, false, null));
        DEFAULTS.add(setting(NOTIFY_EMAIL_SMTP_HOST, "", SettingCategory.NOTIFICATIONS, SettingType.STRING,
                "SMTP server", "SMTP server address", order++, false, null));
        DEFAULTS.add(setting(NOTIFY_EMAIL_SMTP_PORT, "587", SettingCategory.NOTIFICATIONS, SettingType.INTEGER,
                "SMTP port", "SMTP server port", order++, false, "{\"min\":1,\"max\":65535}"));
        DEFAULTS.add(setting(NOTIFY_EMAIL_FROM, "", SettingCategory.NOTIFICATIONS, SettingType.STRING,
                "From address", "Sender email address", order++, false, null));
        DEFAULTS.add(setting(NOTIFY_EMAIL_TO, "", SettingCategory.NOTIFICATIONS, SettingType.STRING,
                "To address", "Recipient email address", order++, false, null));

        // --- Advanced Settings ---
        order = 0;
        DEFAULTS.add(setting(ADVANCED_DEV_MODE, "false", SettingCategory.ADVANCED, SettingType.BOOLEAN,
                "Developer mode", "Enable debug features", order++, false, null));
        DEFAULTS.add(setting(ADVANCED_SHOW_INTERNAL_NODES, "false", SettingCategory.ADVANCED, SettingType.BOOLEAN,
                "Show internal nodes", "Show system node types", order++, false, null));
        DEFAULTS.add(setting(ADVANCED_SCRIPT_TIMEOUT, "60000", SettingCategory.ADVANCED, SettingType.LONG,
                "Script timeout", "Code node timeout (ms)", order++, false, "{\"min\":1000,\"max\":600000}"));
        DEFAULTS.add(setting(ADVANCED_MAX_EXPRESSION_DEPTH, "10", SettingCategory.ADVANCED, SettingType.INTEGER,
                "Max expression depth", "Max nested expressions", order++, false, "{\"min\":1,\"max\":50}"));
        DEFAULTS.add(setting(ADVANCED_TELEMETRY, "false", SettingCategory.ADVANCED, SettingType.BOOLEAN,
                "Telemetry", "Anonymous usage statistics", order++, false, null));
        DEFAULTS.add(setting(ADVANCED_LOG_PATH, "./logs", SettingCategory.ADVANCED, SettingType.PATH,
                "Log file location", "Log file directory", order++, true, null));
        DEFAULTS.add(setting(ADVANCED_LOG_MAX_SIZE, "10", SettingCategory.ADVANCED, SettingType.INTEGER,
                "Max log file size", "Max log file size (MB)", order++, false, "{\"min\":1,\"max\":100}"));
        DEFAULTS.add(setting(ADVANCED_LOG_ROTATION, "5", SettingCategory.ADVANCED, SettingType.INTEGER,
                "Log rotation", "Number of log files to keep", order++, false, "{\"min\":1,\"max\":20}"));
    }

    /**
     * Helper method to create a SettingDTO.
     */
    private static SettingDTO setting(String key, String value, SettingCategory category, SettingType type,
            String label, String description, int order, boolean requiresRestart, String validation) {
        return SettingDTO.full(key, value, category, type, label, description, true, requiresRestart, order,
                validation);
    }

    /**
     * Get all default settings.
     */
    public static List<SettingDTO> getAll() {
        return Collections.unmodifiableList(DEFAULTS);
    }

    /**
     * Get default settings by category.
     */
    public static List<SettingDTO> getByCategory(SettingCategory category) {
        return DEFAULTS.stream()
                .filter(s -> s.category() == category)
                .toList();
    }

    /**
     * Get default value for a setting key.
     */
    public static String getDefault(String key) {
        return DEFAULTS.stream()
                .filter(s -> s.key().equals(key))
                .findFirst()
                .map(SettingDTO::value)
                .orElse(null);
    }

    /**
     * Get setting definition by key.
     */
    public static SettingDTO getDefinition(String key) {
        return DEFAULTS.stream()
                .filter(s -> s.key().equals(key))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all settings as a map of key -> default value.
     */
    public static Map<String, String> asMap() {
        return DEFAULTS.stream()
                .collect(Collectors.toMap(SettingDTO::key, SettingDTO::value));
    }

    /**
     * Get all setting keys.
     */
    public static List<String> getAllKeys() {
        return DEFAULTS.stream()
                .map(SettingDTO::key)
                .toList();
    }
}
