# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Multi-module Maven monorepo of four Spring AI MCP servers for the Dev.Play 2026 conference demo. Each module is an independent Spring Boot application exposing tools over SSE transport.

| Module | Port | MCP tools |
| --- | --- | --- |
| `devplay-info-mcp` | 8080 | `DevPlayInformations`, `DevPlaySchedule` (static text) |
| `rag-search-mcp` | 8081 | `searchWiki` (Chroma vector search) |
| `chat-history-mcp` | 8082 | `searchChatHistory`, `recordChatHistory` (Chroma semantic cache) |
| `web-search-mcp` | 8083 | `searchWeb` (Tavily REST API) |

`common/` is a library JAR (no main class) shared by `rag-search-mcp` and `chat-history-mcp` for OpenAI embeddings + Chroma configuration. `web-search-mcp` and `devplay-info-mcp` do not depend on it.

## Prerequisites

- Java 24
- `OPENAI_API_KEY` — used by `rag-search-mcp` and `chat-history-mcp`
- `TAVILY_API_KEY` — used by `web-search-mcp`
- Copy `.env.example` → `.env` and fill in keys before running with Docker Compose

## Commands

```bash
# Build and run all tests
./mvnw verify

# Run a single module locally (Chroma must be running for rag-search-mcp and chat-history-mcp)
./mvnw spring-boot:run -pl rag-search-mcp

# Start Chroma standalone (needed for local runs of rag-search-mcp and chat-history-mcp)
docker compose up -d chroma

# Run tests for a single module
./mvnw test -pl rag-search-mcp

# Full stack via Docker Compose
docker compose up --build
```

## Connecting n8n to the MCP Docker network

n8n runs in a separate Docker Compose project (`self-hosted-ai-starter-kit`) and must be manually joined to this project's network so it can reach the MCP servers by service name.

```bash
# One-time setup — run after starting both stacks
docker network connect spring-ai-demo-devplay-2026_default n8n
```

Once connected, configure each MCP Client Tool node in n8n with:

| MCP Server | Endpoint URL |
| --- | --- |
| devplay-info-mcp | `http://devplay-info-mcp:8080/sse` |
| rag-search-mcp | `http://rag-search-mcp:8081/sse` |
| chat-history-mcp | `http://chat-history-mcp:8082/sse` |
| web-search-mcp | `http://web-search-mcp:8083/sse` |

Use **Server Transport: Server Sent Events (Deprecated)**. The servers use Spring AI's SSE transport (`2024-11-05` protocol); n8n's MCP SDK sends `2025-06-18` but the server gracefully downgrades. **Save the workflow after changing any URL** — n8n's test runs from the saved state, not the current UI.

## Architecture

### MCP tool registration pattern

Every server registers its tools the same way: a `@Configuration` class creates a `List<ToolCallback>` bean by calling `ToolCallbacks.from(toolsBean)`. Spring AI MCP autoconfiguration picks up that bean and exposes the tools over SSE. The `@Tool`-annotated methods on the tools bean define the tool name and description. A server with multiple tool beans aggregates them into one list — see `devplay-info-mcp`'s `McpConfiguration`, which combines `ToolsConfiguration` (`DevPlayInformations`) and `ScheduleTools` (`DevPlaySchedule`).

### RAG ingest (rag-search-mcp)

`WikiIngestor` implements `ApplicationRunner` and runs at startup. It reads all files under `src/main/resources/wiki/`, chunks them with `TokenTextSplitter`, and writes to Chroma with deterministic IDs derived from `SHA-256(file bytes)[0:16]-<chunkIndex>`. `ChunkExistenceCheck` (implemented by `ChromaChunkExistenceCheck`) gates each file so already-ingested content is skipped — embeddings are not re-billed on restart. To add wiki content, drop files into `rag-search-mcp/src/main/resources/wiki/`.

### Vector store collections

- `wiki` — `rag-search-mcp` (documents + metadata: source, score)
- `chat_history` — `chat-history-mcp` (question text as document, answer + timestamp in metadata)

Both use OpenAI `text-embedding-3-small` via the `common` module's autoconfiguration.

### Web search

`TavilyClient` (in `web-search-mcp`) uses `RestTemplate` to POST to the Tavily API. On HTTP 5xx it retries once; any failure surfaces as an MCP tool error.

### Testing approach

Unit tests only (Mockito + AssertJ, no Testcontainers). External services (OpenAI, Tavily, Chroma) are mocked. Integration tests and end-to-end tests across servers are out of scope.

## Conference presentation (`docs/`)

`docs/index.html` is a self-contained slide deck (with `docs/presentation/` SVG diagrams and speaker photos) — the talk that accompanies this demo. Speaker notes are kept in `docs/speaker-notes.md` and synced to/from the HTML with `docs/notes_tool.py`:

```bash
# from docs/ — extract notes out of the HTML into the Markdown file
python3 notes_tool.py extract index.html

# edit speaker-notes.md, then write the notes back into the HTML
python3 notes_tool.py apply index.html speaker-notes.md
```

Edit notes in `speaker-notes.md` and run `apply` rather than hand-editing the `<aside class="notes">` blocks in `index.html`.

