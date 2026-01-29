package io.toflowai.ui.canvas;

/**
 * Provides help content and sample usage for each node type.
 * Used to display help dialogs when users click the help button on nodes.
 */
public final class NodeHelpProvider {

    private NodeHelpProvider() {
        // Utility class
    }

    /**
     * Get the help content for a node type.
     */
    public static NodeHelp getHelp(String nodeType) {
        return switch (nodeType) {
            // Triggers
            case "manualTrigger" -> new NodeHelp(
                    "Manual Trigger",
                    "Starts a workflow manually when you click the 'Execute' button.",
                    """
                            The Manual Trigger is the simplest way to start a workflow. Use it when:
                            • You want to run a workflow on-demand
                            • Testing and debugging workflows
                            • One-time data processing tasks

                            The node outputs an empty object that downstream nodes can use as a starting point.
                            """,
                    """
                            // Output data structure
                            {
                              "timestamp": "2026-01-29T10:30:00Z",
                              "triggeredBy": "manual"
                            }

                            // Connect to any action node to start processing
                            Manual Trigger → HTTP Request → Set → ...
                            """);

            case "scheduleTrigger" -> new NodeHelp(
                    "Schedule Trigger",
                    "Starts a workflow automatically based on a cron schedule.",
                    """
                            Use cron expressions to schedule workflow execution:
                            • Run daily reports at midnight
                            • Sync data every hour
                            • Weekly cleanup tasks

                            Parameters:
                            • cronExpression: Standard cron format (e.g., "0 0 * * *" for daily at midnight)
                            • timezone: Optional timezone (defaults to system timezone)
                            """,
                    """
                            // Configuration example
                            {
                              "cronExpression": "0 9 * * MON-FRI",  // 9 AM weekdays
                              "timezone": "America/New_York"
                            }

                            // Common cron patterns:
                            "*/15 * * * *"     → Every 15 minutes
                            "0 * * * *"        → Every hour
                            "0 0 * * *"        → Daily at midnight
                            "0 0 * * 0"        → Weekly on Sunday
                            "0 0 1 * *"        → Monthly on the 1st
                            """);

            case "webhookTrigger" -> new NodeHelp(
                    "Webhook Trigger",
                    "Starts a workflow when an HTTP request is received at the webhook URL.",
                    """
                            Creates an HTTP endpoint that triggers the workflow when called:
                            • Receive data from external services
                            • API integrations (GitHub, Slack, etc.)
                            • Custom application callbacks

                            Parameters:
                            • path: URL path for the webhook (e.g., "/my-webhook")
                            • method: HTTP method to accept (GET, POST, etc.)
                            • authentication: Optional auth requirements
                            """,
                    """
                            // Configuration
                            {
                              "path": "/incoming-data",
                              "method": "POST",
                              "responseCode": 200,
                              "responseBody": "{ \"status\": \"received\" }"
                            }

                            // Incoming request data is available as:
                            {
                              "headers": { "Content-Type": "application/json" },
                              "query": { "id": "123" },
                              "body": { "name": "John", "email": "john@example.com" },
                              "method": "POST"
                            }
                            """);

            // Actions
            case "httpRequest" -> new NodeHelp(
                    "HTTP Request",
                    "Makes HTTP requests to external APIs and services.",
                    """
                            Send HTTP requests and process responses:
                            • Call REST APIs
                            • Download data
                            • Post data to services

                            Parameters:
                            • url: The URL to request (supports variable interpolation)
                            • method: GET, POST, PUT, DELETE, PATCH
                            • headers: Request headers as JSON object
                            • body: Request body (for POST/PUT/PATCH)
                            • timeout: Request timeout in seconds
                            """,
                    """
                            // Configuration example
                            {
                              "url": "https://api.example.com/users/{{userId}}",
                              "method": "POST",
                              "headers": {
                                "Authorization": "Bearer {{apiToken}}",
                                "Content-Type": "application/json"
                              },
                              "body": {
                                "name": "{{userName}}",
                                "email": "{{userEmail}}"
                              },
                              "timeout": 30
                            }

                            // Output includes response body, status, and headers
                            {
                              "statusCode": 200,
                              "body": { "id": 123, "created": true },
                              "headers": { "Content-Type": "application/json" }
                            }
                            """);

            case "code" -> new NodeHelp(
                    "Code",
                    "Execute custom JavaScript/TypeScript code to transform data.",
                    """
                            Write custom logic to process workflow data:
                            • Transform data structures
                            • Perform calculations
                            • Implement custom business logic

                            Available variables:
                            • $input: Data from the previous node
                            • $env: Environment variables
                            • $workflow: Workflow metadata

                            The code must return the output data.
                            """,
                    """
                            // Example: Transform user data
                            const users = $input.data;

                            const transformed = users.map(user => ({
                              fullName: `${user.firstName} ${user.lastName}`,
                              email: user.email.toLowerCase(),
                              isActive: user.status === 'active'
                            }));

                            return { users: transformed };

                            // Example: Filter and aggregate
                            const orders = $input.orders;
                            const total = orders
                              .filter(o => o.status === 'completed')
                              .reduce((sum, o) => sum + o.amount, 0);

                            return { totalCompleted: total, count: orders.length };
                            """);

            case "executeCommand" -> new NodeHelp(
                    "Execute Command",
                    "Run shell commands on the system.",
                    """
                            Execute system commands and capture output:
                            • Run scripts (bash, PowerShell, Python)
                            • Execute CLI tools
                            • System administration tasks

                            Parameters:
                            • command: The command to execute
                            • args: Command arguments as array
                            • cwd: Working directory
                            • timeout: Execution timeout in seconds

                            ⚠️ Security: Be careful with user input in commands!
                            """,
                    """
                            // Configuration example
                            {
                              "command": "python",
                              "args": ["process_data.py", "--input", "{{inputFile}}"],
                              "cwd": "/home/user/scripts",
                              "timeout": 60
                            }

                            // Output structure
                            {
                              "exitCode": 0,
                              "stdout": "Processing complete. 150 records processed.",
                              "stderr": "",
                              "duration": 2345
                            }

                            // Shell command example
                            {
                              "command": "bash",
                              "args": ["-c", "ls -la /data | grep .csv"]
                            }
                            """);

            // Flow Control
            case "if" -> new NodeHelp(
                    "If (Condition)",
                    "Routes workflow based on a condition - true or false branch.",
                    """
                            Evaluate a condition and route to different branches:
                            • Split workflow based on data values
                            • Implement business rules
                            • Handle different scenarios

                            Parameters:
                            • condition: JavaScript expression that returns true/false

                            Outputs:
                            • true branch: Executed when condition is true
                            • false branch: Executed when condition is false
                            """,
                    """
                            // Condition examples
                            {{amount}} > 1000                    // Numeric comparison
                            {{status}} === 'approved'            // String equality
                            {{items}}.length > 0                 // Array check
                            {{user.role}} === 'admin'            // Nested property
                            {{email}}.includes('@company.com')   // String method

                            // Complex conditions
                            {{age}} >= 18 && {{country}} === 'US'
                            {{score}} > 80 || {{isVIP}} === true

                            // Workflow structure:
                            If Condition
                              ├── true → Send Approval Email
                              └── false → Send Rejection Email
                            """);

            case "switch" -> new NodeHelp(
                    "Switch",
                    "Routes workflow to multiple branches based on a value.",
                    """
                            Route to different branches based on matching values:
                            • Multi-way branching (like switch/case)
                            • Route by status, type, category, etc.
                            • Includes default branch for unmatched values

                            Parameters:
                            • value: The value to switch on
                            • cases: Map of values to output names
                            • default: Default output if no match
                            """,
                    """
                            // Configuration example
                            {
                              "value": "{{orderStatus}}",
                              "cases": {
                                "pending": "processPending",
                                "approved": "processApproved",
                                "shipped": "processShipped",
                                "delivered": "processDelivered"
                              },
                              "default": "handleUnknown"
                            }

                            // Workflow structure:
                            Switch (orderStatus)
                              ├── pending → Create Invoice
                              ├── approved → Notify Warehouse
                              ├── shipped → Send Tracking Email
                              ├── delivered → Request Review
                              └── default → Log Unknown Status
                            """);

            case "merge" -> new NodeHelp(
                    "Merge",
                    "Combines data from multiple branches into a single output.",
                    """
                            Merge data from parallel branches:
                            • Combine results from multiple API calls
                            • Aggregate data from different sources
                            • Wait for all branches to complete

                            Modes:
                            • append: Combine all items into an array
                            • merge: Merge objects (last value wins)
                            • waitAll: Wait for all inputs before continuing
                            """,
                    """
                            // Configuration
                            {
                              "mode": "append",
                              "outputKey": "results"
                            }

                            // Input from Branch 1: { "users": [...] }
                            // Input from Branch 2: { "orders": [...] }

                            // Output with mode "merge":
                            {
                              "users": [...],
                              "orders": [...]
                            }

                            // Output with mode "append":
                            {
                              "results": [
                                { "source": "branch1", "data": {...} },
                                { "source": "branch2", "data": {...} }
                              ]
                            }
                            """);

            case "loop" -> new NodeHelp(
                    "Loop",
                    "Iterates over an array and processes each item.",
                    """
                            Process arrays item by item:
                            • Iterate over API results
                            • Batch processing
                            • Transform each item individually

                            Parameters:
                            • items: The array to iterate over (or expression)
                            • batchSize: Process items in batches (optional)
                            • parallel: Run iterations in parallel (optional)
                            """,
                    """
                            // Configuration
                            {
                              "items": "{{users}}",
                              "batchSize": 10
                            }

                            // Each iteration receives:
                            {
                              "item": { "id": 1, "name": "John" },
                              "index": 0,
                              "total": 100
                            }

                            // Workflow structure:
                            Loop (users)
                              └── For each user:
                                    HTTP Request (fetch details)
                                    Set (transform)
                                    → outputs collected
                            """);

            // Data
            case "set" -> new NodeHelp(
                    "Set",
                    "Creates or modifies data fields in the workflow.",
                    """
                            Set, modify, or transform data:
                            • Add new fields
                            • Rename fields
                            • Calculate derived values
                            • Extract nested data

                            Parameters:
                            • values: Object with field names and values/expressions
                            • keepOnlySet: If true, removes all other fields
                            """,
                    """
                            // Configuration example
                            {
                              "values": {
                                "fullName": "{{firstName}} {{lastName}}",
                                "emailLower": "{{email | lowercase}}",
                                "orderTotal": "{{price}} * {{quantity}}",
                                "isHighValue": "{{total}} > 1000",
                                "createdAt": "{{$now}}"
                              },
                              "keepOnlySet": false
                            }

                            // Expression helpers:
                            {{value | uppercase}}        // String to uppercase
                            {{value | lowercase}}        // String to lowercase
                            {{value | trim}}             // Trim whitespace
                            {{array | length}}           // Array length
                            {{$now}}                     // Current timestamp
                            {{$uuid}}                    // Generate UUID
                            """);

            case "filter" -> new NodeHelp(
                    "Filter",
                    "Filters array items based on a condition.",
                    """
                            Keep only items matching a condition:
                            • Filter API results
                            • Remove invalid records
                            • Select specific items

                            Parameters:
                            • items: The array to filter
                            • condition: Expression returning true to keep item
                            • limit: Maximum items to return (optional)
                            """,
                    """
                            // Configuration
                            {
                              "items": "{{orders}}",
                              "condition": "{{item.status}} === 'active' && {{item.amount}} > 100"
                            }

                            // Filter examples:
                            // Keep active users
                            {{item.isActive}} === true

                            // Keep recent orders (last 7 days)
                            new Date({{item.createdAt}}) > new Date(Date.now() - 7*24*60*60*1000)

                            // Keep items with valid email
                            {{item.email}}.includes('@')

                            // Output structure:
                            {
                              "items": [...], // Filtered items
                              "count": 42,    // Number of items kept
                              "removed": 58   // Number of items filtered out
                            }
                            """);

            case "sort" -> new NodeHelp(
                    "Sort",
                    "Sorts array items by a field or expression.",
                    """
                            Order items in an array:
                            • Sort by any field
                            • Ascending or descending
                            • Multiple sort keys

                            Parameters:
                            • items: The array to sort
                            • sortBy: Field name or expression
                            • order: 'asc' or 'desc'
                            """,
                    """
                            // Configuration - Simple sort
                            {
                              "items": "{{products}}",
                              "sortBy": "price",
                              "order": "asc"
                            }

                            // Configuration - Multiple sort keys
                            {
                              "items": "{{users}}",
                              "sortBy": [
                                { "field": "lastName", "order": "asc" },
                                { "field": "firstName", "order": "asc" }
                              ]
                            }

                            // Sort by nested field
                            {
                              "sortBy": "address.city"
                            }

                            // Sort by computed value
                            {
                              "sortBy": "{{item.price}} * {{item.quantity}}"
                            }
                            """);

            // AI
            case "llmChat" -> new NodeHelp(
                    "LLM Chat",
                    "Send prompts to AI language models (OpenAI, Anthropic, Ollama, Azure).",
                    """
                            Interact with Large Language Models:
                            • Generate text and content
                            • Answer questions
                            • Summarize documents
                            • Extract information

                            Supported Providers:
                            • OpenAI (GPT-4, GPT-3.5)
                            • Anthropic (Claude)
                            • Ollama (local models)
                            • Azure OpenAI

                            Parameters:
                            • provider: The AI provider to use
                            • model: Model name (e.g., "gpt-4", "claude-3-sonnet")
                            • prompt: The user message
                            • systemPrompt: System instructions (optional)
                            • temperature: Creativity (0.0-1.0)
                            """,
                    """
                            // Configuration example
                            {
                              "provider": "openai",
                              "model": "gpt-4",
                              "prompt": "Summarize this article: {{articleText}}",
                              "systemPrompt": "You are a helpful assistant that summarizes text concisely.",
                              "temperature": 0.7,
                              "maxTokens": 500
                            }

                            // Output structure:
                            {
                              "response": "The article discusses...",
                              "model": "gpt-4",
                              "usage": {
                                "promptTokens": 150,
                                "completionTokens": 100,
                                "totalTokens": 250
                              }
                            }

                            // Variable interpolation in prompts:
                            "Translate this to {{targetLanguage}}: {{text}}"
                            "Generate a {{tone}} email about {{topic}}"
                            """);

            case "textClassifier" -> new NodeHelp(
                    "Text Classifier",
                    "Classify text into categories using AI.",
                    """
                            AI-powered text classification:
                            • Sentiment analysis
                            • Topic categorization
                            • Intent detection
                            • Spam filtering

                            Parameters:
                            • text: The text to classify
                            • categories: List of possible categories
                            • multiLabel: Allow multiple categories
                            • provider/model: AI backend to use
                            """,
                    """
                            // Configuration - Sentiment Analysis
                            {
                              "text": "{{customerFeedback}}",
                              "categories": ["positive", "negative", "neutral"],
                              "multiLabel": false
                            }

                            // Configuration - Topic Classification
                            {
                              "text": "{{emailBody}}",
                              "categories": [
                                "billing",
                                "technical_support",
                                "sales",
                                "general_inquiry"
                              ],
                              "multiLabel": true
                            }

                            // Output structure:
                            {
                              "classifications": [
                                { "category": "positive", "confidence": 0.92 },
                                { "category": "neutral", "confidence": 0.06 },
                                { "category": "negative", "confidence": 0.02 }
                              ],
                              "topCategory": "positive",
                              "confidence": 0.92
                            }
                            """);

            case "embedding" -> new NodeHelp(
                    "Embedding",
                    "Generate vector embeddings for text using AI models.",
                    """
                            Convert text to numerical vectors:
                            • Semantic search
                            • Similarity comparison
                            • Clustering
                            • RAG preparation

                            Providers:
                            • OpenAI (text-embedding-3-small/large)
                            • Ollama (local embedding models)
                            • Cohere (embed-english-v3.0)

                            Parameters:
                            • text: Text or array of texts to embed
                            • provider: Embedding provider
                            • model: Embedding model name
                            """,
                    """
                            // Configuration - Single text
                            {
                              "provider": "openai",
                              "model": "text-embedding-3-small",
                              "text": "{{documentContent}}"
                            }

                            // Configuration - Batch embedding
                            {
                              "text": ["{{doc1}}", "{{doc2}}", "{{doc3}}"],
                              "normalize": true
                            }

                            // Output structure:
                            {
                              "embedding": [0.023, -0.041, 0.067, ...],  // 1536 dimensions
                              "dimensions": 1536,
                              "model": "text-embedding-3-small"
                            }

                            // Use case - Store for similarity search:
                            Embedding → HTTP Request (save to vector DB)

                            // Use case - Compare similarity:
                            Embedding (text1) ─┐
                                               ├→ Code (calculate cosine similarity)
                            Embedding (text2) ─┘
                            """);

            case "rag" -> new NodeHelp(
                    "RAG (Retrieval-Augmented Generation)",
                    "Combine document retrieval with AI generation for grounded responses.",
                    """
                            RAG Pipeline:
                            1. Embed the user query
                            2. Search vector database for relevant documents
                            3. Build context from retrieved documents
                            4. Generate response using LLM with context

                            Use cases:
                            • Question answering over documents
                            • Knowledge base search
                            • Contextual AI responses

                            Parameters:
                            • query: The user's question
                            • documents: Array of documents with embeddings
                            • topK: Number of documents to retrieve
                            • provider/model: LLM for generation
                            """,
                    """
                            // Configuration
                            {
                              "query": "{{userQuestion}}",
                              "documents": "{{knowledgeBase}}",
                              "topK": 5,
                              "provider": "openai",
                              "model": "gpt-4",
                              "systemPrompt": "Answer based only on the provided context."
                            }

                            // Document format:
                            {
                              "documents": [
                                {
                                  "id": "doc1",
                                  "content": "Full document text...",
                                  "embedding": [0.023, -0.041, ...],
                                  "metadata": { "source": "manual.pdf" }
                                }
                              ]
                            }

                            // Output structure:
                            {
                              "response": "Based on the documentation...",
                              "retrievedDocuments": [
                                { "id": "doc1", "score": 0.92, "content": "..." },
                                { "id": "doc3", "score": 0.87, "content": "..." }
                              ],
                              "context": "Combined context text..."
                            }
                            """);

            default -> new NodeHelp(
                    nodeType,
                    "No description available for this node type.",
                    "This node type doesn't have detailed documentation yet.",
                    "// No sample code available");
        };
    }

    /**
     * Record holding help content for a node.
     */
    public record NodeHelp(
            String title,
            String shortDescription,
            String detailedDescription,
            String sampleCode) {
    }
}
