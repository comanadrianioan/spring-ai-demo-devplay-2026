# Agent Orchestrator Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build `agent-orchestrator/` — a host-run TypeScript app using the Claude Agent SDK that connects to the four Dockerized MCP servers over SSE, exposes a minimal REST API + single-file chat UI, and shows which MCP tools were invoked for each answer.

**Architecture:** A small Express server runs the Claude Agent SDK's `query()` per chat turn. The agent is restricted to the four MCP servers' tools (built-ins disabled). A pure reducer turns the SDK message stream into `{ reply, tools[] }`. Multi-turn memory is the SDK's `resume: <session_id>`. The UI is one static `index.html` (vanilla JS). The app runs on the host and reuses the presenter's Claude Code login (no `ANTHROPIC_API_KEY`); the MCP servers stay in Docker on `localhost:8080–8083`.

**Tech Stack:** Node 20+, TypeScript (ESM), `tsx` (no build step), Express 5, `@anthropic-ai/claude-agent-sdk`, Vitest + supertest for tests.

**Design reference:** `docs/plans/2026-06-08-agent-orchestrator-design.md`. UI reference: `docs/plans/agent-orchestrator-mockup.html`.

---

## Conventions & notes for the executor

- **Branch:** All work happens on a dedicated branch `agent-orchestrator` (Task 0). The original request was explicitly "on a separate branch."
- **Git:** The repo owner manages git on this project. Commit steps are included per TDD convention and are fine **on this feature branch**, but if running unattended, batch commits and let the owner review/squash. Do not push or open a PR without being asked.
- **Module style:** ESM (`"type": "module"`), `moduleResolution: "bundler"`, **extensionless** local imports (`from "./parse"`). `tsx` runs TS directly.
- **Auth:** Never set `ANTHROPIC_API_KEY`. The SDK uses the host Claude Code credentials automatically.
- **Tool naming:** MCP server keys in config are kept identical to the compose service names (`devplay-info-mcp`, etc.), so tool full names are `mcp__devplay-info-mcp__DevPlaySchedule` and wildcards are `mcp__devplay-info-mcp__*`.
- **Prereqs to run live:** `docker compose up` (Chroma + 4 MCP servers) and a logged-in Claude Code (`claude login`).

---

## Task 0: Create the feature branch

**Step 1: Branch from main**

Run:
```bash
git checkout -b agent-orchestrator
```
Expected: `Switched to a new branch 'agent-orchestrator'`.

No commit.

---

## Task 1: Scaffold the project

**Files:**
- Create: `agent-orchestrator/package.json`
- Create: `agent-orchestrator/tsconfig.json`
- Create: `agent-orchestrator/.gitignore`
- Create: `agent-orchestrator/.env.example`
- Create dirs: `agent-orchestrator/src/`, `agent-orchestrator/public/`, `agent-orchestrator/tests/`

**Step 1: Create `agent-orchestrator/package.json`**

```json
{
  "name": "agent-orchestrator",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "tsx watch src/server.ts",
    "start": "tsx src/server.ts",
    "test": "vitest run",
    "typecheck": "tsc --noEmit"
  }
}
```

**Step 2: Install dependencies (lets npm resolve correct versions — do not hand-pin)**

Run:
```bash
cd agent-orchestrator
npm install @anthropic-ai/claude-agent-sdk express
npm install -D typescript tsx vitest supertest @types/node @types/express @types/supertest
```
Expected: `node_modules/` populated, versions written into `package.json`.

**Step 3: Create `agent-orchestrator/tsconfig.json`**

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "lib": ["ES2022", "DOM"],
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "resolveJsonModule": true,
    "types": ["node"],
    "outDir": "dist"
  },
  "include": ["src", "tests"]
}
```

**Step 4: Create `agent-orchestrator/.gitignore`**

```gitignore
node_modules/
dist/
.env
```

**Step 5: Create `agent-orchestrator/.env.example`**

```bash
# The agent's model. Aliases "sonnet" / "opus" / "haiku" work with Claude Code auth.
MODEL=sonnet

# HTTP port for this app.
PORT=4000

# Max agent turns per chat message (tool-call loops).
MAX_TURNS=12

# MCP server URLs (defaults match docker-compose port mappings on localhost).
# Override only if you changed the compose ports.
DEVPLAY_INFO_MCP_URL=http://localhost:8080/sse
RAG_SEARCH_MCP_URL=http://localhost:8081/sse
CHAT_HISTORY_MCP_URL=http://localhost:8082/sse
WEB_SEARCH_MCP_URL=http://localhost:8083/sse
```

**Step 6: Commit**

```bash
git add agent-orchestrator/package.json agent-orchestrator/package-lock.json \
        agent-orchestrator/tsconfig.json agent-orchestrator/.gitignore \
        agent-orchestrator/.env.example
