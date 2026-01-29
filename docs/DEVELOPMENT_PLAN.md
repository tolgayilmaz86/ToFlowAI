# ToFlowAI Development Plan

> **Last Updated:** January 30, 2026  
> **Java Version:** 25  
> **Spring Boot:** 3.5.0  
> **Gradle:** 9.2.0

---

## Project Overview

ToFlowAI is a workflow automation application built with:
- **Backend:** Spring Boot + Java 25 (Virtual Threads)
- **Frontend:** JavaFX with AtlantaFX Nord Dark theme
- **Database:** H2 (embedded) with Flyway migrations
- **Visual Style:** n8n-inspired node-based workflow editor

---

## ✅ Completed Features

### Core Infrastructure
- [x] Java 25 migration with virtual threads
- [x] Spring Boot 3.5.0 integration
- [x] Gradle 9.2.0 build system
- [x] H2 database with Flyway migrations
- [x] VS Code launch/debug configurations

### UI Framework
- [x] JavaFX canvas with zoom/pan support
- [x] n8n-style node visual design (square nodes with icons)
- [x] Node palette with drag-and-drop
- [x] Connection lines between nodes
- [x] Node selection and context menus

### Node Executors (Backend)
- [x] `ManualTriggerExecutor` - Manual workflow start
- [x] `HttpRequestExecutor` - HTTP/REST calls
- [x] `CodeExecutor` - Execute custom code
- [x] `IfExecutor` - Conditional branching
- [x] `LoopExecutor` - Loop iteration (with virtual threads)
- [x] `SetExecutor` - Set/transform data
- [x] `ScheduleTriggerExecutor` - Cron-based trigger
- [x] `WebhookTriggerExecutor` - HTTP webhook receiver
- [x] `SwitchExecutor` - Multi-branch conditionals
- [x] `MergeExecutor` - Combine multiple branches
- [x] `FilterExecutor` - Filter array data
- [x] `SortExecutor` - Sort array data
- [x] `ExecuteCommandExecutor` - Shell command execution
- [x] `LlmChatExecutor` - OpenAI/Anthropic/Ollama chat
- [x] `TextClassifierExecutor` - AI text classification
- [x] `EmbeddingExecutor` - Vector embeddings
- [x] `RagExecutor` - Retrieval-augmented generation

### Services
- [x] `WorkflowService` - CRUD operations
- [x] `ExecutionService` - Workflow execution engine
- [x] `SchedulerService` - Cron-based scheduling

---

## 🔄 In Progress / Pending TODO Items

### High Priority - UI Features

| Task | File | Line | Status |
|------|------|------|--------|
| Workflow selection dialog | `MainController.java` | 151 | ⬜ TODO |
| Clipboard paste functionality | `WorkflowCanvas.java` | 316 | ⬜ TODO |
| Save workflow to database | `WorkflowCanvas.java` | 430 | ⬜ TODO |
| Execute workflow via service | `WorkflowCanvas.java` | 435 | ⬜ TODO |
| Execute node via service | `WorkflowCanvas.java` | 898 | ⬜ TODO |
| Multi-selection support | `WorkflowCanvas.java` | 1033 | ⬜ TODO |
| Auto-layout connected nodes | `WorkflowCanvas.java` | 1063 | ⬜ TODO |

---

## 📋 Planned Features (Roadmap)

### Phase 1: Core Workflow Functionality ✅ COMPLETE
- [x] **Save/Load Workflows** - Connect UI to WorkflowService
- [x] **Execute Workflows** - Connect UI to ExecutionService
- [x] **Workflow List View** - Browse and manage workflows (WorkflowListDialog)
- [x] **Import/Export** - JSON workflow format

### Phase 2: Additional Node Types ✅ COMPLETE
- [x] **Schedule Trigger** - Cron-based trigger executor
- [x] **Webhook Trigger** - HTTP webhook receiver
- [x] **Switch Node** - Multi-branch conditionals
- [x] **Merge Node** - Combine multiple branches
- [x] **Filter Node** - Filter array data
- [x] **Sort Node** - Sort array data
- [x] **Execute Command** - Shell command execution

