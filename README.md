# spring-ai-demo-devplay-2026

Multi-module Spring AI MCP server demo for the Dev.Play 2026 conference. Four MCP servers behind SSE, plus a Chroma vector store for RAG and chat-history.

## Modules

| Module | Port | Description |
| --- | --- | --- |
| `devplay-info-mcp` | 8080 | MCP: `DevPlayInformations`, `DevPlaySchedule` |
| `rag-search-mcp` | 8081 | MCP: `searchWiki` (Chroma RAG) |
| `chat-history-mcp` | 8082 | MCP: `searchChatHistory`, `recordChatHistory` |
| `web-search-mcp` | 8083 | MCP: `searchWeb` (Tavily) |
| `agent-orchestrator` | 3000 | Chat UI — Claude Agent SDK + MCP tool tracer |

`common/` is a deps-only library shared by `rag-search-mcp` and `chat-history-mcp` (OpenAI embeddings + Chroma vector store).

## Prerequisites

### MCP servers (Java)

- Java 24
- Docker + Docker Compose (for the full stack)
- `OPENAI_API_KEY` — used by `rag-search-mcp` and `chat-history-mcp` for embeddings
- `TAVILY_API_KEY` — used by `web-search-mcp`

Copy the env template and fill in your keys:

```bash
cp .env.example .env
```

### Agent orchestrator (Node)

- Node 20+
- Claude Code installed and logged in (`claude login`) — the orchestrator reuses your subscription OAuth; no `ANTHROPIC_API_KEY` required

## Build

```bash
./mvnw verify
```

Runs all unit tests across the five modules.

## Run with Docker Compose

Brings up Chroma plus all four MCP servers:

```bash
docker compose up --build
```

Each server exposes `/actuator/health` for readiness checks.

## Run the agent orchestrator

Start the MCP servers first (or `docker compose up -d`), then in a separate terminal:

```bash
cd agent-orchestrator
npm install
npm run dev
```

Open `http://localhost:3000` to access the chat UI. Each answer shows which MCP tools were called (server, tool name, input, and result summary) in a collapsible panel beneath the reply.

## Run a single module locally

```bash
./mvnw spring-boot:run -pl rag-search-mcp
```

For modules that need Chroma, start it independently first:

```bash
docker compose up -d chroma
```

## Project layout

```
spring-ai-demo-devplay-2026/
├── pom.xml                 # parent (Spring AI BOM)
├── docker-compose.yml      # Chroma + 4 MCP servers
├── .env.example            # OPENAI_API_KEY, TAVILY_API_KEY
├── common/                 # shared OpenAI + Chroma deps
├── devplay-info-mcp/       # port 8080
├── rag-search-mcp/         # port 8081
├── chat-history-mcp/       # port 8082
├── web-search-mcp/         # port 8083
├── agent-orchestrator/     # port 3000 — chat UI + REST API (Node/TypeScript)
└── docs/                   # conference slide deck + speaker notes
```