git commit -m "chore(agent-orchestrator): scaffold TS project"
```

---

## Task 2: MCP config module (TDD)

Builds the `mcpServers` map, `allowedTools` wildcards, and the disallowed built-ins list from env, with `localhost` defaults.

**Files:**
- Create: `agent-orchestrator/src/mcp-config.ts`
- Test: `agent-orchestrator/tests/mcp-config.test.ts`

**Step 1: Write the failing test**

`agent-orchestrator/tests/mcp-config.test.ts`:
```ts
import { describe, it, expect } from "vitest";
import { serverDefs, envVarFor, mcpServers, allowedTools } from "../src/mcp-config";

describe("mcp-config", () => {
  it("derives the env var name from a server key", () => {
    expect(envVarFor("devplay-info-mcp")).toBe("DEVPLAY_INFO_MCP_URL");
  });

  it("uses localhost defaults when env is empty", () => {
    const defs = serverDefs({});
    expect(defs).toHaveLength(4);
    expect(defs[0]).toEqual({ key: "devplay-info-mcp", url: "http://localhost:8080/sse" });
  });

  it("overrides a url from env", () => {
    const defs = serverDefs({ WEB_SEARCH_MCP_URL: "http://web:8083/sse" });
    expect(defs.find((d) => d.key === "web-search-mcp")!.url).toBe("http://web:8083/sse");
  });

  it("builds an SSE mcpServers map keyed by service name", () => {
    const map = mcpServers({});
    expect(map["rag-search-mcp"]).toEqual({ type: "sse", url: "http://localhost:8081/sse" });
  });

  it("builds wildcard allowedTools per server", () => {
    expect(allowedTools({})).toEqual([
      "mcp__devplay-info-mcp__*",
      "mcp__rag-search-mcp__*",
      "mcp__chat-history-mcp__*",
      "mcp__web-search-mcp__*",
    ]);
  });
});
```

**Step 2: Run test to verify it fails**

Run: `cd agent-orchestrator && npx vitest run tests/mcp-config.test.ts`
Expected: FAIL — `Cannot find module '../src/mcp-config'`.

**Step 3: Write the implementation**

`agent-orchestrator/src/mcp-config.ts`:
```ts
export interface ServerDef {
  key: string;
  url: string;
}

export interface McpSseConfig {
  type: "sse";
  url: string;
}

const DEFAULT_SERVERS: ServerDef[] = [
  { key: "devplay-info-mcp", url: "http://localhost:8080/sse" },
  { key: "rag-search-mcp", url: "http://localhost:8081/sse" },
  { key: "chat-history-mcp", url: "http://localhost:8082/sse" },
  { key: "web-search-mcp", url: "http://localhost:8083/sse" },
];

/** "devplay-info-mcp" -> "DEVPLAY_INFO_MCP_URL" */
export function envVarFor(key: string): string {
  return key.toUpperCase().replace(/-/g, "_") + "_URL";
}

export function serverDefs(env: Record<string, string | undefined> = process.env): ServerDef[] {
  return DEFAULT_SERVERS.map((s) => ({ key: s.key, url: env[envVarFor(s.key)] ?? s.url }));
}

export function mcpServers(env?: Record<string, string | undefined>): Record<string, McpSseConfig> {
  const out: Record<string, McpSseConfig> = {};
  for (const s of serverDefs(env)) out[s.key] = { type: "sse", url: s.url };
  return out;
}

export function allowedTools(env?: Record<string, string | undefined>): string[] {
  return serverDefs(env).map((s) => `mcp__${s.key}__*`);
}

/** Hard-disable the SDK's built-in tools so the agent only uses MCP tools. */
export const DISALLOWED_BUILTIN_TOOLS: string[] = [
  "Bash", "BashOutput", "KillShell", "Read", "Write", "Edit", "MultiEdit",
  "NotebookEdit", "Glob", "Grep", "WebFetch", "WebSearch", "Task", "TodoWrite",
];
```

**Step 4: Run test to verify it passes**

Run: `npx vitest run tests/mcp-config.test.ts`
Expected: PASS (5 tests).

**Step 5: Commit**

```bash
git add agent-orchestrator/src/mcp-config.ts agent-orchestrator/tests/mcp-config.test.ts
git commit -m "feat(agent-orchestrator): MCP server config from env"
```

---

## Task 3: Message reducer `parse.ts` (TDD — the core)

Turns a collected array of SDK messages into `{ reply, tools, sessionId, error }`. Pure and fully unit-tested.

**Files:**
- Create: `agent-orchestrator/src/parse.ts`
- Test: `agent-orchestrator/tests/parse.test.ts`

**Step 1: Write the failing test**

`agent-orchestrator/tests/parse.test.ts`:
```ts
import { describe, it, expect } from "vitest";
import { reduceMessages, splitToolName, summarizeResult } from "../src/parse";

