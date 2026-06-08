# spring-ai-demo-devplay-2026

Multi-module Spring AI MCP server demo for the Dev.Play 2026 conference. Four MCP servers behind SSE, plus a Chroma vector store for RAG and chat-history.

## Modules

| Module | Port | Description |
| --- | --- | --- |
| `devplay-info-mcp` | 8080 | MCP: `getDevPlayEventOverview`, `getDevPlaySchedule` |
| `rag-search-mcp` | 8081 | MCP: `searchKnowledgeBase` (Chroma RAG) |
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

## n8n integration

The four MCP servers can be used as tools inside an n8n AI Agent node via the **MCP Client Tool** node (SSE transport).

### Endpoints

| n8n node name | Endpoint |
| --- | --- |
| DevPlay Schedule Info | `http://devplay-info-mcp:8080/sse` |
| RAG Search | `http://rag-search-mcp:8081/sse` |
| Chat History | `http://chat-history-mcp:8082/sse` |
| Web Search | `http://web-search-mcp:8083/sse` |

### Network bridge

n8n and this stack run in separate Compose networks. Connect the MCP containers to n8n's network with short DNS aliases so the hostnames above resolve:

```bash
N8N_NETWORK=self-hosted-ai-starter-kit_demo   # adjust to your n8n network name

docker network connect --alias devplay-info-mcp $N8N_NETWORK spring-ai-demo-devplay-2026-devplay-info-mcp-1
docker network connect --alias rag-search-mcp  $N8N_NETWORK spring-ai-demo-devplay-2026-rag-search-mcp-1
docker network connect --alias chat-history-mcp $N8N_NETWORK spring-ai-demo-devplay-2026-chat-history-mcp-1
docker network connect --alias web-search-mcp  $N8N_NETWORK spring-ai-demo-devplay-2026-web-search-mcp-1
```

> These connections are ephemeral — re-run after a Docker restart. To make them permanent, add the n8n network to each service in `docker-compose.yml`:
>
> ```yaml
> networks:
>   default:
>   self-hosted-ai-starter-kit_demo:
>     external: true
> ```

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
