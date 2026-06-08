# Agent Orchestrator — Design

**Date:** 2026-06-08
**Status:** Approved (brainstorming)

## Overview

A new host-run TypeScript app, `agent-orchestrator/`, that uses the Claude Agent
SDK to drive a chat. It connects to the four Dockerized MCP servers over SSE on
`localhost`, exposes a minimal REST API + single-file chat UI, and after each
answer shows a collapsible list of the MCP tools the agent invoked. Its purpose
is to showcase, for a conference audience, how all the project's MCP servers
connect to a single agent — and to make the tool calls behind each answer
visible.

It runs **on the host** (not in Docker) and reuses the presenter's **Claude Code
login** (subscription OAuth) — no `ANTHROPIC_API_KEY`. The MCP servers stay in
Docker and are reached at `localhost:8080–8083`.

## Key decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Agent engine | Claude Agent SDK (`@anthropic-ai/claude-agent-sdk`), TypeScript | Native MCP support + structured tool-call events |
| Run location | Host (`npm run dev`), not Docker | Lets the SDK reuse host Claude Code OAuth automatically |
| Auth | Claude Code subscription OAuth (no API key) | Reuses the presenter's existing login; no new credential |
| Tool tracking UX | Summary after each answer (collapsible "tools used") | Simple request/response REST; no streaming needed |
| UI stack | Plain HTML/CSS/JS, single `index.html` | Zero build step, readable on stage, "minimal" |
| Agent tools | Restricted to the 4 MCP servers' tools only | Built-in Bash/Read/Edit disabled → pure conference assistant; tools panel only shows MCP calls |

## Components

```
agent-orchestrator/
├── package.json          # deps: @anthropic-ai/claude-agent-sdk, express
│                         # dev:  tsx, typescript, @types/{express,node}, vitest
├── tsconfig.json
├── .env.example          # MODEL, MCP URLs (localhost defaults), PORT
├── src/
│   ├── server.ts         # Express: serves UI + REST endpoints
│   ├── agent.ts          # wraps SDK query(), runs a turn, returns {reply, tools}
│   ├── mcp-config.ts     # the 4 SSE MCP server definitions
│   └── parse.ts          # SDK message stream → {reply, tools[]}  (unit-tested)
├── public/
│   └── index.html        # vanilla-JS chat + "tools used" panel, zero build
└── README.md
```

## Data flow (one chat turn)

1. Browser `POST /api/chat {message}`.
2. `server.ts` appends to an in-memory conversation, calls `agent.run(history)`.
3. `agent.ts` calls the Agent SDK `query()` with: the conversation, the four MCP
   servers config, and `allowedTools` limited to the MCP tools (built-in
   Bash/Read/Edit disabled) plus a system prompt defining the "Dev.Play
   conference assistant" role.
4. The SDK streams structured messages. `parse.ts` collects the final assistant
   text as `reply`, and every `tool_use` block (name + input) paired with its
   `tool_result` (success/error + short result) into `tools[]`. MCP tools are
   namespaced `mcp__<server>__<tool>`, so each call is labelled with its server.
5. Respond `{ reply, tools: [{ server, tool, input, resultSummary, isError }] }`.
6. UI renders the answer bubble + `▾ tools used (N)`.

### Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/` | Serve the chat UI |
| POST | `/api/chat` | `{message}` → `{reply, tools}` |
| POST | `/api/reset` | Clear the in-memory conversation (new chat) |
| GET | `/api/health` | Ping the 4 SSE endpoints; report which are up |

## MCP connection

The four servers are configured as SSE MCP servers, URLs from env with
`localhost` defaults:

| Server | Default URL | Tools |
| --- | --- | --- |
| devplay-info-mcp | `http://localhost:8080/sse` | `DevPlayInformations`, `DevPlaySchedule` |
| rag-search-mcp | `http://localhost:8081/sse` | `searchWiki` |
| chat-history-mcp | `http://localhost:8082/sse` | `searchChatHistory`, `recordChatHistory` |
| web-search-mcp | `http://localhost:8083/sse` | `searchWeb` |

## Auth & prerequisites

No `ANTHROPIC_API_KEY`. The SDK picks up the host Claude Code OAuth
automatically. Model is env-configurable. Demo run order:

1. `docker compose up` (starts Chroma + the 4 MCP servers)
2. Ensure logged into Claude Code (`claude login`)
3. `npm run dev` in `agent-orchestrator/`

## Error handling

- **MCP down/unreachable:** surfaces as a `tool_result` error → shown red in the
  tools panel; the agent explains it. `/api/health` warns up front.
- **Auth failure (not logged in):** `500` with a clear "run `claude login`" hint,
  shown as an error bubble in the UI.
- No streaming, so a multi-tool turn just takes a few seconds — generous timeout,
  no partial-state complexity.

## Testing

- **One unit test** on `parse.ts`: feed a recorded SDK message array, assert
  `reply` + `tools[]` (including server mapping and an error case). No live API
  calls — matches the repo's mock-only testing approach.
- **Manual:** ask questions that route to each server (schedule →
  `DevPlaySchedule`, wiki → `searchWiki`, web → `searchWeb`, history →
  `searchChatHistory`).

## Out of scope (YAGNI)

No Docker image, no DB/persistence, no multi-user sessions, no streaming, no
React/build step.

## Next steps

1. Mockup of the REST API + chat UI (separate deliverable).
2. Implementation plan via the writing-plans skill.