// Minimal fakes shaped like real SDK messages (see agent-sdk typescript reference).
const happyPath = [
  { type: "system", subtype: "init", session_id: "sess-1", mcp_servers: [] },
  {
    type: "assistant",
    session_id: "sess-1",
    message: {
      content: [
        { type: "text", text: "Let me check." },
        { type: "tool_use", id: "t1", name: "mcp__devplay-info-mcp__DevPlaySchedule", input: {} },
        { type: "tool_use", id: "t2", name: "mcp__rag-search-mcp__searchWiki", input: { query: "keynote" } },
      ],
    },
  },
  {
    type: "user",
    session_id: "sess-1",
    message: {
      content: [
        { type: "tool_result", tool_use_id: "t1", content: "Day 1 09:00 — Keynote (Main Hall)" },
        { type: "tool_result", tool_use_id: "t2", content: [{ type: "text", text: "3 chunks (top 0.82)" }] },
      ],
    },
  },
  { type: "result", subtype: "success", session_id: "sess-1", result: "The keynote is at 9am." },
];

describe("reduceMessages", () => {
  it("extracts the final reply", () => {
    expect(reduceMessages(happyPath).reply).toBe("The keynote is at 9am.");
  });

  it("extracts MCP tool calls in order with server/tool labels", () => {
    const { tools } = reduceMessages(happyPath);
    expect(tools.map((t) => `${t.server}/${t.tool}`)).toEqual([
      "devplay-info-mcp/DevPlaySchedule",
      "rag-search-mcp/searchWiki",
    ]);
    expect(tools[1].input).toEqual({ query: "keynote" });
    expect(tools[0].resultSummary).toBe("Day 1 09:00 — Keynote (Main Hall)");
    expect(tools[1].resultSummary).toBe("3 chunks (top 0.82)");
    expect(tools.every((t) => t.isError === false)).toBe(true);
  });

  it("captures the session id", () => {
    expect(reduceMessages(happyPath).sessionId).toBe("sess-1");
  });

  it("marks failed tool results as errors", () => {
    const withError = [
      {
        type: "assistant",
        message: { content: [{ type: "tool_use", id: "e1", name: "mcp__web-search-mcp__searchWeb", input: { query: "x" } }] },
      },
      {
        type: "user",
        message: { content: [{ type: "tool_result", tool_use_id: "e1", is_error: true, content: "error: ECONNREFUSED" }] },
      },
      { type: "result", subtype: "success", session_id: "s", result: "couldn't reach web search" },
    ];
    const { tools } = reduceMessages(withError);
    expect(tools[0].isError).toBe(true);
    expect(tools[0].resultSummary).toContain("ECONNREFUSED");
  });

  it("surfaces an execution error result", () => {
    const errored = [{ type: "result", subtype: "error_during_execution", errors: ["boom"] }];
    expect(reduceMessages(errored).error).toBe("boom");
  });

  it("ignores non-MCP (built-in) tool_use blocks", () => {
    const withBuiltin = [
      { type: "assistant", message: { content: [{ type: "tool_use", id: "b1", name: "Bash", input: { cmd: "ls" } }] } },
      { type: "result", subtype: "success", result: "done" },
    ];
    expect(reduceMessages(withBuiltin).tools).toHaveLength(0);
  });
});

describe("helpers", () => {
  it("splits an mcp tool name", () => {
    expect(splitToolName("mcp__web-search-mcp__searchWeb")).toEqual({ server: "web-search-mcp", tool: "searchWeb" });
  });
  it("summarizes and truncates long results", () => {
    expect(summarizeResult("x".repeat(300)).endsWith("…")).toBe(true);
    expect(summarizeResult([{ type: "text", text: "hi" }])).toBe("hi");
  });
});
```

**Step 2: Run test to verify it fails**

Run: `npx vitest run tests/parse.test.ts`
Expected: FAIL — `Cannot find module '../src/parse'`.

**Step 3: Write the implementation**

`agent-orchestrator/src/parse.ts`:
```ts
export interface ToolCall {
  id: string;
  server: string;
  tool: string;
  input: unknown;
  resultSummary: string;
  isError: boolean;
}

export interface TurnResult {
  reply: string;
  tools: ToolCall[];
  sessionId: string | null;
  error: string | null;
}

/** "mcp__web-search-mcp__searchWeb" -> { server, tool } */
export function splitToolName(name: string): { server: string; tool: string } {
  const parts = name.split("__"); // ["mcp", "<server>", "<tool...>"]
  return { server: parts[1] ?? "", tool: parts.slice(2).join("__") };
}

export function summarizeResult(content: unknown, max = 200): string {
  let text = "";
  if (typeof content === "string") {
    text = content;
  } else if (Array.isArray(content)) {
    text = content
      .map((b) => (typeof b === "string" ? b : b && typeof b === "object" && (b as any).type === "text" ? (b as any).text : ""))
      .join(" ");
  }
  text = text.replace(/\s+/g, " ").trim();
  return text.length > max ? text.slice(0, max) + "…" : text;
}

function asBlocks(content: unknown): any[] {
  return Array.isArray(content) ? content : [];
}

