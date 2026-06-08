# agent-orchestrator

A host-run TypeScript app (Claude Agent SDK) that chats over the four Dev.Play MCP servers and shows which MCP tools each answer used.

## Why it runs on the host
It reuses your existing **Claude Code login** (subscription OAuth) — no `ANTHROPIC_API_KEY`. The MCP servers stay in Docker; this app reaches them on `localhost`.

## Run
```bash
# 1. Start the MCP servers (from the repo root)
docker compose up -d

# 2. Make sure you're logged into Claude Code
claude login            # if not already

# 3. Start this app
cd agent-orchestrator
cp .env.example .env     # adjust MODEL if you like (sonnet/opus)
npm install
npm run dev              # → http://localhost:4000
```

## Configuration
`.env` (copied from `.env.example`):
- `MODEL` — agent model alias (`sonnet` / `opus` / `haiku`). Default `sonnet`.
- `PORT` — HTTP port. Default `4000`.
- `MAX_TURNS` — max agent tool-call turns per message. Default `12`.
- `DEVPLAY_INFO_MCP_URL`, `RAG_SEARCH_MCP_URL`, `CHAT_HISTORY_MCP_URL`, `WEB_SEARCH_MCP_URL` — override only if you changed the compose ports.

## Endpoints
- `GET  /` — chat UI
- `POST /api/chat` `{message}` → `{reply, tools[]}`
- `POST /api/reset` — start a new conversation
- `GET  /api/health` — MCP server reachability

## Test
```bash
npm test
```