<!-- rtk-instructions v2 -->
# RTK (Rust Token Killer) - Token-Optimized Commands

## Golden Rule

**Always prefix commands with `rtk`**. If RTK has a dedicated filter, it uses it. If not, it passes through unchanged. This means RTK is always safe to use.

**Important**: Even in command chains with `&&`, use `rtk`:
```bash
# ❌ Wrong
git add . && git commit -m "msg" && git push

# ✅ Correct
rtk git add . && rtk git commit -m "msg" && rtk git push
```

## RTK Commands by Workflow

### Build & Compile (80-90% savings)
```bash
rtk cargo build         # Cargo build output
rtk cargo check         # Cargo check output
rtk cargo clippy        # Clippy warnings grouped by file (80%)
rtk tsc                 # TypeScript errors grouped by file/code (83%)
rtk lint                # ESLint/Biome violations grouped (84%)
rtk prettier --check    # Files needing format only (70%)
rtk next build          # Next.js build with route metrics (87%)
```

### Test (60-99% savings)
```bash
rtk cargo test          # Cargo test failures only (90%)
rtk go test             # Go test failures only (90%)
rtk jest                # Jest failures only (99.5%)
rtk vitest              # Vitest failures only (99.5%)
rtk playwright test     # Playwright failures only (94%)
rtk pytest              # Python test failures only (90%)
rtk rake test           # Ruby test failures only (90%)
rtk rspec               # RSpec test failures only (60%)
rtk test <cmd>          # Generic test wrapper - failures only
```

### Git (59-80% savings)
```bash
rtk git status          # Compact status
rtk git log             # Compact log (works with all git flags)
rtk git diff            # Compact diff (80%)
rtk git show            # Compact show (80%)
rtk git add             # Ultra-compact confirmations (59%)
rtk git commit          # Ultra-compact confirmations (59%)
rtk git push            # Ultra-compact confirmations
rtk git pull            # Ultra-compact confirmations
rtk git branch          # Compact branch list
rtk git fetch           # Compact fetch
rtk git stash           # Compact stash
rtk git worktree        # Compact worktree
```

Note: Git passthrough works for ALL subcommands, even those not explicitly listed.

### GitHub (26-87% savings)
```bash
rtk gh pr view <num>    # Compact PR view (87%)
rtk gh pr checks        # Compact PR checks (79%)
rtk gh run list         # Compact workflow runs (82%)
rtk gh issue list       # Compact issue list (80%)
rtk gh api              # Compact API responses (26%)
```

### JavaScript/TypeScript Tooling (70-90% savings)
```bash
rtk pnpm list           # Compact dependency tree (70%)
rtk pnpm outdated       # Compact outdated packages (80%)
rtk pnpm install        # Compact install output (90%)
rtk npm run <script>    # Compact npm script output
rtk npx <cmd>           # Compact npx command output
rtk prisma              # Prisma without ASCII art (88%)
```

### Files & Search (60-75% savings)
```bash
rtk ls <path>           # Tree format, compact (65%)
rtk read <file>         # Code reading with filtering (60%)
rtk grep <pattern>      # Search grouped by file (75%). Format flags (-c, -l, -L, -o, -Z) run raw.
rtk find <pattern>      # Find grouped by directory (70%)
```

### Analysis & Debug (70-90% savings)
```bash
rtk err <cmd>           # Filter errors only from any command
rtk log <file>          # Deduplicated logs with counts
rtk json <file>         # JSON structure without values
rtk deps                # Dependency overview
rtk env                 # Environment variables compact
rtk summary <cmd>       # Smart summary of command output
rtk diff                # Ultra-compact diffs
```

### Infrastructure (85% savings)
```bash
rtk docker ps           # Compact container list
rtk docker images       # Compact image list
rtk docker logs <c>     # Deduplicated logs
rtk kubectl get         # Compact resource list
rtk kubectl logs        # Deduplicated pod logs
```

### Network (65-70% savings)
```bash
rtk curl <url>          # Compact HTTP responses (70%)
rtk wget <url>          # Compact download output (65%)
```

### Meta Commands
```bash
rtk gain                # View token savings statistics
rtk gain --history      # View command history with savings
rtk discover            # Analyze Claude Code sessions for missed RTK usage
rtk proxy <cmd>         # Run command without filtering (for debugging)
rtk init                # Add RTK instructions to CLAUDE.md
rtk init --global       # Add RTK to ~/.claude/CLAUDE.md
```

## Token Savings Overview

| Category | Commands | Typical Savings |
|----------|----------|-----------------|
| Tests | vitest, playwright, cargo test | 90-99% |
| Build | next, tsc, lint, prettier | 70-87% |
| Git | status, log, diff, add, commit | 59-80% |
| GitHub | gh pr, gh run, gh issue | 26-87% |
| Package Managers | pnpm, npm, npx | 70-90% |
| Files | ls, read, grep, find | 60-75% |
| Infrastructure | docker, kubectl | 85% |
| Network | curl, wget | 65-70% |

Overall average: **60-90% token reduction** on common development operations.
<!-- /rtk-instructions -->