/** Reduce a collected SDK message array into the turn's reply + MCP tool trace. */
export function reduceMessages(messages: any[]): TurnResult {
  const byId = new Map<string, ToolCall>();
  const order: string[] = [];
  let reply = "";
  let sessionId: string | null = null;
  let error: string | null = null;

  for (const m of messages) {
    if (m?.session_id) sessionId = m.session_id;

    if (m?.type === "assistant") {
      for (const block of asBlocks(m.message?.content)) {
        if (block?.type === "tool_use" && typeof block.name === "string" && block.name.startsWith("mcp__")) {
          const { server, tool } = splitToolName(block.name);
          byId.set(block.id, { id: block.id, server, tool, input: block.input, resultSummary: "", isError: false });
          order.push(block.id);
        }
      }
    }

    if (m?.type === "user") {
      for (const block of asBlocks(m.message?.content)) {
        if (block?.type === "tool_result") {
          const call = byId.get(block.tool_use_id);
          if (call) {
            call.resultSummary = summarizeResult(block.content);
            call.isError = block.is_error === true;
          }
        }
      }
    }

    if (m?.type === "result") {
      if (m.subtype === "success") reply = m.result ?? "";
      else error = (Array.isArray(m.errors) ? m.errors.join("; ") : "") || m.subtype || "error";
    }
  }

  return { reply, tools: order.map((id) => byId.get(id)!), sessionId, error };
}
```

**Step 4: Run test to verify it passes**

Run: `npx vitest run tests/parse.test.ts`
Expected: PASS (all cases).

**Step 5: Commit**

```bash
git add agent-orchestrator/src/parse.ts agent-orchestrator/tests/parse.test.ts
git commit -m "feat(agent-orchestrator): reduce SDK messages to reply + tool trace"
```

---

## Task 4: Agent runner `agent.ts` (TDD with injected query)

Wraps `query()`, collects the stream, and returns the reduced `TurnResult`. The SDK `query` is injectable so the test never hits the network.

**Files:**
- Create: `agent-orchestrator/src/agent.ts`
- Test: `agent-orchestrator/tests/agent.test.ts`

**Step 1: Write the failing test**

`agent-orchestrator/tests/agent.test.ts`:
```ts
import { describe, it, expect, vi } from "vitest";
import { runTurn } from "../src/agent";

function fakeStream(messages: any[]) {
  return (async function* () {
    for (const m of messages) yield m;
  })();
}

describe("runTurn", () => {
  it("passes options through and returns the reduced result", async () => {
    const queryImpl = vi.fn(() =>
      fakeStream([
        {
          type: "assistant",
          session_id: "s1",
          message: { content: [{ type: "tool_use", id: "t1", name: "mcp__web-search-mcp__searchWeb", input: { query: "q" } }] },
        },
        { type: "user", message: { content: [{ type: "tool_result", tool_use_id: "t1", content: "ok" }] } },
        { type: "result", subtype: "success", session_id: "s1", result: "answer" },
      ]),
    );

    const res = await runTurn("hello", null, { env: {}, queryImpl: queryImpl as any });

    expect(res.reply).toBe("answer");
    expect(res.sessionId).toBe("s1");
    expect(res.tools[0].tool).toBe("searchWeb");

    const opts = queryImpl.mock.calls[0][0].options;
    expect(opts.allowedTools).toContain("mcp__web-search-mcp__*");
    expect(opts.disallowedTools).toContain("Bash");
    expect(opts.mcpServers["devplay-info-mcp"].type).toBe("sse");
    expect(opts.resume).toBeUndefined();
  });

  it("passes resume when a session id is supplied", async () => {
    const queryImpl = vi.fn(() => fakeStream([{ type: "result", subtype: "success", session_id: "s2", result: "ok" }]));
    await runTurn("again", "s1", { env: {}, queryImpl: queryImpl as any });
    expect(queryImpl.mock.calls[0][0].options.resume).toBe("s1");
  });
});
```

**Step 2: Run test to verify it fails**

Run: `npx vitest run tests/agent.test.ts`
Expected: FAIL — `Cannot find module '../src/agent'`.

**Step 3: Write the implementation**

`agent-orchestrator/src/agent.ts`:
```ts
import { query } from "@anthropic-ai/claude-agent-sdk";
import { mcpServers, allowedTools, DISALLOWED_BUILTIN_TOOLS } from "./mcp-config";
import { reduceMessages, type TurnResult } from "./parse";

export const SYSTEM_PROMPT = `You are the Dev.Play 2026 conference assistant.
Answer attendee questions using ONLY the connected MCP tools:
- devplay-info-mcp: conference information and the schedule
- rag-search-mcp: searchWiki over the conference wiki
- chat-history-mcp: searchChatHistory / recordChatHistory (semantic Q&A cache)
- web-search-mcp: searchWeb for general/current info

Prefer the conference-specific tools first. Use web search only for general or
current information that is not in the conference data. Be concise. If a tool
fails, tell the user which service was unavailable rather than guessing.`;

export interface RunOptions {
  env?: Record<string, string | undefined>;
  /** Injectable for tests; defaults to the real SDK query(). */
  queryImpl?: typeof query;
}

