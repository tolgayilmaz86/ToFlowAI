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

1. **Connect UI to Backend Services**
   - Implement save workflow button
   - Implement execute workflow button
   - Add workflow list/open dialog

2. **Node Properties Panel**
   - Edit node name
   - Configure node parameters
   - View node documentation

3. **Execution Feedback**
   - Show running state on nodes
   - Display execution results
   - Handle and show errors

---

*This document should be updated as features are completed or new requirements emerge.*
