import express, { type Express } from "express";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { runTurn as defaultRunTurn } from "./agent";
import { serverDefs as defaultServerDefs } from "./mcp-config";
import { pingSse as defaultPingSse, authHint } from "./health";

const here = path.dirname(fileURLToPath(import.meta.url));

async function defaultClearChatHistory(): Promise<{ cleared: boolean }> {
  const base = process.env.CHROMA_URL ?? "http://localhost:8000";
  const url = `${base}/api/v2/tenants/default_tenant/databases/default_database/collections/chat_history`;
  const resp = await fetch(url, { method: "DELETE" });
  if (!resp.ok && resp.status !== 404) throw new Error(`Chroma ${resp.status}`);
  return { cleared: true };
}

export interface AppDeps {
  runTurn?: typeof defaultRunTurn;
  serverDefs?: typeof defaultServerDefs;
  pingSse?: typeof defaultPingSse;
  clearChatHistory?: () => Promise<{ cleared: boolean }>;
}

export function createApp(deps: AppDeps = {}): Express {
  const runTurn = deps.runTurn ?? defaultRunTurn;
  const serverDefs = deps.serverDefs ?? defaultServerDefs;
  const pingSse = deps.pingSse ?? defaultPingSse;
  const clearChatHistory = deps.clearChatHistory ?? defaultClearChatHistory;

  const app = express();
  app.use(express.json());

  // One shared in-memory conversation (single-presenter demo).
  let sessionId: string | null = null;

  app.post("/api/chat", async (req, res) => {
    const message = String(req.body?.message ?? "").trim();
    if (!message) {
      res.status(400).json({ error: "message is required" });
      return;
    }
    try {
      const result = await runTurn(message, sessionId);
      const resumed = sessionId !== null;
      sessionId = result.sessionId ?? sessionId; // advance regardless of error (e.g. max_turns)
      // Debug trace: how many MCP tools fired this turn, and whether it resumed a session.
      console.log(
        `[chat] ${resumed ? "resume" : "new"} → ${result.tools.length} tool(s)` +
          (result.tools.length ? ": " + result.tools.map((t) => `${t.server}/${t.tool}${t.isError ? "(err)" : ""}`).join(", ") : ""),
      );
      if (result.error) {
        res.status(502).json({ error: result.error, tools: result.tools });
        return;
      }
      res.json({ reply: result.reply, tools: result.tools });
    } catch (e) {
      res.status(500).json({ error: authHint(e) });
    }
  });

  app.post("/api/reset", (_req, res) => {
    sessionId = null;
    res.json({ ok: true });
  });

  app.post("/api/clear-chat-history", async (_req, res) => {
    try {
      const result = await clearChatHistory();
      res.json(result);
    } catch (e) {
      res.status(500).json({ error: String(e) });
    }
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