export async function runTurn(
  message: string,
  sessionId: string | null,
  opts: RunOptions = {},
): Promise<TurnResult> {
  const env = opts.env ?? process.env;
  const q = opts.queryImpl ?? query;

  const response = q({
    prompt: message,
    options: {
      model: env.MODEL || "sonnet",
      systemPrompt: SYSTEM_PROMPT,
      mcpServers: mcpServers(env),
      allowedTools: allowedTools(env),
      disallowedTools: DISALLOWED_BUILTIN_TOOLS,
      settingSources: [], // don't inherit repo CLAUDE.md / .mcp.json
      maxTurns: Number(env.MAX_TURNS ?? 12),
      ...(sessionId ? { resume: sessionId } : {}),
    },
  });

  const collected: any[] = [];
  for await (const m of response) collected.push(m);
  return reduceMessages(collected);
}
```

**Step 4: Run test to verify it passes**

Run: `npx vitest run tests/agent.test.ts`
Expected: PASS (2 tests).

**Step 5: Commit**

```bash
git add agent-orchestrator/src/agent.ts agent-orchestrator/tests/agent.test.ts
git commit -m "feat(agent-orchestrator): agent runner over Claude Agent SDK"
```

---

## Task 5: Health helper `health.ts` (TDD)

Reachability ping for the SSE endpoints + an auth-aware error hint.

**Files:**
- Create: `agent-orchestrator/src/health.ts`
- Test: `agent-orchestrator/tests/health.test.ts`

**Step 1: Write the failing test**

`agent-orchestrator/tests/health.test.ts`:
```ts
import { describe, it, expect, vi, afterEach } from "vitest";
import { pingSse, authHint } from "../src/health";

afterEach(() => vi.restoreAllMocks());

describe("pingSse", () => {
  it("returns true when the endpoint responds", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => ({ ok: true, status: 200, body: { cancel: () => {} } })) as any);
    expect(await pingSse("http://localhost:8080/sse")).toBe(true);
  });

  it("returns false when fetch throws (connection refused)", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => { throw new Error("ECONNREFUSED"); }) as any);
    expect(await pingSse("http://localhost:8080/sse")).toBe(false);
  });
});

describe("authHint", () => {
  it("adds a claude login hint for auth errors", () => {
    expect(authHint(new Error("authentication_failed"))).toMatch(/claude login/);
  });
  it("passes other errors through unchanged", () => {
    expect(authHint(new Error("kaboom"))).toBe("kaboom");
  });
});
```

**Step 2: Run test to verify it fails**

Run: `npx vitest run tests/health.test.ts`
Expected: FAIL — `Cannot find module '../src/health'`.

**Step 3: Write the implementation**

`agent-orchestrator/src/health.ts`:
```ts
export async function pingSse(url: string, timeoutMs = 1500): Promise<boolean> {
  try {
    const res = await fetch(url, {
      headers: { Accept: "text/event-stream" },
      signal: AbortSignal.timeout(timeoutMs),
    });
    // SSE keeps the body open; we only needed the headers. Cancel the stream.
    try { await (res as any).body?.cancel?.(); } catch { /* ignore */ }
    return res.ok || res.status === 400 || res.status === 405;
  } catch {
    return false;
  }
}

export function authHint(e: unknown): string {
  const msg = e instanceof Error ? e.message : String(e);
  if (/auth|login|credential|unauthor/i.test(msg)) {
    return `${msg} — make sure you're logged into Claude Code (run: claude login).`;
  }
  return msg;
}
```

**Step 4: Run test to verify it passes**

Run: `npx vitest run tests/health.test.ts`
Expected: PASS (4 tests).

**Step 5: Commit**

```bash
git add agent-orchestrator/src/health.ts agent-orchestrator/tests/health.test.ts
git commit -m "feat(agent-orchestrator): SSE health ping + auth hint"
```

---

## Task 6: Express app `app.ts` (TDD with supertest)

The REST surface. `createApp()` takes injectable deps so the test never runs the real agent.

**Files:**
- Create: `agent-orchestrator/src/app.ts`
- Test: `agent-orchestrator/tests/app.test.ts`

**Step 1: Write the failing test**

`agent-orchestrator/tests/app.test.ts`:
```ts
import { describe, it, expect, vi } from "vitest";
import request from "supertest";
import { createApp } from "../src/app";

function appWith(runTurn: any) {
  return createApp({
    runTurn,
    serverDefs: () => [{ key: "web-search-mcp", url: "http://localhost:8083/sse" }],
    pingSse: async () => true,
  });
}

describe("POST /api/chat", () => {
  it("returns reply + tools and remembers the session", async () => {
    const runTurn = vi
      .fn()
      .mockResolvedValueOnce({ reply: "hi", tools: [{ tool: "searchWeb" }], sessionId: "s1", error: null })
      .mockResolvedValueOnce({ reply: "again", tools: [], sessionId: "s1", error: null });
    const app = appWith(runTurn);

    const r1 = await request(app).post("/api/chat").send({ message: "hello" });
    expect(r1.status).toBe(200);
    expect(r1.body).toEqual({ reply: "hi", tools: [{ tool: "searchWeb" }] });
    expect(runTurn.mock.calls[0][1]).toBeNull(); // first call: no session

    await request(app).post("/api/chat").send({ message: "more" });
    expect(runTurn.mock.calls[1][1]).toBe("s1"); // second call resumes
  });

  it("rejects an empty message", async () => {
    const app = appWith(vi.fn());
    const r = await request(app).post("/api/chat").send({ message: "  " });
    expect(r.status).toBe(400);
  });

  it("maps a 500 with an auth hint when the agent throws", async () => {
    const app = appWith(vi.fn().mockRejectedValue(new Error("authentication_failed")));
    const r = await request(app).post("/api/chat").send({ message: "x" });
    expect(r.status).toBe(500);
    expect(r.body.error).toMatch(/claude login/);
  });
});

