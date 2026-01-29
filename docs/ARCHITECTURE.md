# ToFlowAI Architecture Guide

> **A Comprehensive Guide for Junior Developers**  
> **Version:** 1.0  
> **Last Updated:** January 29, 2026

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [What is Workflow Automation?](#2-what-is-workflow-automation)
3. [Project Overview](#3-project-overview)
4. [Technology Stack](#4-technology-stack)
5. [Module Architecture](#5-module-architecture)
6. [Core Concepts](#6-core-concepts)
7. [UML Diagrams](#7-uml-diagrams)
8. [Backend Architecture (Spring Boot)](#8-backend-architecture-spring-boot)
9. [Frontend Architecture (JavaFX)](#9-frontend-architecture-javafx)
10. [Data Flow](#10-data-flow)
11. [Adding New Features](#11-adding-new-features)
12. [Best Practices](#12-best-practices)
13. [Troubleshooting](#13-troubleshooting)
14. [Glossary](#14-glossary)

---

## 1. Introduction

### Welcome, Developer! 👋

ToFlowAI is a **visual workflow automation application** similar to n8n, Zapier, or Make.com. It allows users to create automated workflows by connecting "nodes" (building blocks) together visually.

**This guide will help you:**
- Understand how workflow automation works
- Navigate the codebase confidently
- Add new features and node types
- Follow best practices

**What You'll Learn:**
```
┌─────────────────────────────────────────────────────────────────┐
│  📚 Workflow Automation Concepts                                │
│  ├── What are nodes, connections, and workflows?                │
│  ├── How data flows between nodes                               │
│  └── Trigger types and execution patterns                       │
│                                                                 │
│  🏗️ Architecture Understanding                                  │
│  ├── Multi-module Gradle project structure                      │
│  ├── Spring Boot backend services                               │
│  ├── JavaFX desktop UI                                          │
│  └── Database persistence with H2                               │
│                                                                 │
│  🔧 Practical Skills                                            │
│  ├── Adding new node types (executors)                          │
│  ├── Creating UI components                                     │
│  ├── Working with settings and credentials                      │
│  └── Testing and debugging                                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. What is Workflow Automation?

### 2.1 The Big Picture

Imagine you want to:
1. Monitor a folder for new files
2. When a file arrives, extract text from it
3. Send the text to an AI for analysis
4. Email the results to your team

**Without automation:** You do this manually, every time.  
**With ToFlowAI:** You build it once, it runs forever.

### 2.2 Core Terminology

| Term | Definition | Example |
|------|------------|---------|
| **Workflow** | A complete automation blueprint | "Process customer emails" |
| **Node** | A single action/step in the workflow | "Send HTTP request", "Run code" |
| **Connection** | Link between nodes showing data flow | Output of node A → Input of node B |
| **Trigger** | Special node that starts the workflow | "Every Monday at 9 AM", "On webhook" |
| **Executor** | Backend code that runs a node | `HttpRequestExecutor`, `CodeExecutor` |
| **Execution** | One complete run of a workflow | Started at 10:30, took 5 seconds |

### 2.3 Visual Example

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         SAMPLE WORKFLOW                                 │
│                                                                         │
│   ┌──────────────┐     ┌──────────────┐     ┌──────────────┐           │
│   │   Webhook    │     │    HTTP      │     │     Code     │           │
│   │   Trigger    │────▶│   Request    │────▶│   (Process)  │           │
│   │              │     │              │     │              │           │
│   │ "Start when  │     │ "Call AI API"│     │ "Format      │           │
│   │  data arrives│     │              │     │  response"   │           │
│   └──────────────┘     └──────────────┘     └──────────────┘           │
│          │                                          │                   │
│          │                                          ▼                   │
│          │                                  ┌──────────────┐           │
│          │                                  │     Email    │           │
│          │                                  │     Node     │           │
│          │                                  │              │           │
│          │                                  │ "Send result │           │
│          │                                  │  to team"    │           │
│          │                                  └──────────────┘           │
│          │                                                              │
│   DATA FLOW: webhook_data → api_response → formatted_data → email_sent │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.4 Comparison with Other Tools

| Feature | n8n | Zapier | ToFlowAI |
|---------|-----|--------|----------|
| Visual Editor | ✅ | ✅ | ✅ |
| Self-Hosted | ✅ | ❌ | ✅ |
| Open Source | ✅ | ❌ | ✅ |
| Desktop App | ❌ | ❌ | ✅ |
| AI Nodes | ✅ | ✅ | ✅ |
| Code Nodes | ✅ | Limited | ✅ |

---

## 3. Project Overview

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         ToFlowAI Application                            │
│                                                                         │
│   ┌───────────────────────────────────────────────────────────────┐    │
│   │                    UI MODULE (JavaFX)                         │    │
│   │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐             │    │
│   │  │   Canvas    │ │   Dialogs   │ │   Editors   │             │    │
│   │  │  (Workflow) │ │ (Settings)  │ │ (Properties)│             │    │
│   │  └─────────────┘ └─────────────┘ └─────────────┘             │    │
│   └───────────────────────────────────────────────────────────────┘    │
│                              │                                          │
│                              ▼                                          │
│   ┌───────────────────────────────────────────────────────────────┐    │
│   │                   COMMON MODULE (Shared)                       │    │
│   │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐         │    │
│   │  │   DTOs   │ │  Enums   │ │ Interfaces│ │  Domain  │         │    │
│   │  └──────────┘ └──────────┘ └──────────┘ └──────────┘         │    │
│   └───────────────────────────────────────────────────────────────┘    │
│                              │                                          │
│                              ▼                                          │
│   ┌───────────────────────────────────────────────────────────────┐    │
│   │                    APP MODULE (Spring Boot)                    │    │
│   │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐             │    │
│   │  │  Services   │ │  Executors  │ │  Database   │             │    │
│   │  │ (Business)  │ │   (Nodes)   │ │   (H2)      │             │    │
│   │  └─────────────┘ └─────────────┘ └─────────────┘             │    │
│   └───────────────────────────────────────────────────────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Directory Structure

```
ToFlowAI/
├── 📁 app/                          # Spring Boot backend
│   └── src/main/java/io/toflowai/app/
│       ├── 📁 api/                  # REST controllers (future)
│       ├── 📁 config/               # Spring configuration
│       ├── 📁 database/             # JPA entities & repositories
│       │   ├── model/               # Database entities
│       │   └── repository/          # Spring Data JPA repos
│       ├── 📁 entity/               # Additional entities
│       ├── 📁 executor/             # 🌟 NODE EXECUTORS (key!)
│       │   ├── HttpRequestExecutor.java
│       │   ├── CodeExecutor.java
│       │   ├── LlmChatExecutor.java
│       │   └── ... (22+ executors)
│       └── 📁 service/              # Business logic services
│           ├── ExecutionService.java
│           ├── WorkflowService.java
│           ├── NodeExecutor.java    # Interface
│           └── NodeExecutorRegistry.java
│
├── 📁 common/                       # Shared code (no dependencies)
│   └── src/main/java/io/toflowai/common/
│       ├── 📁 domain/               # Core domain objects
│       │   ├── Node.java            # Node record
│       │   ├── Connection.java      # Connection record
│       │   └── Execution.java       # Execution record
│       ├── 📁 dto/                  # Data Transfer Objects
│       │   ├── WorkflowDTO.java
│       │   ├── ExecutionDTO.java
│       │   └── SettingDTO.java
│       ├── 📁 enums/                # Enumerations
│       │   ├── ExecutionStatus.java
│       │   ├── SettingCategory.java
│       │   └── SettingType.java
│       ├── 📁 expression/           # Expression evaluation
│       └── 📁 service/              # Service interfaces
│           ├── WorkflowServiceInterface.java
│           ├── ExecutionServiceInterface.java
│           └── SettingsServiceInterface.java
│
├── 📁 ui/                           # JavaFX desktop UI
│   └── src/main/java/io/toflowai/ui/
│       ├── 📁 canvas/               # Workflow editor canvas
│       │   ├── WorkflowCanvas.java  # Main canvas component
│       │   ├── NodeView.java        # Visual node representation
│       │   ├── ConnectionLine.java  # Visual connection
│       │   └── NodePropertiesPanel.java
│       ├── 📁 console/              # Execution console
│       ├── 📁 controller/           # FXML controllers
│       ├── 📁 dialog/               # Dialog windows
│       │   ├── SettingsDialog.java
│       │   └── CredentialManagerDialog.java
│       └── 📁 editor/               # Code editors
│
├── 📁 docs/                         # Documentation
│   ├── DEVELOPMENT_PLAN.md
│   └── ARCHITECTURE.md              # This file!
│
├── 📄 build.gradle                  # Root build configuration
├── 📄 settings.gradle               # Multi-module settings
└── 📄 gradlew.bat                   # Gradle wrapper (Windows)
```

### 3.3 Why This Structure?

| Module | Responsibility | Depends On |
|--------|----------------|------------|
| **common** | Shared types (DTOs, enums, interfaces) | Nothing |
| **app** | Business logic, database, execution | common |
| **ui** | User interface, visual editor | common |

**Benefits:**
- ✅ Clear separation of concerns
- ✅ UI can be replaced without touching business logic
- ✅ Common types ensure consistency
- ✅ Each module can be tested independently

---

## 4. Technology Stack

### 4.1 Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         TECHNOLOGY STACK                                │
│                                                                         │
│   ┌───────────────────────────────────────────────────────────────┐    │
│   │                        FRONTEND                                │    │
│   │   JavaFX 21.0.5         │  Desktop GUI framework              │    │
│   │   AtlantaFX (Nord Dark) │  Modern UI theme                    │    │
│   │   Ikonli                │  Icon library (Material Design)     │    │
│   │   FXWeaver              │  Spring + FXML integration          │    │
│   └───────────────────────────────────────────────────────────────┘    │
│                                                                         │
│   ┌───────────────────────────────────────────────────────────────┐    │
│   │                        BACKEND                                 │    │
│   │   Java 25               │  Latest LTS with virtual threads    │    │
│   │   Spring Boot 3.5.0     │  Application framework              │    │
│   │   Spring Data JPA       │  Database abstraction               │    │
│   │   H2 Database           │  Embedded SQL database              │    │
│   │   Flyway                │  Database migrations                │    │
│   │   Jackson               │  JSON serialization                 │    │
│   └───────────────────────────────────────────────────────────────┘    │
│                                                                         │
│   ┌───────────────────────────────────────────────────────────────┐    │
│   │                        BUILD & TOOLS                           │    │
│   │   Gradle 9.2.0          │  Build automation                   │    │
│   │   VS Code               │  Recommended IDE                    │    │
│   │   Git                   │  Version control                    │    │
│   └───────────────────────────────────────────────────────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Why These Technologies?

#### Java 25 with Virtual Threads

```java
// OLD WAY: Platform threads (expensive, limited)
ExecutorService executor = Executors.newFixedThreadPool(10);

// NEW WAY: Virtual threads (cheap, unlimited)
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

**Virtual Threads Benefits:**
- Each node execution can have its own thread
- No thread pool exhaustion
- Blocking I/O doesn't block other work
- Perfect for workflow automation (many concurrent operations)

#### Spring Boot 3.5.0

Spring Boot provides:
- **Dependency Injection** - Components are wired automatically
- **Transaction Management** - Database operations are atomic
- **Configuration** - `application.properties` for settings
- **Testing** - Built-in test support

```java
// Spring creates and wires this automatically
@Service
public class ExecutionService {
    private final WorkflowService workflowService;  // Injected!
    private final NodeExecutorRegistry registry;    // Injected!
    
    public ExecutionService(WorkflowService ws, NodeExecutorRegistry nr) {
        this.workflowService = ws;
        this.registry = nr;
    }
}
```

#### JavaFX with AtlantaFX

```java
// Modern dark theme UI
Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
```

**Why JavaFX?**
- Native desktop performance
- Rich visual components
- Canvas for workflow editor
- Cross-platform (Windows, Mac, Linux)

### 4.3 Running the Application

```bash
# Set Java 25
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.0.36-hotspot"

# Build the project
.\gradlew.bat clean build -x test

# Run the application
.\gradlew.bat :app:bootRun
```

---

## 5. Module Architecture

### 5.1 Module Dependency Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       MODULE DEPENDENCIES                               │
│                                                                         │
│                        ┌───────────────┐                               │
│                        │      UI       │                               │
│                        │   (JavaFX)    │                               │
│                        └───────┬───────┘                               │
│                                │                                        │
│                                │ uses interfaces                        │
│                                ▼                                        │
│                        ┌───────────────┐                               │
│                        │    COMMON     │◀─────────────────────┐        │
│                        │  (Interfaces) │                      │        │
│                        └───────┬───────┘                      │        │
│                                │                               │        │
│                                │ implements                    │ uses   │
│                                ▼                               │        │
│                        ┌───────────────┐                      │        │
│                        │      APP      │──────────────────────┘        │
│                        │(Spring Boot)  │                               │
│                        └───────────────┘                               │
│                                                                         │
│  RULE: Lower modules don't know about higher modules                   │
│  - common knows nothing about app or ui                                │
│  - app knows nothing about ui                                          │
│  - ui uses common interfaces, app provides implementations             │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Interface Pattern

The `common` module defines interfaces. The `app` module implements them. The `ui` module uses them.

```java
// In COMMON module - just the interface
public interface WorkflowServiceInterface {
    List<WorkflowDTO> findAll();
    Optional<WorkflowDTO> findById(Long id);
    WorkflowDTO save(WorkflowDTO workflow);
}

// In APP module - the implementation
@Service
public class WorkflowService implements WorkflowServiceInterface {
    private final WorkflowRepository repository;
    
    @Override
    public List<WorkflowDTO> findAll() {
        return repository.findAll().stream()
            .map(this::toDTO)
            .toList();
    }
    // ... more methods
}

// In UI module - uses the interface
public class WorkflowCanvas {
    private final WorkflowServiceInterface workflowService;  // Interface!
    
    public void saveWorkflow() {
        workflowService.save(this.workflow);
    }
}
```

**Why This Pattern?**
- UI doesn't depend on Spring
- We could swap `WorkflowService` for a mock in tests
- Clear contract between modules

---

## 6. Core Concepts

### 6.1 Node

A **Node** is a single step in a workflow.

```java
// In common/domain/Node.java
public record Node(
    String id,           // Unique ID (UUID)
    String type,         // "httpRequest", "code", "if", etc.
    String name,         // User's display name
    Position position,   // Canvas position (x, y)
    Map<String, Object> parameters,  // Configuration
    Long credentialId,   // Optional credential reference
    boolean disabled,    // Skip this node?
    String notes         // User's notes
) {
    public record Position(double x, double y) {}
}
```

**Example Node (HTTP Request):**
```json
{
    "id": "node_abc123",
    "type": "httpRequest",
    "name": "Call Weather API",
    "position": { "x": 300, "y": 200 },
    "parameters": {
        "url": "https://api.weather.com/v1/forecast",
        "method": "GET",
        "headers": {
            "Accept": "application/json"
        }
    },
    "credentialId": 42,
    "disabled": false
}
```

### 6.2 Connection

A **Connection** links two nodes together.

```java
// In common/domain/Connection.java
public record Connection(
    String id,              // Unique ID
    String sourceNodeId,    // Output from this node
    String targetNodeId,    // Input to this node
    String sourceHandle,    // Which output port ("main", "true", "false")
    String targetHandle     // Which input port ("main")
) {}
```

**Visual Representation:**
```
┌──────────────┐         ┌──────────────┐
│   IF Node    │         │  Email Node  │
│              │         │              │
│        TRUE ●─────────▶●              │
│       FALSE ●          │              │
└──────────────┘         └──────────────┘

Connection: {
    sourceNodeId: "if_node_1",
    targetNodeId: "email_node_1", 
    sourceHandle: "true",
    targetHandle: "main"
}
```

### 6.3 Workflow

A **Workflow** is a collection of nodes and connections.

```java
// In common/dto/WorkflowDTO.java
public record WorkflowDTO(
    Long id,
    String name,
    String description,
    List<Node> nodes,           // All nodes
    List<Connection> connections, // All connections
    Map<String, Object> settings, // Workflow settings
    boolean active,              // Is it runnable?
    Instant createdAt,
    Instant updatedAt
) {
    // Helper method
    public List<Node> getTriggerNodes() {
        return nodes.stream()
            .filter(n -> n.type().endsWith("Trigger"))
            .toList();
    }
}
```

### 6.4 Execution

An **Execution** is one run of a workflow.

```java
// Execution lifecycle
public enum ExecutionStatus {
    PENDING,    // Waiting to start
    RUNNING,    // Currently executing
    SUCCESS,    // Completed successfully
    FAILED,     // Encountered an error
    CANCELLED   // Stopped by user
}
```

### 6.5 NodeExecutor

A **NodeExecutor** is the code that actually runs a node.

```java
// Interface every executor must implement
public interface NodeExecutor {
    
    // Run the node with input data
    Map<String, Object> execute(
        Node node,                        // Node configuration
        Map<String, Object> input,        // Data from previous node
        ExecutionService.ExecutionContext context  // Execution state
    );
    
    // What node type does this handle?
    String getNodeType();
}
```

---

## 7. UML Diagrams

### 7.1 Class Diagram - Core Domain

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           CORE DOMAIN CLASS DIAGRAM                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│    ┌────────────────────┐         ┌─────────────────────┐                      │
│    │    WorkflowDTO     │         │    ExecutionDTO     │                      │
│    ├────────────────────┤         ├─────────────────────┤                      │
│    │ - id: Long         │         │ - id: Long          │                      │
│    │ - name: String     │ 1    * │ - workflowId: Long  │                      │
│    │ - description      │─────────│ - status: Status    │                      │
│    │ - nodes: List<Node>│         │ - startedAt: Instant│                      │
│    │ - connections: List│         │ - finishedAt: Inst. │                      │
│    │ - active: boolean  │         │ - errorMessage: Str │                      │
│    ├────────────────────┤         └─────────────────────┘                      │
│    │ +getTriggerNodes() │                                                       │
│    │ +getNodeById()     │                                                       │
│    └────────────────────┘                                                       │
│            │ contains                                                            │
│            │                                                                     │
│    ┌───────┴────────┐                                                           │
│    │                │                                                            │
│    ▼                ▼                                                            │
│  ┌──────────────┐  ┌──────────────────┐                                        │
│  │     Node     │  │   Connection     │                                        │
│  ├──────────────┤  ├──────────────────┤                                        │
│  │ - id: String │  │ - id: String     │                                        │
│  │ - type: Str  │  │ - sourceNodeId   │                                        │
│  │ - name: Str  │  │ - targetNodeId   │                                        │
│  │ - position   │  │ - sourceHandle   │                                        │
│  │ - parameters │  │ - targetHandle   │                                        │
│  │ - disabled   │  └──────────────────┘                                        │
│  ├──────────────┤                                                               │
│  │ +withPos()   │                                                               │
│  │ +withParams()│                                                               │
│  └──────────────┘                                                               │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 7.2 Class Diagram - Node Executor Pattern

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        NODE EXECUTOR CLASS DIAGRAM                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│                       ┌────────────────────────┐                               │
│                       │    <<interface>>       │                               │
│                       │     NodeExecutor       │                               │
│                       ├────────────────────────┤                               │
│                       │ +execute(node, input,  │                               │
│                       │   context): Map        │                               │
│                       │ +getNodeType(): String │                               │
│                       └───────────┬────────────┘                               │
│                                   │                                             │
│            ┌──────────────┬───────┴──────┬─────────────────┐                   │
│            │              │              │                 │                    │
│            ▼              ▼              ▼                 ▼                    │
│   ┌────────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│   │HttpRequest     │ │ Code        │ │ If          │ │ LlmChat     │          │
│   │Executor        │ │ Executor    │ │ Executor    │ │ Executor    │          │
│   ├────────────────┤ ├─────────────┤ ├─────────────┤ ├─────────────┤          │
│   │ - httpClient   │ │ - engine    │ │             │ │ - apiClient │          │
│   ├────────────────┤ ├─────────────┤ ├─────────────┤ ├─────────────┤          │
│   │ +execute()     │ │ +execute()  │ │ +execute()  │ │ +execute()  │          │
│   │ +getNodeType() │ │ +getNode..()│ │ +getNode..()│ │ +getNode..()│          │
│   │  ="httpRequest"│ │  ="code"    │ │  ="if"      │ │  ="llmChat" │          │
│   └────────────────┘ └─────────────┘ └─────────────┘ └─────────────┘          │
│                                                                                 │
│            ...and 18+ more executor implementations...                          │
│                                                                                 │
│                                                                                 │
│   ┌──────────────────────────────────────────────────────────────────┐        │
│   │                    NodeExecutorRegistry                           │        │
│   ├──────────────────────────────────────────────────────────────────┤        │
│   │ - executors: Map<String, NodeExecutor>                           │        │
│   ├──────────────────────────────────────────────────────────────────┤        │
│   │ +register(executor): void      # Add executor to registry        │        │
│   │ +getExecutor(type): NodeExecutor  # Find executor by node type   │        │
│   │ +getSupportedTypes(): Set<String>  # List all node types         │        │
│   └──────────────────────────────────────────────────────────────────┘        │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 7.3 Class Diagram - Service Layer

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         SERVICE LAYER CLASS DIAGRAM                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│   ┌─────────────────────────────────┐    ┌────────────────────────────────┐   │
│   │    <<interface>>                │    │    <<interface>>               │   │
│   │  WorkflowServiceInterface       │    │  ExecutionServiceInterface     │   │
│   ├─────────────────────────────────┤    ├────────────────────────────────┤   │
│   │ +findAll(): List<WorkflowDTO>   │    │ +execute(id, input): Exec.DTO  │   │
│   │ +findById(id): Optional<Wf>     │    │ +executeAsync(id): Future      │   │
│   │ +save(workflow): WorkflowDTO    │    │ +cancel(id): void              │   │
│   │ +delete(id): void               │    │ +findByWorkflowId(): List      │   │
│   └─────────────────┬───────────────┘    └────────────────┬───────────────┘   │
│                     │                                      │                    │
│                     │ implements                           │ implements         │
│                     ▼                                      ▼                    │
│   ┌─────────────────────────────────┐    ┌────────────────────────────────┐   │
│   │       WorkflowService           │    │       ExecutionService         │   │
│   │        <<@Service>>             │    │        <<@Service>>            │   │
│   ├─────────────────────────────────┤    ├────────────────────────────────┤   │
│   │ - repository: WorkflowRepo      │    │ - workflowService              │   │
│   │ - objectMapper: Jackson         │    │ - credentialService            │   │
│   ├─────────────────────────────────┤    │ - nodeExecutorRegistry         │   │
│   │ +findAll()                      │    │ - executionLogger              │   │
│   │ +save()                         │    ├────────────────────────────────┤   │
│   │ -toDTO()                        │    │ +execute()                     │   │
│   │ -toEntity()                     │    │ -executeWorkflow()             │   │
│   └─────────────────────────────────┘    │ -executeNode()                 │   │
│                                          └────────────────────────────────┘   │
│                                                                                 │
│   ┌─────────────────────────────────┐    ┌────────────────────────────────┐   │
│   │      CredentialService          │    │       SettingsService          │   │
│   │        <<@Service>>             │    │        <<@Service>>            │   │
│   ├─────────────────────────────────┤    ├────────────────────────────────┤   │
│   │ - encryptionService             │    │ - cache: ConcurrentHashMap     │   │
│   │ - repository                    │    │ - repository                   │   │
│   ├─────────────────────────────────┤    ├────────────────────────────────┤   │
│   │ +store(name, data): Credential  │    │ +getValue(key): String         │   │
│   │ +retrieve(id): String           │    │ +setValue(key, value)          │   │
│   │ +delete(id): void               │    │ +resetToDefault(key)           │   │
│   └─────────────────────────────────┘    └────────────────────────────────┘   │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 7.4 Sequence Diagram - Workflow Execution

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    SEQUENCE DIAGRAM: WORKFLOW EXECUTION                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│    User          UI            ExecutionService   NodeRegistry   Executor       │
│     │            │                    │               │             │           │
│     │  click     │                    │               │             │           │
│     │   Run      │                    │               │             │           │
│     ├───────────▶│                    │               │             │           │
│     │            │                    │               │             │           │
│     │            │  execute(          │               │             │           │
│     │            │    workflowId,     │               │             │           │
│     │            │    inputData)      │               │             │           │
│     │            ├───────────────────▶│               │             │           │
│     │            │                    │               │             │           │
│     │            │                    │  Load workflow from DB      │           │
│     │            │                    ├──────────────────┐          │           │
│     │            │                    │                  │          │           │
│     │            │                    │◀─────────────────┘          │           │
│     │            │                    │               │             │           │
│     │            │                    │  Create ExecutionContext    │           │
│     │            │                    ├──────────────────┐          │           │
│     │            │                    │◀─────────────────┘          │           │
│     │            │                    │               │             │           │
│     │            │                    │  Find trigger │             │           │
│     │            │                    │  nodes        │             │           │
│     │            │                    ├──────────────────┐          │           │
│     │            │                    │◀─────────────────┘          │           │
│     │            │                    │               │             │           │
│     │            │                    │  FOR EACH NODE:             │           │
│     │            │                    │  ┌───────────────────────────────────┐  │
│     │            │                    │  │                          │        │  │
│     │            │                    │  │ getExecutor(node.type)   │        │  │
│     │            │                    │  ├─────────────────────────▶│        │  │
│     │            │                    │  │                          │        │  │
│     │            │                    │  │     executor             │        │  │
│     │            │                    │  │◀─────────────────────────┤        │  │
│     │            │                    │  │                          │        │  │
│     │            │                    │  │  execute(node, input, ctx)        │  │
│     │            │                    │  ├───────────────────────────────────▶  │
│     │            │                    │  │                          │        │  │
│     │            │                    │  │     output               │        │  │
│     │            │                    │  │◀───────────────────────────────────  │
│     │            │                    │  │                          │        │  │
│     │            │                    │  │ Find next nodes via connections   │  │
│     │            │                    │  │ Pass output as input     │        │  │
│     │            │                    │  └───────────────────────────────────┘  │
│     │            │                    │               │             │           │
│     │            │                    │  Save execution to DB       │           │
│     │            │                    ├──────────────────┐          │           │
│     │            │                    │◀─────────────────┘          │           │
│     │            │                    │               │             │           │
│     │            │   ExecutionDTO     │               │             │           │
│     │            │◀───────────────────┤               │             │           │
│     │            │                    │               │             │           │
│     │  show      │                    │               │             │           │
│     │  result    │                    │               │             │           │
│     │◀───────────┤                    │               │             │           │
│     │            │                    │               │             │           │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 7.5 Sequence Diagram - Node Execution Detail

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    SEQUENCE DIAGRAM: NODE EXECUTION                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ExecutionService     Logger      Executor       HttpClient   Credential        │
│        │                │            │               │           │              │
│        │  Log node      │            │               │           │              │
│        │  start         │            │               │           │              │
│        ├───────────────▶│            │               │           │              │
│        │                │            │               │           │              │
│        │  Get parameters from node   │               │           │              │
│        ├────────────────────────────┐│               │           │              │
│        │◀───────────────────────────┘│               │           │              │
│        │                │            │               │           │              │
│        │  Check if credential needed │               │           │              │
│        ├────────────────────────────────────────────────────────▶│              │
│        │                │            │               │           │              │
│        │  Decrypted API key          │               │           │              │
│        │◀────────────────────────────────────────────────────────┤              │
│        │                │            │               │           │              │
│        │  execute(node, │            │               │           │              │
│        │    input,      │            │               │           │              │
│        │    context)    │            │               │           │              │
│        ├────────────────────────────▶│               │           │              │
│        │                │            │               │           │              │
│        │                │            │  Interpolate  │           │              │
│        │                │            │  {{ vars }}   │           │              │
│        │                │            ├─────┐         │           │              │
│        │                │            │◀────┘         │           │              │
│        │                │            │               │           │              │
│        │                │            │  HTTP Request │           │              │
│        │                │            ├──────────────▶│           │              │
│        │                │            │               │           │              │
│        │                │            │  Response     │           │              │
│        │                │            │◀──────────────┤           │              │
│        │                │            │               │           │              │
│        │                │            │  Build output │           │              │
│        │                │            │  map          │           │              │
│        │                │            ├─────┐         │           │              │
│        │                │            │◀────┘         │           │              │
│        │                │            │               │           │              │
│        │  output map    │            │               │           │              │
│        │◀────────────────────────────┤               │           │              │
│        │                │            │               │           │              │
│        │  Log node      │            │               │           │              │
│        │  complete      │            │               │           │              │
│        ├───────────────▶│            │               │           │              │
│        │                │            │               │           │              │
│        │  Pass output   │            │               │           │              │
│        │  to next node  │            │               │           │              │
│        ├─────────────────────────────────────────────────────────────────┐     │
│        │                │            │               │           │       │     │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 7.6 Use Case Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           USE CASE DIAGRAM                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│                    ┌─────────────────────────────────────────────┐             │
│                    │              ToFlowAI System                 │             │
│                    │                                              │             │
│                    │   ┌─────────────────────────────────────┐   │             │
│                    │   │      WORKFLOW MANAGEMENT            │   │             │
│     ┌──────┐       │   │                                     │   │             │
│     │      │       │   │  ○ Create New Workflow              │   │             │
│     │ User │───────┼──▶│  ○ Open Existing Workflow           │   │             │
│     │      │       │   │  ○ Save Workflow                    │   │             │
│     └──────┘       │   │  ○ Delete Workflow                  │   │             │
│        │           │   │  ○ Import/Export Workflow           │   │             │
│        │           │   │                                     │   │             │
│        │           │   └─────────────────────────────────────┘   │             │
│        │           │                                              │             │
│        │           │   ┌─────────────────────────────────────┐   │             │
│        │           │   │      WORKFLOW EDITING               │   │             │
│        │           │   │                                     │   │             │
│        └───────────┼──▶│  ○ Add Node to Canvas               │   │             │
│                    │   │  ○ Remove Node                      │   │             │
│                    │   │  ○ Connect Nodes                    │   │             │
│                    │   │  ○ Configure Node Properties        │   │             │
│                    │   │  ○ Pan/Zoom Canvas                  │   │             │
│                    │   │                                     │   │             │
│                    │   └─────────────────────────────────────┘   │             │
│                    │                                              │             │
│                    │   ┌─────────────────────────────────────┐   │             │
│                    │   │      WORKFLOW EXECUTION             │   │             │
│                    │   │                                     │   │             │
│     ┌──────┐       │   │  ○ Run Workflow Manually            │   │             │
│     │      │───────┼──▶│  ○ Stop Running Workflow            │   │             │
│     │ User │       │   │  ○ View Execution Console           │   │             │
│     │      │       │   │  ○ View Execution History           │   │             │
│     └──────┘       │   │                                     │   │             │
│                    │   └─────────────────────────────────────┘   │             │
│                    │                                              │             │
│                    │   ┌─────────────────────────────────────┐   │             │
│     ┌──────┐       │   │      SETTINGS & CREDENTIALS         │   │             │
│     │      │       │   │                                     │   │             │
│     │ User │───────┼──▶│  ○ Manage API Credentials           │   │             │
│     │      │       │   │  ○ Configure App Settings           │   │             │
│     └──────┘       │   │  ○ Configure AI Providers           │   │             │
│                    │   │                                     │   │             │
│                    │   └─────────────────────────────────────┘   │             │
│                    │                                              │             │
│                    │   ┌─────────────────────────────────────┐   │             │
│     ┌──────────┐   │   │      AUTOMATED TRIGGERS             │   │             │
│     │          │   │   │                                     │   │             │
│     │ Schedule │───┼──▶│  ○ Execute on Schedule (Cron)       │   │             │
│     │ Service  │   │   │                                     │   │             │
│     └──────────┘   │   └─────────────────────────────────────┘   │             │
│                    │                                              │             │
│     ┌──────────┐   │   ┌─────────────────────────────────────┐   │             │
│     │          │   │   │      WEBHOOK HANDLING               │   │             │
│     │ External │───┼──▶│                                     │   │             │
│     │ Service  │   │   │  ○ Receive Webhook                  │   │             │
│     └──────────┘   │   │  ○ Trigger Workflow                 │   │             │
│                    │   │                                     │   │             │
│                    │   └─────────────────────────────────────┘   │             │
│                    │                                              │             │
│                    └──────────────────────────────────────────────┘             │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 7.7 Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          COMPONENT DIAGRAM                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│    ┌────────────────────────────────────────────────────────────────────────┐  │
│    │                         UI COMPONENTS                                   │  │
│    │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │  │
│    │  │ Workflow     │  │ Settings     │  │ Execution    │                 │  │
│    │  │ Canvas       │  │ Dialog       │  │ Console      │                 │  │
│    │  │              │  │              │  │              │                 │  │
│    │  │ - NodeView   │  │ - Category   │  │ - Log        │                 │  │
│    │  │ - Connection │  │   List       │  │   Entries    │                 │  │
│    │  │ - Palette    │  │ - Controls   │  │ - Filters    │                 │  │
│    │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘                 │  │
│    │         │                 │                 │                          │  │
│    └─────────┼─────────────────┼─────────────────┼──────────────────────────┘  │
│              │                 │                 │                              │
│              │      Service Interfaces           │                              │
│              ▼                 ▼                 ▼                              │
│    ┌─────────────────────────────────────────────────────────────────────────┐ │
│    │                     SERVICE INTERFACES (common)                         │ │
│    │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐      │ │
│    │  │WorkflowService   │  │SettingsService   │  │ExecutionService  │      │ │
│    │  │Interface         │  │Interface         │  │Interface         │      │ │
│    │  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘      │ │
│    └───────────┼────────────────────┼────────────────────┼───────────────────┘ │
│                │                    │                    │                      │
│                │  implements        │  implements        │  implements          │
│                ▼                    ▼                    ▼                      │
│    ┌─────────────────────────────────────────────────────────────────────────┐ │
│    │                      SERVICE LAYER (app)                                │ │
│    │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐      │ │
│    │  │ WorkflowService  │  │ SettingsService  │  │ ExecutionService │      │ │
│    │  │ <<@Service>>     │  │ <<@Service>>     │  │ <<@Service>>     │      │ │
│    │  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘      │ │
│    │           │                     │                     │                 │ │
│    │           ▼                     ▼                     ▼                 │ │
│    │  ┌──────────────────────────────────────────────────────────────┐      │ │
│    │  │                   NODE EXECUTOR REGISTRY                     │      │ │
│    │  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐│      │ │
│    │  │  │HTTP Req │ │  Code   │ │   If    │ │LLM Chat │ │  Loop   ││      │ │
│    │  │  │Executor │ │Executor │ │Executor │ │Executor │ │Executor ││      │ │
│    │  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘│      │ │
│    │  └──────────────────────────────────────────────────────────────┘      │ │
│    └─────────────────────────────────────────────────────────────────────────┘ │
│                                          │                                      │
│                                          ▼                                      │
│    ┌─────────────────────────────────────────────────────────────────────────┐ │
│    │                      DATA LAYER (app)                                   │ │
│    │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐      │ │
│    │  │WorkflowRepository│  │SettingRepository │  │ExecutionRepository│     │ │
│    │  │  <<JpaRepo>>     │  │  <<JpaRepo>>     │  │  <<JpaRepo>>     │      │ │
│    │  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘      │ │
│    │           │                     │                     │                 │ │
│    │           └─────────────────────┼─────────────────────┘                 │ │
│    │                                 │                                       │ │
│    │                                 ▼                                       │ │
│    │                        ┌──────────────────┐                            │ │
│    │                        │   H2 Database    │                            │ │
│    │                        │   (Embedded)     │                            │ │
│    │                        └──────────────────┘                            │ │
│    └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Backend Architecture (Spring Boot)

### 8.1 Spring Boot Basics for Beginners

#### What is Spring Boot?

Spring Boot is a framework that makes it easy to create Java applications. It handles:
- **Dependency Injection** - Objects are created and wired together automatically
- **Configuration** - Settings via `application.properties`
- **Database Access** - JPA/Hibernate integration
- **Web Server** - Embedded Tomcat

#### Key Annotations

```java
// This class is a Spring-managed service
@Service
public class WorkflowService { ... }

// This class is a Spring-managed REST controller
@Controller
public class MainController { ... }

// This class handles database access
@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowEntity, Long> { ... }

// This method runs after the bean is created
@PostConstruct
public void initialize() { ... }

// This method runs in a database transaction
@Transactional
public void saveWorkflow(WorkflowDTO workflow) { ... }
```

#### Dependency Injection Explained

```java
// ❌ BAD: Creating dependencies manually
public class ExecutionService {
    private WorkflowService workflowService = new WorkflowService(); // Tightly coupled!
}

// ✅ GOOD: Let Spring inject dependencies
@Service
public class ExecutionService {
    private final WorkflowService workflowService;
    
    // Spring automatically provides WorkflowService here
    public ExecutionService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }
}
```

### 8.2 Service Layer Deep Dive

#### ExecutionService - The Heart of Workflow Execution

```java
@Service
@Transactional
public class ExecutionService implements ExecutionServiceInterface {
    
    // Dependencies injected by Spring
    private final WorkflowService workflowService;
    private final CredentialService credentialService;
    private final NodeExecutorRegistry nodeExecutorRegistry;
    
    /**
     * Main execution method
     */
    public ExecutionDTO execute(Long workflowId, Map<String, Object> input) {
        // 1. Load the workflow from database
        WorkflowDTO workflow = workflowService.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));
        
        // 2. Create execution record
        ExecutionEntity execution = new ExecutionEntity(workflowId, TriggerType.MANUAL);
        execution.setStatus(ExecutionStatus.RUNNING);
        execution = executionRepository.save(execution);
        
        // 3. Build execution context (holds state during execution)
        ExecutionContext context = new ExecutionContext(
            execution.getId(),
            workflow,
            input,
            credentialService
        );
        
        // 4. Find trigger nodes and start execution
        try {
            List<Node> triggerNodes = workflow.getTriggerNodes();
            for (Node trigger : triggerNodes) {
                executeNode(trigger, workflow, context, input);
            }
            execution.setStatus(ExecutionStatus.SUCCESS);
        } catch (Exception e) {
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
        }
        
        // 5. Save final state
        return toDTO(executionRepository.save(execution));
    }
}
```

#### NodeExecutorRegistry - Finding the Right Executor

```java
@Component
public class NodeExecutorRegistry {
    
    private final Map<String, NodeExecutor> executors = new HashMap<>();
    
    // Spring automatically finds all NodeExecutor implementations
    // and passes them to this constructor!
    public NodeExecutorRegistry(List<NodeExecutor> allExecutors) {
        for (NodeExecutor executor : allExecutors) {
            // Key: "httpRequest", "code", "if", etc.
            // Value: HttpRequestExecutor, CodeExecutor, IfExecutor, etc.
            executors.put(executor.getNodeType(), executor);
        }
    }
    
    public NodeExecutor getExecutor(String nodeType) {
        NodeExecutor executor = executors.get(nodeType);
        if (executor == null) {
            throw new IllegalArgumentException("Unknown node type: " + nodeType);
        }
        return executor;
    }
}
```

### 8.3 Database Layer

#### Entity Example

```java
@Entity
@Table(name = "workflows")
public class WorkflowEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Lob  // Large Object - stored as CLOB
    @Column(name = "nodes_json")
    private String nodesJson;  // JSON string of nodes
    
    @Lob
    @Column(name = "connections_json")
    private String connectionsJson;  // JSON string of connections
    
    private boolean active = true;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    // Getters and setters...
}
```

#### Repository Example

```java
public interface WorkflowRepository extends JpaRepository<WorkflowEntity, Long> {
    
    // Spring Data JPA generates SQL from method name!
    List<WorkflowEntity> findByActiveTrue();
    
    // Custom query
    @Query("SELECT w FROM WorkflowEntity w WHERE w.name LIKE %:query%")
    List<WorkflowEntity> search(@Param("query") String query);
    
    // Count active workflows
    long countByActiveTrue();
}
```

#### Flyway Migrations

Database schema is managed by Flyway. Each migration is a SQL file:

```sql
-- V001__Initial_Schema.sql
CREATE TABLE workflows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    nodes_json CLOB,
    connections_json CLOB,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP
);

-- V002__Variables_Table.sql
CREATE TABLE variables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    value TEXT,
    scope VARCHAR(50)
);
```

---

## 9. Frontend Architecture (JavaFX)

### 9.1 JavaFX Basics for Beginners

#### What is JavaFX?

JavaFX is a GUI toolkit for building desktop applications in Java. Key concepts:

| Concept | Description | Example |
|---------|-------------|---------|
| **Stage** | The window | Main application window |
| **Scene** | Content container | What's displayed in the window |
| **Node** | UI element | Button, Label, Pane |
| **Pane** | Layout container | VBox, HBox, BorderPane |
| **FXML** | XML-based UI definition | Main.fxml |
| **Controller** | Java class handling events | MainController.java |

#### Layout Containers

```
┌─────────────────────────────────────────────────────────────────┐
│                          BorderPane                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                         TOP                              │   │
│  │                    (Menu, Toolbar)                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│  ┌──────────┐  ┌───────────────────────────┐  ┌───────────┐   │
│  │   LEFT   │  │          CENTER           │  │   RIGHT   │   │
│  │ (Sidebar)│  │      (Main Content)       │  │(Properties)   │
│  │          │  │                           │  │           │   │
│  └──────────┘  └───────────────────────────┘  └───────────┘   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                        BOTTOM                            │   │
│  │                      (Status Bar)                        │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘

VBox (Vertical):     HBox (Horizontal):     StackPane:
┌───────────┐        ┌───┬───┬───┬───┐      ┌───────────┐
│  Item 1   │        │ 1 │ 2 │ 3 │ 4 │      │ Stacked   │
├───────────┤        └───┴───┴───┴───┘      │ on top    │
│  Item 2   │                               │ of each   │
├───────────┤                               │ other     │
│  Item 3   │                               └───────────┘
└───────────┘
```

### 9.2 FXML and Controllers

#### Main.fxml (Simplified)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<BorderPane fx:controller="io.toflowai.ui.controller.MainController">
    
    <!-- Top: Menu and Toolbar -->
    <top>
        <VBox>
            <MenuBar>
                <Menu text="File">
                    <MenuItem text="New" onAction="#onNewWorkflow"/>
                    <MenuItem text="Open" onAction="#onOpenWorkflow"/>
                    <MenuItem text="Save" onAction="#onSaveWorkflow"/>
                </Menu>
            </MenuBar>
            <ToolBar>
                <Button text="▶ Run" onAction="#onRunWorkflow"/>
            </ToolBar>
        </VBox>
    </top>
    
    <!-- Left: Sidebar -->
    <left>
        <VBox fx:id="sidebarNav">
            <Button fx:id="btnWorkflows"/>
            <Button fx:id="btnSettings"/>
        </VBox>
    </left>
    
    <!-- Center: Main content area -->
    <center>
        <StackPane fx:id="contentArea"/>
    </center>
    
</BorderPane>
```

#### MainController.java (Simplified)

```java
@Component
@FxmlView("Main.fxml")  // Links to the FXML file
public class MainController implements Initializable {
    
    // Services injected by Spring
    private final WorkflowServiceInterface workflowService;
    private final ExecutionServiceInterface executionService;
    
    // FXML-injected UI elements
    @FXML private StackPane contentArea;
    @FXML private Button btnWorkflows;
    @FXML private Button btnSettings;
    
    // Constructor injection
    public MainController(
            WorkflowServiceInterface workflowService,
            ExecutionServiceInterface executionService) {
        this.workflowService = workflowService;
        this.executionService = executionService;
    }
    
    // Called after FXML is loaded
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSidebarActions();
        showWorkflowsView();
    }
    
    // Event handlers (referenced in FXML via onAction="#methodName")
    @FXML
    private void onNewWorkflow() {
        workflowCanvas.newWorkflow();
    }
    
    @FXML
    private void onRunWorkflow() {
        workflowCanvas.runWorkflow();
    }
}
```

### 9.3 WorkflowCanvas - The Visual Editor

The `WorkflowCanvas` is the heart of the UI - it's where users build workflows.

```java
public class WorkflowCanvas extends BorderPane {
    
    // Visual layers (from back to front)
    private final Pane gridLayer;        // Grid background
    private final Pane connectionLayer;  // Connection lines
    private final Pane nodeLayer;        // Node rectangles
    
    // State
    private final Map<String, NodeView> nodeViews = new HashMap<>();
    private final Map<String, ConnectionLine> connectionLines = new HashMap<>();
    private WorkflowDTO workflow;
    
    // Current interaction state
    private NodeView selectedNode = null;
    private boolean isConnectionDragging = false;
    
    /**
     * Add a new node to the canvas
     */
    public void addNode(String nodeType, double x, double y) {
        // 1. Create domain node
        Node node = new Node(
            UUID.randomUUID().toString(),
            nodeType,
            nodeType,
            new Node.Position(x, y),
            Map.of(),
            null,
            false,
            null
        );
        
        // 2. Create visual representation
        NodeView nodeView = new NodeView(node, this);
        
        // 3. Add to canvas
        nodeLayer.getChildren().add(nodeView);
        nodeViews.put(node.id(), nodeView);
        
        // 4. Update workflow DTO
        workflow.nodes().add(node);
    }
}
```

### 9.4 NodeView - Visual Node Representation

```java
public class NodeView extends VBox {
    
    private final Node node;
    private ExecutionState executionState = ExecutionState.IDLE;
    
    public NodeView(Node node, WorkflowCanvas canvas) {
        this.node = node;
        
        // Set up visual appearance
        getStyleClass().add("node-view");
        setPrefWidth(120);
        
        // Icon
        FontIcon icon = getIconForNodeType(node.type());
        
        // Label
        Label nameLabel = new Label(node.name());
        
        // Assemble
        getChildren().addAll(icon, nameLabel);
        
        // Position on canvas
        setLayoutX(node.position().x());
        setLayoutY(node.position().y());
        
        // Set up drag behavior
        setupDragBehavior();
    }
    
    private void setupDragBehavior() {
        setOnMousePressed(event -> {
            // Record starting position
            dragStartX = event.getSceneX() - getLayoutX();
            dragStartY = event.getSceneY() - getLayoutY();
        });
        
        setOnMouseDragged(event -> {
            // Move node
            setLayoutX(event.getSceneX() - dragStartX);
            setLayoutY(event.getSceneY() - dragStartY);
            // Redraw connections
            canvas.updateConnections(node.id());
        });
    }
}
```

---

## 10. Data Flow

### 10.1 Complete Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      COMPLETE DATA FLOW                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  1️⃣ USER INTERACTION                                                            │
│     ┌─────────────┐                                                             │
│     │   User      │  Clicks "Run Workflow"                                     │
│     │   Action    │                                                             │
│     └──────┬──────┘                                                             │
│            │                                                                     │
│            ▼                                                                     │
│  2️⃣ UI LAYER                                                                    │
│     ┌─────────────────────────────────────────────────────┐                    │
│     │  WorkflowCanvas.runWorkflow()                       │                    │
│     │                                                     │                    │
│     │  - Serialize current workflow state                 │                    │
│     │  - Call executionService.execute(workflowId, {})    │                    │
│     │  - Update node visual states                        │                    │
│     └──────┬──────────────────────────────────────────────┘                    │
│            │                                                                     │
│            ▼                                                                     │
│  3️⃣ SERVICE LAYER                                                               │
│     ┌─────────────────────────────────────────────────────┐                    │
│     │  ExecutionService.execute(workflowId, input)        │                    │
│     │                                                     │                    │
│     │  - Load workflow from DB                            │                    │
│     │  - Create ExecutionEntity (status: RUNNING)         │                    │
│     │  - Build ExecutionContext                           │                    │
│     │  - Find trigger nodes                               │                    │
│     └──────┬──────────────────────────────────────────────┘                    │
│            │                                                                     │
│            ▼                                                                     │
│  4️⃣ NODE EXECUTION LOOP                                                         │
│     ┌─────────────────────────────────────────────────────┐                    │
│     │  FOR EACH NODE (BFS traversal):                     │                    │
│     │  ┌─────────────────────────────────────────────┐   │                    │
│     │  │ 1. Get executor: registry.getExecutor(type) │   │                    │
│     │  │ 2. Execute: executor.execute(node, input)   │   │                    │
│     │  │ 3. Log result: logger.logNodeComplete()     │   │                    │
│     │  │ 4. Find next: getConnectedNodes(nodeId)     │   │                    │
│     │  │ 5. Pass output as input to next nodes       │   │                    │
│     │  └─────────────────────────────────────────────┘   │                    │
│     └──────┬──────────────────────────────────────────────┘                    │
│            │                                                                     │
│            ▼                                                                     │
│  5️⃣ INDIVIDUAL EXECUTOR                                                         │
│     ┌─────────────────────────────────────────────────────┐                    │
│     │  HttpRequestExecutor.execute(node, input, context)  │                    │
│     │                                                     │                    │
│     │  - Extract parameters (url, method, headers)        │                    │
│     │  - Interpolate {{ variables }}                      │                    │
│     │  - Get credential if needed                         │                    │
│     │  - Make HTTP request                                │                    │
│     │  - Build output map { statusCode, body, json }      │                    │
│     └──────┬──────────────────────────────────────────────┘                    │
│            │                                                                     │
│            ▼                                                                     │
│  6️⃣ DATA PERSISTENCE                                                            │
│     ┌─────────────────────────────────────────────────────┐                    │
│     │  ExecutionRepository.save(execution)                │                    │
│     │                                                     │                    │
│     │  - Save execution status (SUCCESS/FAILED)           │                    │
│     │  - Save output data as JSON                         │                    │
│     │  - Save execution log                               │                    │
│     └──────┬──────────────────────────────────────────────┘                    │
│            │                                                                     │
│            ▼                                                                     │
│  7️⃣ UI UPDATE                                                                   │
│     ┌─────────────────────────────────────────────────────┐                    │
│     │  ExecutionConsole & NodeView updates                │                    │
│     │                                                     │                    │
│     │  - Show logs in execution console                   │                    │
│     │  - Update node colors (green=success, red=error)    │                    │
│     │  - Display execution result                         │                    │
│     └─────────────────────────────────────────────────────┘                    │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 10.2 Data Flow Between Nodes

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    DATA FLOW BETWEEN NODES                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │                           TRIGGER NODE                                    │  │
│  │  ManualTrigger                                                           │  │
│  │                                                                          │  │
│  │  Input: {}  (empty for manual trigger)                                   │  │
│  │  Output: { "timestamp": "2026-01-29T10:30:00Z", "trigger": "manual" }   │  │
│  └────────────────────────────────┬─────────────────────────────────────────┘  │
│                                   │                                             │
│                                   │  output becomes input                       │
│                                   ▼                                             │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │                          HTTP REQUEST NODE                                │  │
│  │  Call Weather API                                                        │  │
│  │                                                                          │  │
│  │  Parameters:                                                              │  │
│  │    url: "https://api.weather.com/v1/forecast?ts={{timestamp}}"          │  │
│  │                                                                          │  │
│  │  Input: { "timestamp": "2026-01-29T10:30:00Z", "trigger": "manual" }    │  │
│  │  Output: {                                                               │  │
│  │    "statusCode": 200,                                                    │  │
│  │    "body": "{\"temp\": 22, \"condition\": \"sunny\"}",                  │  │
│  │    "json": { "temp": 22, "condition": "sunny" }                         │  │
│  │  }                                                                       │  │
│  └────────────────────────────────┬─────────────────────────────────────────┘  │
│                                   │                                             │
│                                   │  output becomes input                       │
│                                   ▼                                             │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │                             IF NODE                                       │  │
│  │  Check if hot                                                            │  │
│  │                                                                          │  │
│  │  Parameters:                                                              │  │
│  │    condition: "{{ json.temp }} > 25"                                    │  │
│  │                                                                          │  │
│  │  Input: { "statusCode": 200, "json": { "temp": 22 } }                   │  │
│  │  Evaluation: 22 > 25 = FALSE                                             │  │
│  │                                                                          │  │
│  │  Output (on FALSE branch): { "condition": false, "json": {...} }        │  │
│  └─────────────────────────────────┬────────────────────────────────────────┘  │
│                          TRUE │    │ FALSE                                      │
│                               ▼    ▼                                            │
│                         ┌─────────────────┐                                    │
│                         │  Next nodes...  │                                    │
│                         └─────────────────┘                                    │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 10.3 Expression Interpolation

The `{{ }}` syntax is used to reference data from previous nodes:

```java
// In HttpRequestExecutor
private String interpolate(String template, Map<String, Object> data) {
    // Template: "Hello, {{ name }}! Today is {{ weather.condition }}."
    // Data: { "name": "Alice", "weather": { "condition": "sunny" } }
    // Result: "Hello, Alice! Today is sunny."
    
    Pattern pattern = Pattern.compile("\\{\\{\\s*([^}]+)\\s*\\}\\}");
    Matcher matcher = pattern.matcher(template);
    
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
        String path = matcher.group(1).trim();  // "name" or "weather.condition"
        Object value = resolvePath(data, path);  // Navigate nested objects
        matcher.appendReplacement(result, value.toString());
    }
    matcher.appendTail(result);
    
    return result.toString();
}
```

---

## 11. Adding New Features

### 11.1 Adding a New Node Type (Step-by-Step)

Let's add a **Slack Message** node as an example.

#### Step 1: Create the Executor

```java
// File: app/src/main/java/io/toflowai/app/executor/SlackMessageExecutor.java

package io.toflowai.app.executor;

import io.toflowai.app.service.ExecutionService;
import io.toflowai.app.service.NodeExecutor;
import io.toflowai.common.domain.Node;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Executor for sending Slack messages via webhook.
 * 
 * Parameters:
 *   - webhookUrl: Slack incoming webhook URL
 *   - message: The message text (supports {{ interpolation }})
 *   - channel: Optional channel override
 */
@Component  // This annotation registers it with Spring!
public class SlackMessageExecutor implements NodeExecutor {
    
    private final HttpClient httpClient;
    
    public SlackMessageExecutor() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
    }
    
    @Override
    public String getNodeType() {
        // This MUST match the type used in the UI
        return "slackMessage";
    }
    
    @Override
    public Map<String, Object> execute(
            Node node, 
            Map<String, Object> input, 
            ExecutionService.ExecutionContext context) {
        
        // 1. Get parameters from node configuration
        Map<String, Object> params = node.parameters();
        String webhookUrl = (String) params.get("webhookUrl");
        String message = interpolate((String) params.get("message"), input);
        String channel = (String) params.getOrDefault("channel", "");
        
        // 2. Validate required parameters
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new IllegalArgumentException("Slack webhook URL is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message is required");
        }
        
        // 3. Build Slack payload
        StringBuilder payload = new StringBuilder();
        payload.append("{\"text\":\"").append(escapeJson(message)).append("\"");
        if (!channel.isBlank()) {
            payload.append(",\"channel\":\"").append(escapeJson(channel)).append("\"");
        }
        payload.append("}");
        
        // 4. Send HTTP request to Slack
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
            
            HttpResponse<String> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofString()
            );
            
            // 5. Build output
            Map<String, Object> output = new HashMap<>();
            output.put("success", response.statusCode() == 200);
            output.put("statusCode", response.statusCode());
            output.put("response", response.body());
            output.put("messageSent", message);
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Slack API error: " + response.body());
            }
            
            return output;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to send Slack message: " + e.getMessage(), e);
        }
    }
    
    // Helper: Interpolate {{ variables }}
    private String interpolate(String template, Map<String, Object> data) {
        if (template == null) return "";
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{\\s*([^}]+)\\s*\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(template);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object value = data.getOrDefault(key, "");
            matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    // Helper: Escape JSON special characters
    private String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
```

#### Step 2: Add to Node Palette (UI)

In `WorkflowCanvas.java`, add the node to the palette:

```java
// In createNodePalette() method, add:

private VBox createNodePalette() {
    VBox palette = new VBox(10);
    
    // ... existing code ...
    
    // Add new category or node
    TitledPane communicationPane = new TitledPane();
    communicationPane.setText("Communication");
    VBox communicationNodes = new VBox(5);
    
    communicationNodes.getChildren().addAll(
        createPaletteItem("Slack Message", "slackMessage", MaterialDesignS.SLACK),
        createPaletteItem("Email", "email", MaterialDesignE.EMAIL),
        createPaletteItem("SMS", "sms", MaterialDesignM.MESSAGE_TEXT)
    );
    
    communicationPane.setContent(communicationNodes);
    palette.getChildren().add(communicationPane);
    
    return palette;
}
```

#### Step 3: Add Icon Mapping

In `NodeView.java`, add the icon:

```java
private FontIcon getIconForNodeType(String type) {
    return switch (type) {
        case "httpRequest" -> FontIcon.of(MaterialDesignH.HTTP, 24);
        case "code" -> FontIcon.of(MaterialDesignC.CODE_BRACES, 24);
        case "if" -> FontIcon.of(MaterialDesignS.SOURCE_BRANCH, 24);
        // Add new node type:
        case "slackMessage" -> FontIcon.of(MaterialDesignS.SLACK, 24);
        default -> FontIcon.of(MaterialDesignC.CUBE_OUTLINE, 24);
    };
}
```

#### Step 4: Add Properties Configuration

In `NodePropertiesPanel.java`, add the node's configurable fields:

```java
private void buildPropertiesForm(Node node) {
    // ... existing code ...
    
    switch (node.type()) {
        case "slackMessage" -> {
            addTextField("Webhook URL", "webhookUrl", params);
            addTextArea("Message", "message", params);
            addTextField("Channel (optional)", "channel", params);
        }
        // ... other node types ...
    }
}
```

#### Step 5: Test It!

1. Build: `.\gradlew.bat clean build -x test`
2. Run: `.\gradlew.bat :app:bootRun`
3. Drag "Slack Message" from palette
4. Configure webhook URL and message
5. Connect to other nodes
6. Run workflow

### 11.2 Adding a New Setting

#### Step 1: Define in SettingsDefaults

```java
// In SettingsDefaults.java, add to the defaults list:

public static final String SLACK_DEFAULT_CHANNEL = "slack.default_channel";

// In getDefaults() method:
defaults.add(SettingDTO.full(
    SLACK_DEFAULT_CHANNEL,
    "#general",                    // Default value
    SettingCategory.NOTIFICATIONS, // Category
    SettingType.STRING,            // Type
    "Default Slack Channel",       // Label
    "Default channel for Slack notifications",
    true,                          // Visible
    false,                         // Requires restart
    5,                             // Display order
    null                           // Validation rules (JSON)
));
```

#### Step 2: Use the Setting

```java
// In SlackMessageExecutor:
private final SettingsServiceInterface settingsService;

public SlackMessageExecutor(SettingsServiceInterface settingsService) {
    this.settingsService = settingsService;
}

@Override
public Map<String, Object> execute(...) {
    // Get default channel from settings if not specified
    String channel = (String) params.getOrDefault(
        "channel", 
        settingsService.getValue(SettingsDefaults.SLACK_DEFAULT_CHANNEL, "#general")
    );
    // ... rest of implementation
}
```

### 11.3 Adding a New Dialog

```java
// File: ui/src/main/java/io/toflowai/ui/dialog/MyCustomDialog.java

package io.toflowai.ui.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

public class MyCustomDialog extends Dialog<String> {
    
    private final TextField inputField;
    
    public MyCustomDialog() {
        setTitle("My Custom Dialog");
        initModality(Modality.APPLICATION_MODAL);
        
        // Content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #2e3440;");
        
        Label label = new Label("Enter something:");
        label.setStyle("-fx-text-fill: #eceff4;");
        
        inputField = new TextField();
        inputField.setPromptText("Type here...");
        
        content.getChildren().addAll(label, inputField);
        
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Convert result
        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return inputField.getText();
            }
            return null;
        });
    }
}

// Usage:
MyCustomDialog dialog = new MyCustomDialog();
Optional<String> result = dialog.showAndWait();
result.ifPresent(text -> System.out.println("User entered: " + text));
```

---

## 12. Best Practices

### 12.1 Code Organization

```
✅ DO:
- One class per file
- Group related classes in packages
- Keep methods under 30 lines
- Use meaningful names

❌ DON'T:
- Put multiple classes in one file
- Create "god classes" with too many responsibilities
- Use abbreviations in names
- Mix UI logic with business logic
```

### 12.2 Java Records

Use records for immutable data carriers:

```java
// ✅ Use records for DTOs and domain objects
public record Node(
    String id,
    String type,
    Map<String, Object> parameters
) {}

// ✅ Records can have methods
public record Position(double x, double y) {
    public Position add(double dx, double dy) {
        return new Position(x + dx, y + dy);
    }
}
```

### 12.3 Error Handling

```java
// ✅ Good: Specific error handling
try {
    HttpResponse<String> response = httpClient.send(request, ...);
    if (response.statusCode() >= 400) {
        throw new ApiException("API error: " + response.body());
    }
} catch (IOException e) {
    throw new NetworkException("Network error: " + e.getMessage(), e);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    throw new ExecutionException("Request was interrupted", e);
}

// ❌ Bad: Catching generic Exception
try {
    // ... 
} catch (Exception e) {
    e.printStackTrace();  // Never do this!
}
```

### 12.4 Null Safety

```java
// ✅ Good: Use Optional
public Optional<WorkflowDTO> findById(Long id) {
    return repository.findById(id).map(this::toDTO);
}

// ✅ Good: Null checks with default
String value = params.getOrDefault("key", "default");

// ❌ Bad: Returning null
public WorkflowDTO findById(Long id) {
    return repository.findById(id).orElse(null);  // Avoid!
}
```

### 12.5 Logging

```java
// ✅ Good: Structured logging
log.info("Executing node {} of type {}", node.id(), node.type());
log.error("Failed to execute node {}: {}", node.id(), e.getMessage(), e);

// ❌ Bad: String concatenation
log.info("Executing node " + node.id() + " of type " + node.type());
```

### 12.6 Testing

```java
// Unit test example
@Test
void httpRequestExecutor_shouldReturnSuccessForValidUrl() {
    // Arrange
    Node node = new Node(
        "test-id",
        "httpRequest",
        "Test HTTP",
        new Node.Position(0, 0),
        Map.of("url", "https://httpbin.org/get", "method", "GET"),
        null,
        false,
        null
    );
    
    // Act
    Map<String, Object> result = executor.execute(node, Map.of(), context);
    
    // Assert
    assertEquals(200, result.get("statusCode"));
    assertTrue((Boolean) result.get("success"));
}
```

---

## 13. Troubleshooting

### 13.1 Common Issues

| Problem | Cause | Solution |
|---------|-------|----------|
| "Node type not found" | Executor not registered | Ensure `@Component` annotation is present |
| JavaFX not starting | Wrong Java version | Use Java 25 with `JAVA_HOME` set |
| Database error | Missing migration | Check Flyway migrations |
| UI not updating | Not on FX thread | Use `Platform.runLater()` |
| Settings not saved | Missing `@Transactional` | Add annotation to service method |

### 13.2 Debugging Tips

```java
// Print debug info
System.out.println("Node parameters: " + node.parameters());

// Check execution context
log.debug("Current context: workflowId={}, input={}", 
    context.getWorkflowId(), context.getInput());

// Debug JavaFX layout issues
node.setStyle("-fx-border-color: red; -fx-border-width: 2;");
```

### 13.3 VS Code Tips

1. **Clean Java Workspace**: `Ctrl+Shift+P` → "Java: Clean Java Language Server Workspace"
2. **Reload Window**: `Ctrl+Shift+P` → "Developer: Reload Window"
3. **View Gradle Tasks**: Open "Gradle" sidebar panel

---

## 14. Glossary

| Term | Definition |
|------|------------|
| **Bean** | Object managed by Spring container |
| **DTO** | Data Transfer Object - carries data between layers |
| **Entity** | JPA-managed database object |
| **Executor** | Code that runs a specific node type |
| **FXML** | XML format for JavaFX UI definitions |
| **JPA** | Java Persistence API - database access standard |
| **Node** | Single step in a workflow |
| **Record** | Immutable data carrier class (Java 16+) |
| **Repository** | Spring Data interface for database access |
| **Service** | Business logic component |
| **Trigger** | Node that starts workflow execution |
| **Virtual Thread** | Lightweight thread (Java 21+) |
| **Workflow** | Complete automation blueprint |

---

## 15. Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         TOFLOWAI QUICK REFERENCE                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  BUILD & RUN                                                                    │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  Build:     .\gradlew.bat clean build -x test                                  │
│  Run:       .\gradlew.bat :app:bootRun                                         │
│  Test:      .\gradlew.bat test                                                 │
│                                                                                 │
│  KEY DIRECTORIES                                                                │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  Executors: app/src/main/java/io/toflowai/app/executor/                        │
│  Services:  app/src/main/java/io/toflowai/app/service/                         │
│  UI:        ui/src/main/java/io/toflowai/ui/                                   │
│  Common:    common/src/main/java/io/toflowai/common/                           │
│                                                                                 │
│  ADD NEW NODE TYPE                                                              │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  1. Create executor: executor/MyNodeExecutor.java                              │
│  2. Annotate: @Component                                                       │
│  3. Implement: NodeExecutor interface                                          │
│  4. Add to UI palette: WorkflowCanvas.createNodePalette()                      │
│  5. Add icon: NodeView.getIconForNodeType()                                    │
│                                                                                 │
│  COMMON ANNOTATIONS                                                             │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  @Service      - Business logic service                                        │
│  @Component    - Generic Spring bean                                           │
│  @Repository   - Database access                                               │
│  @Transactional- Database transaction                                          │
│  @FXML         - JavaFX injection                                              │
│                                                                                 │
│  DATA FLOW                                                                      │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  UI → ExecutionService → NodeExecutorRegistry → Executor → DB                  │
│                                                                                 │
│  INTERPOLATION                                                                  │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  Use {{ variableName }} in node parameters                                     │
│  Nested: {{ response.data.name }}                                              │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

**Happy Coding!** 🚀

If you have questions, check:
1. This document
2. Existing code for similar patterns
3. DEVELOPMENT_PLAN.md for feature status
4. Spring Boot / JavaFX documentation

---

*Document maintained by the ToFlowAI team. Last updated: January 29, 2026*
