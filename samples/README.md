# ToFlowAI Sample Workflows

This directory contains sample workflow JSON files that demonstrate various features of ToFlowAI.

## Available Samples

| # | Workflow | Description | Features Demonstrated |
|---|----------|-------------|----------------------|
| 01 | [Weather Alert](01-weather-alert-workflow.json) | Fetches weather data and sends alerts based on temperature | HTTP Request, Code, IF Conditional, Data Flow |
| 02 | [AI Content Generator](02-ai-content-generator.json) | Uses LLM to generate and validate content | LLM Chat, Code, IF Conditional, Chained AI Calls |
| 03 | [Data Processing Pipeline](03-data-processing-pipeline.json) | Processes item lists with filtering and aggregation | Loop, Code, IF, Merge, Data Aggregation |
| 04 | [Multi-API Integration](04-multi-api-integration.json) | Chains multiple APIs with AI enrichment | HTTP Requests, LLM, Data Transformation |
| 05 | [Error Handling Demo](05-error-handling-demo.json) | Demonstrates retry logic and fallback patterns | Error Handling, Retry, Fallback, Conditional Flows |

## How to Import

1. Open ToFlowAI
2. Go to **File** > **Import Workflow** (or use `Ctrl+I`)
3. Select a `.json` file from this directory
4. The workflow will appear in your canvas

## Prerequisites

### API Keys Required

You have **three secure options** for managing API keys. Choose the one that works best for your workflow sharing and security needs:

#### Option 1: Global Settings (Easiest for Personal Use)
Configure API keys in **Settings** > **AI Providers**:

| Setting | Used By | Get Key From |
|---------|---------|--------------|
| `AI_OPENAI_API_KEY` | AI Content Generator, Multi-API Integration | [OpenAI Platform](https://platform.openai.com/api-keys) |
| `AI_ANTHROPIC_API_KEY` | (Optional) Alternative AI provider | [Anthropic Console](https://console.anthropic.com/) |

**Pros**: Quick setup, works immediately
**Cons**: Keys stored globally, workflows not portable

#### Option 2: Credential Manager (Recommended for Sharing)
Store API keys securely in the **Tools** > **Credential Manager**:

1. Open **Tools** > **Credential Manager**
2. Click **Add Credential**
3. Create credentials for each API:
   - **Name**: `openai-api` (choose a memorable name)
   - **Type**: `API_KEY`
   - **Data**: Your actual API key
4. In workflow nodes, select the credential from the dropdown

| Credential Name | API Service | Get Key From |
|----------------|-------------|--------------|
| `openai-api` | OpenAI (GPT models) | [OpenAI Platform](https://platform.openai.com/api-keys) |
| `anthropic-api` | Anthropic (Claude models) | [Anthropic Console](https://console.anthropic.com/) |
| `weather-api` | OpenWeatherMap | [OpenWeatherMap](https://openweathermap.org/api) |

**Pros**: Secure encryption, workflow portability, granular access control
**Cons**: Extra setup step per workflow

#### Option 3: Import from .env File (Best for Development Teams)
For development teams or bulk credential management:

1. **Create a `.env` file** in the ToFlowAI root directory:
   ```env
   OPENAI_API_KEY=sk-your-openai-key-here
   WEATHER_API_KEY=your-weather-api-key
   ANTHROPIC_API_KEY=sk-ant-your-anthropic-key
   ```

2. **Automatic Loading**: Credentials are loaded automatically on application startup

3. **Manual Import**: Use the REST API to import credentials on-demand:
   ```bash
   # Import from .env content
   curl -X POST http://localhost:8080/api/credentials/import \
     -F "content=OPENAI_API_KEY=sk-your-key"

   # Import from .env file
   curl -X POST http://localhost:8080/api/credentials/import \
     -F "file=@credentials.env"
   ```

**Pros**: Easy bulk import, version control friendly, team sharing
**Cons**: Requires API access or .env file management

### External API Keys (Workflow-Specific)

Some workflows require additional API keys that are configured within the workflow itself. These can be set up using either approach above:

| API | Workflows | Setup Method |
|-----|-----------|--------------|
| OpenWeatherMap | Weather Alert, Multi-API Integration | Use credential `weather-api` in HTTP Request node, or set in workflow input |

**Note**: For maximum security and portability, use the Credential Manager option and reference credentials by name in your workflows.

## How to Use Credentials in Workflows

When using the Credential Manager option:

1. **HTTP Request nodes**: Select your credential from the "Authentication" dropdown in node properties
2. **LLM nodes**: Currently fall back to global settings (credential support coming soon)
3. **Workflow sharing**: Credentials are referenced by ID, so workflows remain secure when shared

**Example**: In an HTTP Request node, instead of putting your API key directly in the URL, select the appropriate credential from the dropdown. The system will automatically use the encrypted key during execution.

### Bulk Credential Management

For teams or complex setups, you can import multiple credentials at once:

#### Via .env File (Automatic)
1. Create `.env` file in ToFlowAI root directory
2. Credentials load automatically on startup
3. Format: `KEY_NAME=value` (one per line)

#### Via API (Manual)
```bash
# Import multiple credentials
curl -X POST http://localhost:8080/api/credentials/import \
  -F "content=API_KEY_1=secret1
API_KEY_2=secret2
THIRD_KEY=secret3"
```

The API returns detailed results showing which credentials were created, skipped, or had errors.

## Workflow Details

### 01 - Weather Alert Workflow

**Use Case:** Automated weather monitoring with conditional alerts

```
Manual Trigger → HTTP Request → Code (Parse) → IF (Temp > 25°C)
                                                    ├── TRUE → Format Hot Alert
                                                    └── FALSE → Format Normal Status
                                                            └── Final Output
```

**To Test:**
1. **Using Credentials (Recommended)**:
   - Create a credential named `weather-api` with your OpenWeatherMap API key
   - In the HTTP Request node, select `weather-api` from the Authentication dropdown
2. **Using Direct Key (Less Secure)**:
   - Get a free API key from OpenWeatherMap
   - Update the `apiKey` in the HTTP Request node URL or pass as workflow input
3. Run the workflow
4. Check console for weather alert or normal status

---

### 02 - AI Content Generator

**Use Case:** Automated content creation with AI

```
Manual Trigger → Set Topic → LLM (Outline) → LLM (Content) → Code (Format)
                                                                └── IF (Valid)
                                                                    ├── TRUE → Success
                                                                    └── FALSE → Error
```

**To Test:**
1. **Current Method**: Configure your OpenAI API key in **Settings** > **AI Providers**
2. **Future Method**: Credential support for LLM nodes coming soon - you'll be able to select credentials directly in the node
3. Modify the topic in the "Set Topic" node
4. Run the workflow
5. View generated content in console

---

### 03 - Data Processing Pipeline

**Use Case:** Batch processing with filtering and aggregation

```
Manual Trigger → Set Data → Loop (Each Product)
                                └── IF (In Stock)
                                    ├── TRUE → IF (Min Price)
                                    │           ├── TRUE → Apply Discount
                                    │           └── FALSE → Mark Skipped
                                    └── FALSE → Mark Skipped
                                            └── Merge → Aggregate Results
```

**To Test:**
1. No API keys needed - uses sample data
2. Modify the products array in "Sample Data" node
3. Adjust `discountPercent` and `minPrice` parameters
4. Run and view aggregated results

---

### 04 - Multi-API Integration

**Use Case:** Combining multiple data sources with AI enhancement

```
Manual Trigger → Get Random User → Parse User → Get Weather → Parse Weather
                                                                └── LLM (Greeting) → Build Profile
```

**To Test:**
1. Configure OpenAI API key in Settings
2. Get OpenWeatherMap API key
3. Run workflow
4. View enriched profile with AI greeting

---

### 05 - Error Handling Demo

**Use Case:** Building robust workflows with retry and fallback

```
Manual Trigger → Config → Attempt API
                            └── IF (Success)
                                ├── TRUE → Process Success → Report
                                └── FALSE → Check Retry
                                            └── IF (Retry Available)
                                                ├── TRUE → Wait → Attempt API
                                                └── FALSE → Fallback API → Process Success
```

**To Test:**
1. No API keys needed - uses simulated responses
2. Toggle `simulateError` in Configuration node
3. Run with `true` to see retry/fallback path
4. Run with `false` to see success path

## Customizing Workflows

After importing, you can:

1. **Modify Parameters:** Click any node to edit its configuration
2. **Add Nodes:** Drag from the palette on the left
3. **Change Connections:** Delete and redraw connection lines
4. **Save As:** Save with a new name to keep the original

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "API key not found" | Configure keys in **Settings** > **AI Providers**, or create credentials in **Tools** > **Credential Manager**, or import via `.env` file |
| "Credential not found" | Ensure the credential exists and is selected in the node properties |
| HTTP timeout | Increase timeout in node settings or HTTP settings |
| "Node type not found" | Ensure you're using a compatible ToFlowAI version |
| Empty LLM response | Check API key is valid and has credits, or verify credential is properly configured |
| "Authentication failed" | Verify credential data is correct and hasn't expired |
| "Import failed" | Check `.env` file format (KEY=value), ensure API is running, verify file permissions |
| "Duplicate credential" | Import skips existing credentials; delete old ones first if needed |

## Security Best Practices

- **Always use Credential Manager** for production workflows - it encrypts your API keys
- **Never commit API keys** to version control or share workflows with embedded keys
- **Use different credentials** for different environments (dev/staging/prod)
- **Regularly rotate** API keys and update credentials in the manager
- **Test credentials** after creation using the built-in test functionality
- **Use .env files** for development teams - they can be gitignored and imported securely
- **Import credentials** via API for automated deployment scenarios
- **Monitor credential usage** through the manager's audit features

## Creating Your Own Workflows

Use these samples as templates! Common patterns:

1. **API Integration:** HTTP Request → Code (Parse) → Process
2. **Conditional Logic:** Data → IF → Branch A / Branch B → Merge
3. **AI Enhancement:** Data → LLM → Code (Format) → Output
4. **Batch Processing:** Data → Loop → Process Each → Aggregate
5. **Error Handling:** Try → IF (Success) → Continue / Retry

---

*For more information, see the [Architecture Guide](../docs/ARCHITECTURE.md)*