### Phase 3: AI Integration ✅ COMPLETE
- [x] **LLM Chat Node** - OpenAI/Anthropic API integration
- [x] **Text Classifier** - AI-based text classification
- [x] **Embedding Node** - Vector embeddings
- [x] **RAG Node** - Retrieval-augmented generation

### Phase 4: Enhanced UI
- [x] **Node Properties Panel** - Edit node parameters
- [x] **Execution Visualization** - Show running nodes
- [x] **Execution History** - View past runs
- [x] **Error Display** - Show execution errors on nodes
- [x] **Mini-map** - Canvas overview navigation
- [x] **Undo/Redo** - Command pattern implementation

### Phase 5: Data & Credentials
- [x] **Credential Manager** - Secure credential storage
- [x] **Data Preview** - Show data flowing between nodes
- [x] **Variable System** - Global/workflow variables
- [x] **Expression Editor** - Dynamic value expressions

### Phase 6: Advanced Features ✅ COMPLETE
- [x] **Subworkflows** - Nested workflow execution (SubworkflowExecutor)
- [x] **Parallel Execution** - Run branches concurrently (ParallelExecutor with virtual threads)
- [x] **Error Handling** - Try/catch nodes (TryCatchExecutor with finally support)
- [x] **Retry Logic** - Automatic retry with backoff (RetryExecutor with exponential/fibonacci/linear strategies)
- [x] **Rate Limiting** - Throttle API calls (RateLimitExecutor with token bucket and sliding window)
- [x] **Logging** - Structured execution logs (ExecutionLogger with JSON export)
- [x] **Execution Console** - UI console with hierarchical logging and node state visualization

### Phase 7: Settings & Preferences (⬜ PLANNED)
- [ ] **Settings Infrastructure** - Database model, service, DTOs
- [ ] **Settings View** - Tabbed settings UI with all categories
- [ ] **General Settings** - Theme, language, auto-save, updates
- [ ] **Editor Settings** - Grid, zoom, minimap, connection style
- [ ] **Execution Settings** - Timeouts, retries, logging levels
- [ ] **AI Providers** - OpenAI, Anthropic, Ollama, Azure configuration
- [ ] **Credentials Migration** - Move from sidebar to Settings tab
- [ ] **HTTP/Network** - Proxy, timeouts, user agent
- [ ] **Database & Storage** - Backups, cleanup, optimization
- [ ] **Webhook & Server** - Port, CORS, webhook secrets
- [ ] **Notifications** - System and email notifications
- [ ] **Advanced** - Developer mode, telemetry, logging config

---

## 🐛 Known Issues

| Issue | Description | Priority |
|-------|-------------|----------|
| VS Code Debug | Java Language Server needs workspace clean before debug | Medium |
| JavaFX Warning | "Unsupported JavaFX configuration" on startup | Low |
| H2 Console | Security password generated each run | Low |

---

## 🏗️ Architecture Notes

### Module Structure
```
ToFlowAI/
├── app/          # Spring Boot application, services, executors
├── ui/           # JavaFX UI components
├── common/       # Shared domain models and DTOs
└── tools/        # Build tools and utilities
```

### Node Execution Flow
1. User triggers workflow (manual/schedule/webhook)
2. `ExecutionService` loads workflow and creates execution context
3. For each node, find appropriate `NodeExecutor`
4. Execute node, passing data through connections
5. Handle branches (if/switch) and loops
6. Complete execution and log results

### Virtual Thread Usage
- `SchedulerService` - Uses virtual thread pool for scheduling
- `LoopExecutor` - Parallel loop iterations with virtual threads
- `ExecutionService` - Parallel branch execution

---

## 📝 Development Notes

### Running the Application
```bash
# Set Java 25
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.0.36-hotspot"

# Build
.\gradlew.bat clean build -x test

# Run
.\gradlew.bat :app:bootRun
```

### VS Code Setup
1. Clean Java workspace: `Ctrl+Shift+P` → "Java: Clean Java Language Server Workspace"
2. Use launch configurations from `.vscode/launch.json`
3. For reliable debugging, use "🔌 Attach to Remote Debug" option

---

## 📌 Next Steps (Immediate)

1. **Settings View Implementation** (Phase 7)
   - Create SettingsView with tabbed interface
   - Implement settings persistence (database + preferences)
   - Move Credentials from sidebar to Settings

2. **Execution Console Polish**
   - Finalize console window integration
   - Test hierarchical logging with real workflows