describe("POST /api/reset", () => {
  it("clears the session", async () => {
    const runTurn = vi.fn().mockResolvedValue({ reply: "x", tools: [], sessionId: "s9", error: null });
    const app = appWith(runTurn);
    await request(app).post("/api/chat").send({ message: "a" });
    await request(app).post("/api/reset");
    await request(app).post("/api/chat").send({ message: "b" });
    expect(runTurn.mock.calls[1][1]).toBeNull(); // reset cleared the session
  });
});

describe("GET /api/health", () => {
  it("reports server reachability", async () => {
    const app = appWith(vi.fn());
    const r = await request(app).get("/api/health");
    expect(r.status).toBe(200);
    expect(r.body.ok).toBe(true);
    expect(r.body.servers[0]).toEqual({ name: "web-search-mcp", url: "http://localhost:8083/sse", up: true });
  });
});
```

**Step 2: Run test to verify it fails**

Run: `npx vitest run tests/app.test.ts`
Expected: FAIL — `Cannot find module '../src/app'`.

**Step 3: Write the implementation**

`agent-orchestrator/src/app.ts`:
```ts
import express, { type Express } from "express";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { runTurn as defaultRunTurn } from "./agent";
import { serverDefs as defaultServerDefs } from "./mcp-config";
import { pingSse as defaultPingSse, authHint } from "./health";

const here = path.dirname(fileURLToPath(import.meta.url));

export interface AppDeps {
  runTurn?: typeof defaultRunTurn;
  serverDefs?: typeof defaultServerDefs;
  pingSse?: typeof defaultPingSse;
}

export function createApp(deps: AppDeps = {}): Express {
  const runTurn = deps.runTurn ?? defaultRunTurn;
  const serverDefs = deps.serverDefs ?? defaultServerDefs;
  const pingSse = deps.pingSse ?? defaultPingSse;

  const app = express();
  app.use(express.json());

  // One shared in-memory conversation (single-presenter demo).
  let sessionId: string | null = null;

  app.post("/api/chat", async (req, res) => {
    const message = String(req.body?.message ?? "").trim();
    if (!message) return res.status(400).json({ error: "message is required" });
    try {
      const result = await runTurn(message, sessionId);
      if (result.error) return res.status(502).json({ error: result.error, tools: result.tools });
      sessionId = result.sessionId ?? sessionId;
      return res.json({ reply: result.reply, tools: result.tools });
    } catch (e) {
      return res.status(500).json({ error: authHint(e) });
    }
  });

  app.post("/api/reset", (_req, res) => {
    sessionId = null;
    res.json({ ok: true });
  });

  app.get("/api/health", async (_req, res) => {
    const servers = await Promise.all(
      serverDefs().map(async (s) => ({ name: s.key, url: s.url, up: await pingSse(s.url) })),
    );
    res.json({ ok: servers.every((s) => s.up), servers });
  });

  app.use(express.static(path.join(here, "..", "public")));
  return app;
}
```

**Step 4: Run test to verify it passes**

Run: `npx vitest run tests/app.test.ts`
Expected: PASS (all cases).

**Step 5: Commit**

```bash
git add agent-orchestrator/src/app.ts agent-orchestrator/tests/app.test.ts
git commit -m "feat(agent-orchestrator): REST API (chat/reset/health)"
```

---

## Task 7: Server entrypoint `server.ts`

Thin launcher: builds the app and listens. No test (just wiring).

**Files:**
- Create: `agent-orchestrator/src/server.ts`

**Step 1: Write it**

`agent-orchestrator/src/server.ts`:
```ts
import { createApp } from "./app";
import { serverDefs } from "./mcp-config";
import { pingSse } from "./health";

const port = Number(process.env.PORT ?? 4000);
const app = createApp();

