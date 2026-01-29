# ToFlowAI

<p align="center">
  <img src="docs/assets/logo.png" alt="ToFlowAI Logo" width="200" />
</p>

<p align="center">
  <strong>Visual Workflow Automation for Everyone</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="#documentation">Documentation</a> •
  <a href="#contributing">Contributing</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk" alt="Java 25" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen?style=flat-square&logo=spring" alt="Spring Boot 3.5.0" />
  <img src="https://img.shields.io/badge/JavaFX-21-blue?style=flat-square" alt="JavaFX 21" />
  <img src="https://img.shields.io/badge/Gradle-9.2.0-02303A?style=flat-square&logo=gradle" alt="Gradle 9.2.0" />
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="License MIT" />
</p>

---

## 🚀 What is ToFlowAI?

ToFlowAI is a **visual workflow automation application** similar to [n8n](https://n8n.io), [Zapier](https://zapier.com), or [Make.com](https://make.com). It's also inspired by [Sim.ai](https://sim.ai) but designed as a **simpler, local-first alternative** that runs entirely on your machine—no cloud deployment, no infrastructure hassles. Just **install and use**.

Create automated workflows by connecting "nodes" (building blocks) together visually, with your data staying completely local.

Built with **Java 25**, **Spring Boot 3.5**, and **JavaFX 21**, ToFlowAI runs as a native desktop application with a modern dark theme powered by [AtlantaFX](https://github.com/mkpaz/atlantafx).

---

## ✨ Features

### 🎨 Visual Workflow Designer
- Drag-and-drop node placement
- Visual connection system with smooth curves
- Real-time execution preview
- Pan, zoom, and grid snapping

### 🔧 Node Types
| Category | Nodes |
|----------|-------|
| **Triggers** | Manual Trigger, Schedule Trigger, Webhook |
| **Actions** | HTTP Request, Code (JavaScript), Set Data |
| **Flow Control** | IF Conditional, Loop, Merge |
| **AI/LLM** | LLM Chat (OpenAI, Anthropic, etc.) |

### ⚡ Execution Engine
- Real-time step-by-step execution
- Expression interpolation (`{{ variable }}` syntax)
- Error handling with retry and fallback patterns
- Execution history and debugging

### 🔐 Settings & Configuration
- Secure credential storage (AES-256 encryption)
- Import/Export settings as JSON
- Per-workflow and global settings
- API key management for LLM providers

### 💾 Data Management
- H2 embedded database
- Flyway database migrations
- Workflow import/export (JSON)
- Sample workflows included

---

## 📸 Screenshots

<p align="center">
  <em>Screenshots coming soon...</em>
</p>

<!-- 
<p align="center">
  <img src="docs/assets/screenshot-canvas.png" alt="Workflow Canvas" width="800" />
  <br />
  <em>Visual Workflow Canvas</em>
</p>

<p align="center">
  <img src="docs/assets/screenshot-execution.png" alt="Execution View" width="800" />
  <br />
  <em>Real-time Execution</em>
</p>
-->

---

## 🏁 Getting Started

### Prerequisites

- **Java 25** (Microsoft OpenJDK recommended)
  ```powershell
  # Windows - Download from:
  # https://learn.microsoft.com/en-us/java/openjdk/download
  ```
- **Git** for cloning the repository

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/ToFlowAI.git
   cd ToFlowAI
   ```

2. **Set JAVA_HOME** (if not already set)
   ```powershell
   # Windows PowerShell
   $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.0.36-hotspot"
   ```

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run the application**
   ```bash
   ./gradlew :app:bootRun
   ```

### First Run

1. The application will start with an empty canvas
2. **Create a new workflow**: File → New Workflow
3. **Add nodes**: Drag from the Node Palette on the left
4. **Connect nodes**: Drag from output port to input port
5. **Execute**: Click the ▶️ Run button

---

## 📁 Project Structure

```
ToFlowAI/
├── app/                    # Main Spring Boot application
│   └── src/main/java/
│       └── io.toflowai.app/
│           ├── config/     # Spring configuration
│           ├── controller/ # REST API endpoints
│           ├── service/    # Business logic
│           └── repository/ # Data access
│
├── ui/                     # JavaFX UI module
│   └── src/main/java/
│       └── io.toflowai.ui/
│           ├── canvas/     # Workflow canvas components
│           ├── nodes/      # Node UI implementations
│           ├── dialogs/    # Settings, properties dialogs
│           └── theme/      # AtlantaFX theming
│
├── common/                 # Shared DTOs and utilities
│   └── src/main/java/
│       └── io.toflowai.common/
│           ├── dto/        # Data Transfer Objects
│           ├── enums/      # NodeType, etc.
│           └── util/       # Shared utilities
│
├── samples/                # Sample workflow JSON files
│   ├── 01-weather-alert-workflow.json
│   ├── 02-ai-content-generator.json
│   ├── 03-data-processing-pipeline.json
│   ├── 04-multi-api-integration.json
│   ├── 05-error-handling-demo.json
│   └── README.md
│
├── docs/                   # Documentation
│   └── ARCHITECTURE.md     # Comprehensive architecture guide
│
├── tools/                  # Build and packaging tools
│   └── build-installer.ps1 # Windows installer builder
├── .github/workflows/      # CI/CD pipelines
│   └── build.yml           # Build, test, analyze, package
├── build.gradle            # Root build configuration
├── sonar-project.properties # SonarQube configuration
└── settings.gradle         # Multi-module settings
```

---

## 🛠️ Technology Stack

| Layer | Technology | Version |
|-------|------------|---------|
| **Language** | Java | 25 |
| **Backend** | Spring Boot | 3.5.0 |
| **Frontend** | JavaFX | 21.0.5 |
| **UI Theme** | AtlantaFX (NordDark) | 2.0.1 |
| **Database** | H2 (embedded) | - |
| **Migrations** | Flyway | 10.20.1 |
| **Build** | Gradle | 9.2.0 |
| **JavaScript** | GraalVM JS | 24.1.1 |
| **Code Quality** | SonarQube | 6.0.1 |
| **Testing** | JUnit 5, TestFX, ArchUnit | - |

---

## 📦 Building Installers

ToFlowAI can be packaged as a standalone installer with an embedded GraalVM runtime. Users don't need Java installed.

### Windows MSI Installer

```powershell
# Build installer with embedded runtime
.\tools\build-installer.ps1

# Or with specific version
.\tools\build-installer.ps1 -Version "1.0.0" -Clean
```

The installer will be created at `app/build/distributions/ToFlowAI-{version}.msi`

### What's Included

- ✅ ToFlowAI application
- ✅ Embedded GraalVM runtime (~60MB compressed)
- ✅ All dependencies bundled
- ✅ Windows Start Menu shortcuts
- ✅ No Java installation required

---

## 🔍 Code Quality & Analysis

### SonarQube Integration

ToFlowAI uses SonarQube for static code analysis, code coverage, and quality gates.

```bash
# Run local analysis (requires SonarQube server)
./gradlew sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=YOUR_TOKEN

# With Docker
docker run -d --name sonarqube -p 9000:9000 sonarqube:community
# Wait for startup, create project, then run analysis
```

### Coverage Reports

```bash
# Generate JaCoCo coverage reports
./gradlew test jacocoTestReport

# Reports at: app/build/reports/jacoco/test/html/index.html
```

---

## 📖 Documentation

- **[Architecture Guide](docs/ARCHITECTURE.md)** - Comprehensive guide for developers
- **[Sample Workflows](samples/README.md)** - Ready-to-import workflow examples
- **[API Reference](docs/API.md)** - REST API documentation *(coming soon)*

---

## 🧪 Sample Workflows

ToFlowAI includes sample workflows to help you get started:

| Sample | Description | Features Demonstrated |
|--------|-------------|----------------------|
| **Weather Alert** | Monitor weather and send alerts | HTTP Request, Code, IF |
| **AI Content Generator** | Generate content with LLMs | LLM Chat, Chained calls |
| **Data Processing** | Batch process with filtering | Loop, Merge, Aggregation |
| **Multi-API Integration** | Combine multiple APIs + AI | HTTP Chain, LLM |
| **Error Handling** | Retry and fallback patterns | Error handling, Conditionals |

Import via: **File** → **Import Workflow** → Select from `samples/` directory

---

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API key for LLM nodes | - |
| `ANTHROPIC_API_KEY` | Anthropic API key | - |
| `APP_VERSION` | Application version | `0.1.0-SNAPSHOT` |

### Application Settings

Settings are managed via the **Settings** dialog (`Ctrl+,`) and stored securely in the H2 database with AES-256 encryption for sensitive values.

---

## 🤝 Contributing

Contributions are welcome! Please read our contributing guidelines before submitting a PR.

### Development Setup

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes
4. Run tests: `./gradlew test`
5. Commit: `git commit -m 'Add amazing feature'`
6. Push: `git push origin feature/amazing-feature`
7. Open a Pull Request

### Code Style

- Follow Java conventions (camelCase for methods, PascalCase for classes)
- Use `record` for immutable DTOs
- Add JavaDoc for public APIs
- Write unit tests for new features

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [n8n](https://n8n.io) - Inspiration for workflow automation concepts
- [AtlantaFX](https://github.com/mkpaz/atlantafx) - Beautiful JavaFX themes
- [Ikonli](https://kordamp.org/ikonli/) - Icon packs for JavaFX
- [GraalVM](https://www.graalvm.org/) - JavaScript execution engine

---

<p align="center">
  Made with ❤️ by the ToFlowAI Team
</p>