3. **UI Polish**
   - Multi-selection support
   - Auto-layout algorithms
   - Clipboard functionality

---

## ⚙️ Phase 7: Settings & Preferences

### Overview
The Settings view will consolidate all user preferences and application configuration into a single, organized interface. The current "Credentials" sidebar item will become a tab within Settings.

### Settings Categories

#### 1. General Settings
| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| Theme | Dropdown | Nord Dark | UI theme selection (Nord Dark, Nord Light, System) |
| Language | Dropdown | English | UI language/locale |
| Auto-save | Toggle | On | Automatically save workflows on change |
| Auto-save interval | Number | 30 | Seconds between auto-saves |
| Show welcome screen | Toggle | On | Show welcome/start screen on launch |
| Recent workflows limit | Number | 10 | Max items in recent workflows list |
| Confirm on delete | Toggle | On | Ask confirmation before deleting workflows |
| Check for updates | Toggle | On | Automatically check for application updates |

#### 2. Editor Settings
| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| Grid size | Number | 20 | Canvas grid size in pixels |
| Snap to grid | Toggle | On | Snap nodes to grid when moving |
| Show grid | Toggle | On | Display grid lines on canvas |
| Show minimap | Toggle | On | Show minimap in canvas corner |
| Default zoom | Slider | 100% | Initial canvas zoom level |
| Animation speed | Dropdown | Normal | UI animation speed (Slow, Normal, Fast, None) |
| Node spacing | Number | 50 | Default spacing for auto-layout |
| Connection style | Dropdown | Bezier | Line style (Bezier, Straight, Step) |

#### 3. Execution Settings
| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| Default timeout | Number | 30000 | Default node timeout in ms |
| Max parallel nodes | Number | 10 | Maximum concurrent node executions |
| Retry attempts | Number | 3 | Default retry count for failed nodes |
| Retry delay | Number | 1000 | Default delay between retries (ms) |
| Log level | Dropdown | INFO | Minimum log level (TRACE, DEBUG, INFO, WARN, ERROR) |
| Keep execution history | Number | 100 | Max executions to keep per workflow |
| Execution history retention | Number | 30 | Days to keep execution history |
| Show console on run | Toggle | Off | Auto-open execution console when running |

#### 4. AI Providers Settings
| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| **OpenAI** | Section | | |
| └ API Key | Password | - | OpenAI API key (stored encrypted) |
| └ Organization ID | Text | - | Optional organization ID |
| └ Base URL | Text | api.openai.com | API endpoint (for proxies) |
| └ Default model | Dropdown | gpt-4o | Default model for LLM nodes |
| **Anthropic** | Section | | |
| └ API Key | Password | - | Anthropic API key (stored encrypted) |
| └ Base URL | Text | api.anthropic.com | API endpoint |
| └ Default model | Dropdown | claude-3-5-sonnet | Default model |
| **Ollama** | Section | | |
| └ Base URL | Text | http://localhost:11434 | Ollama server URL |
| └ Default model | Text | llama3.2 | Default local model |
| └ Connection test | Button | - | Test Ollama connectivity |
| **Azure OpenAI** | Section | | |
| └ API Key | Password | - | Azure API key |
| └ Endpoint | Text | - | Azure OpenAI endpoint URL |
| └ Deployment name | Text | - | Model deployment name |
| └ API version | Text | 2024-02-15 | API version string |

#### 5. Credentials (Moved from sidebar)
| Feature | Description |
|---------|-------------|
| Credential list | Table view of all stored credentials |
| Add credential | Create new credential (API Key, OAuth, Basic Auth, etc.) |
| Edit credential | Modify existing credential |
| Delete credential | Remove credential (with usage check) |
| Test credential | Verify credential is valid |
| Import/Export | Backup/restore credentials (encrypted) |

#### 6. HTTP/Network Settings
| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| Default user agent | Text | ToFlowAI/1.0 | User-Agent header for HTTP requests |
| Connection timeout | Number | 10000 | Connection timeout (ms) |
| Read timeout | Number | 30000 | Read timeout (ms) |
| Follow redirects | Toggle | On | Automatically follow HTTP redirects |
| Max redirects | Number | 5 | Maximum redirect hops |
| Proxy enabled | Toggle | Off | Use proxy for HTTP requests |
| Proxy host | Text | - | Proxy server hostname |
| Proxy port | Number | 8080 | Proxy server port |
| Proxy auth | Toggle | Off | Proxy requires authentication |
| Proxy username | Text | - | Proxy username |
| Proxy password | Password | - | Proxy password |