app.listen(port, async () => {
  console.log(`agent-orchestrator → http://localhost:${port}`);
  // Friendly startup check so a down MCP server is obvious on stage.
  for (const s of serverDefs()) {
    const up = await pingSse(s.url);
    console.log(`  [${up ? "ok" : "DOWN"}] ${s.key} ${s.url}`);
  }
});
```

**Step 2: Verify it boots (manual)**

Run: `cd agent-orchestrator && npm run dev`
Expected: logs the URL and a status line per MCP server (DOWN is fine here if compose isn't up yet). Stop with Ctrl-C.

**Step 3: Commit**

```bash
git add agent-orchestrator/src/server.ts
git commit -m "feat(agent-orchestrator): server entrypoint with startup health log"
```

---

## Task 8: Chat UI `public/index.html`

Single static file, vanilla JS. Styling mirrors `docs/plans/agent-orchestrator-mockup.html`; this version is wired to the real API.

**Files:**
- Create: `agent-orchestrator/public/index.html`

**Step 1: Write it**

`agent-orchestrator/public/index.html`:
```html
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>Dev.Play Agent — MCP tool tracer</title>
<style>
  :root {
    --bg:#0f1115; --panel:#171a21; --line:#262b36; --text:#e6e9ef; --muted:#9aa3b2;
    --accent:#4f9cf9; --ok:#3fb950; --err:#f85149; --user-bg:#1d2734; --bot-bg:#161b22;
  }
  * { box-sizing:border-box; }
  body { margin:0; background:var(--bg); color:var(--text);
    font:15px/1.5 -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;
    display:flex; justify-content:center; }
  .app { width:100%; max-width:760px; height:100vh; display:flex; flex-direction:column;
    border-left:1px solid var(--line); border-right:1px solid var(--line); }
  header { display:flex; align-items:center; justify-content:space-between; padding:12px 16px;
    border-bottom:1px solid var(--line); background:var(--panel); }
  header .title { font-weight:600; } header .title span { color:var(--muted); font-weight:400; }
  .newchat { background:transparent; color:var(--text); border:1px solid var(--line);
    border-radius:8px; padding:6px 12px; cursor:pointer; font-size:13px; }
  .newchat:hover { border-color:var(--accent); }
  .chat { flex:1; overflow-y:auto; padding:20px 16px; display:flex; flex-direction:column; gap:16px; }
  .row { display:flex; } .row.user { justify-content:flex-end; }
  .bubble { max-width:80%; padding:10px 14px; border-radius:14px; white-space:pre-wrap; }
  .user .bubble { background:var(--user-bg); border:1px solid var(--line); }
  .bot .bubble { background:var(--bot-bg); border:1px solid var(--line); }
  .bot.err .bubble { border-color:var(--err); }
  .tools { margin-top:10px; border:1px solid var(--line); border-radius:10px; background:#11151b; font-size:13px; }
  .tools > summary { cursor:pointer; padding:8px 12px; color:var(--muted); user-select:none; list-style:none; }
  .tools > summary::-webkit-details-marker { display:none; }
  .tools > summary::before { content:"▸ "; } .tools[open] > summary::before { content:"▾ "; }
  .call { padding:8px 12px 12px; border-top:1px solid var(--line); }
  .call .name { display:flex; align-items:center; gap:8px; font-weight:600; }
  .dot { width:8px; height:8px; border-radius:50%; background:var(--ok); flex:none; } .dot.err { background:var(--err); }
  .call .server { color:var(--muted); font-weight:400; }
  .kv { margin:4px 0 0 16px; color:var(--muted); word-break:break-word; }
  .kv code { color:var(--text); background:#0c0f14; padding:1px 5px; border-radius:5px; } .kv.err code { color:var(--err); }
  footer { padding:12px 16px; border-top:1px solid var(--line); background:var(--panel); display:flex; gap:8px; }
  footer input { flex:1; background:#0c0f14; border:1px solid var(--line); color:var(--text);
    border-radius:10px; padding:10px 12px; font-size:15px; }
  footer button { background:var(--accent); color:#04101f; border:0; border-radius:10px; padding:0 18px; font-weight:600; cursor:pointer; }
  footer button:disabled { opacity:.5; cursor:default; }
</style>
</head>
<body>
  <div class="app">
    <header>
      <div class="title">Dev.Play Agent <span>· MCP tool tracer</span></div>
      <button class="newchat" id="newchat">+ New chat</button>
    </header>
    <div class="chat" id="chat"></div>
    <footer>
      <input id="input" placeholder="Type a question…" autocomplete="off" />
      <button id="send">Send</button>
    </footer>
  </div>
<script>
  const chat = document.getElementById("chat");
  const input = document.getElementById("input");
  const sendBtn = document.getElementById("send");

  function el(tag, cls, text) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (text != null) e.textContent = text;
    return e;
  }
  function addRow(role, text, isErr) {
    const row = el("div", "row " + role + (isErr ? " err" : ""));
    const bubble = el("div", "bubble", text);
    row.appendChild(bubble);
    chat.appendChild(row);
    chat.scrollTop = chat.scrollHeight;
    return bubble;
  }
  function renderTools(bubble, tools) {
    if (!tools || !tools.length) return;
    const d = el("details", "tools"); d.open = true;
    d.appendChild(el("summary", null, "tools used (" + tools.length + ")"));
    for (const t of tools) {
      const call = el("div", "call");
      const name = el("div", "name");
      name.appendChild(el("span", "dot" + (t.isError ? " err" : "")));
      name.appendChild(document.createTextNode((t.tool || "") ));
      name.appendChild(el("span", "server", " · " + (t.server || "")));
      call.appendChild(name);
      const inp = el("div", "kv"); inp.innerHTML = "in: <code></code>";
      inp.querySelector("code").textContent = JSON.stringify(t.input ?? {});
      call.appendChild(inp);
      const out = el("div", "kv" + (t.isError ? " err" : "")); out.innerHTML = "out: <code></code>";
      out.querySelector("code").textContent = t.resultSummary || "";
      call.appendChild(out);
      d.appendChild(call);
    }
    bubble.appendChild(d);
  }

  async function send() {
    const message = input.value.trim();
    if (!message) return;
    input.value = ""; sendBtn.disabled = true;
    addRow("user", message);
    const thinking = addRow("bot", "…");
    try {
      const res = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message }),
      });
      const data = await res.json();
      if (!res.ok) {
        thinking.textContent = data.error || "Something went wrong.";
        thinking.parentElement.classList.add("err");
        renderTools(thinking, data.tools);
      } else {
        thinking.textContent = data.reply || "(no answer)";
        renderTools(thinking, data.tools);
      }
    } catch (e) {
      thinking.textContent = "Network error: " + e.message;
      thinking.parentElement.classList.add("err");
    } finally {
      sendBtn.disabled = false; input.focus();
    }
  }

  sendBtn.addEventListener("click", send);
  input.addEventListener("keydown", (e) => { if (e.key === "Enter") send(); });
  document.getElementById("newchat").addEventListener("click", async () => {
    await fetch("/api/reset", { method: "POST" });
    chat.innerHTML = ""; input.focus();
  });
  input.focus();
