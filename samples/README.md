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

Configure these in **Settings** > **AI Providers** before running:

| Setting | Used By | Get Key From |
|---------|---------|--------------|
| `AI_OPENAI_API_KEY` | AI Content Generator, Multi-API Integration | [OpenAI Platform](https://platform.openai.com/api-keys) |
| `AI_ANTHROPIC_API_KEY` | (Optional) Alternative AI provider | [Anthropic Console](https://console.anthropic.com/) |

### External API Keys (in workflow settings)

Some workflows use external APIs. You'll need to:

1. Get an API key from the service
2. Update the workflow settings or `Set` node with your key

| API | Workflows | Get Key From |
|-----|-----------|--------------|
| OpenWeatherMap | Weather Alert, Multi-API Integration | [OpenWeatherMap](https://openweathermap.org/api) |

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
1. Get a free API key from OpenWeatherMap
2. Update the `apiKey` in the HTTP Request node URL
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
1. Configure your OpenAI API key in Settings
2. Modify the topic in the "Set Topic" node
3. Run the workflow
4. View generated content in console

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
| "API key not found" | Configure keys in Settings > AI Providers |
| HTTP timeout | Increase timeout in node settings or HTTP settings |
| "Node type not found" | Ensure you're using a compatible ToFlowAI version |
| Empty LLM response | Check API key is valid and has credits |

## Creating Your Own Workflows

Use these samples as templates! Common patterns:

1. **API Integration:** HTTP Request → Code (Parse) → Process
2. **Conditional Logic:** Data → IF → Branch A / Branch B → Merge
3. **AI Enhancement:** Data → LLM → Code (Format) → Output
4. **Batch Processing:** Data → Loop → Process Each → Aggregate
5. **Error Handling:** Try → IF (Success) → Continue / Retry

---

*For more information, see the [Architecture Guide](../docs/ARCHITECTURE.md)*