#### 7. Database & Storage Settings
| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| Database location | Path | ./data | Data directory path |
| Backup enabled | Toggle | Off | Automatic database backups |
| Backup interval | Dropdown | Daily | Backup frequency |
| Backup retention | Number | 7 | Days to keep backups |
| Backup location | Path | ./backups | Backup directory |
| Manual backup | Button | - | Create backup now |
| Restore backup | Button | - | Restore from backup |
| Clear execution history | Button | - | Delete all execution logs |
| Vacuum database | Button | - | Optimize database size |

#### 8. Webhook & Server Settings
| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| Server port | Number | 8080 | HTTP server port |
| Webhook base URL | Text | http://localhost:8080 | External webhook URL |
| Enable webhooks | Toggle | On | Accept incoming webhooks |
| Webhook secret | Password | - | HMAC secret for webhook validation |
| CORS enabled | Toggle | Off | Allow cross-origin requests |
| CORS origins | Text | * | Allowed origins (comma-separated) |

#### 9. Notifications Settings
| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| Enable notifications | Toggle | On | Show system notifications |
| Notify on success | Toggle | Off | Notify when workflow completes |
| Notify on failure | Toggle | On | Notify when workflow fails |
| Sound enabled | Toggle | Off | Play sound with notifications |
| Email notifications | Toggle | Off | Send email notifications |
| Email SMTP server | Text | - | SMTP server address |
| Email SMTP port | Number | 587 | SMTP port |
| Email from address | Text | - | Sender email address |
| Email to address | Text | - | Recipient email address |

#### 10. Advanced Settings
| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| Developer mode | Toggle | Off | Enable debug features |
| Show internal nodes | Toggle | Off | Show system/internal node types |
| Script timeout | Number | 60000 | Code node execution timeout (ms) |
| Max expression depth | Number | 10 | Max nested expression evaluation |
| Enable telemetry | Toggle | Off | Anonymous usage statistics |
| Log file location | Path | ./logs | Log file directory |
| Max log file size | Number | 10 | Max log file size (MB) |
| Log file rotation | Number | 5 | Number of log files to keep |
| Reset all settings | Button | - | Restore all defaults |
| Export settings | Button | - | Export settings to file |
| Import settings | Button | - | Import settings from file |

### Implementation Plan

#### Phase 7.1: Settings Infrastructure
- [x] Create `SettingsEntity` database model for persistent settings
- [x] Create `SettingsService` for CRUD operations
- [x] Create `SettingsDTO` for UI/API communication
- [x] Define `SettingsDefaults` with all default values
- [x] Implement settings validation logic

#### Phase 7.2: Settings View UI
- [ ] Create `SettingsView` component with tabbed layout
- [ ] Create reusable form controls (toggle, dropdown, number input, path picker)
- [ ] Implement settings binding (UI ↔ Service)
- [ ] Style consistent with Nord Dark theme

#### Phase 7.3: Category Implementation
- [ ] General Settings tab
- [ ] Editor Settings tab
- [ ] Execution Settings tab
- [ ] AI Providers tab
- [ ] Credentials tab (migrate existing dialog)
- [ ] HTTP/Network tab
- [ ] Database & Storage tab
- [ ] Webhook & Server tab
- [ ] Notifications tab
- [ ] Advanced tab

#### Phase 7.4: Settings Integration
- [ ] Replace sidebar Credentials button with full Settings
- [ ] Wire settings to relevant services (ExecutionService, LlmChatExecutor, etc.)
- [ ] Add settings import/export functionality
- [ ] Implement settings reset with confirmation
- [ ] Add settings change listeners for real-time updates

#### Phase 7.5: Testing & Polish
- [ ] Add settings search/filter functionality
- [ ] Unit tests for SettingsService
- [ ] UI tests for SettingsView
- [ ] Settings migration for existing installations
- [ ] Documentation for each setting

---

*This document should be updated as features are completed or new requirements emerge.*