</script>
</body>
</html>
```

**Step 2: Manual check**

Run: `npm run dev`, open `http://localhost:4000`. The chat shell renders, "New chat" clears, input focuses. (Live answers need Task 9 prerequisites.)

**Step 3: Commit**

```bash
git add agent-orchestrator/public/index.html
git commit -m "feat(agent-orchestrator): chat UI with tools-used panel"
```

---

## Task 9: README + full test run + end-to-end manual verification

**Files:**
- Create: `agent-orchestrator/README.md`

**Step 1: Write `agent-orchestrator/README.md`**

````markdown
# agent-orchestrator

A host-run TypeScript app (Claude Agent SDK) that chats over the four Dev.Play
MCP servers and shows which MCP tools each answer used.

## Why it runs on the host
It reuses your existing **Claude Code login** (subscription OAuth) — no
`ANTHROPIC_API_KEY`. The MCP servers stay in Docker; this app reaches them on
`localhost`.

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

## Endpoints
- `GET  /` — chat UI
- `POST /api/chat` `{message}` → `{reply, tools[]}`
- `POST /api/reset` — start a new conversation
- `GET  /api/health` — MCP server reachability

## Test
```bash
npm test
```
````

**Step 2: Run the full test suite**

Run: `cd agent-orchestrator && npm test`
Expected: PASS — all files (`mcp-config`, `parse`, `agent`, `health`, `app`).

**Step 3: Typecheck**

Run: `npm run typecheck`
Expected: no errors.

**Step 4: End-to-end manual verification (live)**

Prereqs: `docker compose up -d` from repo root; logged into Claude Code; keys in repo `.env` (`OPENAI_API_KEY`, `TAVILY_API_KEY`).

Run: `npm run dev`, open `http://localhost:4000`, and confirm each route:

| Ask | Expect tool(s) |
| --- | --- |
| "What's on the schedule for the keynote?" | `devplay-info-mcp · DevPlaySchedule` |
| "What does the wiki say about <topic>?" | `rag-search-mcp · searchWiki` |
| "What's the latest news about <current event>?" | `web-search-mcp · searchWeb` |
| "Have we answered questions about X before?" | `chat-history-mcp · searchChatHistory` |

Also confirm: stop one MCP container (`docker compose stop web-search-mcp`), ask a web question, and verify the tools panel shows a **red** entry with the error — then restart it.

**Step 5: Commit**

```bash
git add agent-orchestrator/README.md
git commit -m "docs(agent-orchestrator): README + verification checklist"
```

---

## Done criteria

- `npm test` and `npm run typecheck` pass in `agent-orchestrator/`.
- `npm run dev` serves the chat UI; questions route to the right MCP server and the tools-used panel lists each call (red on failure).
- App reuses Claude Code login (no `ANTHROPIC_API_KEY` anywhere).
- Repo `docker-compose.yml` and the four MCP modules are unchanged.

## Risks / watch-outs

- **SDK auth:** if `query()` throws an auth error, the `/api/chat` 500 includes the "run `claude login`" hint. Confirm you're logged in.
- **Model string:** `MODEL=sonnet` (alias) is the default; switch to `opus` in `.env` if preferred. If a model alias is rejected under your subscription, try the other.
- **`settingSources: []`** keeps the agent from inheriting the repo's `CLAUDE.md`/`.mcp.json`. Keep it — it makes the demo reproducible.
- **SDK field drift:** the message-shape assumptions live entirely in `parse.ts`; if a future SDK version changes block shapes, that one file (and its tests) is where to adjust.
````
