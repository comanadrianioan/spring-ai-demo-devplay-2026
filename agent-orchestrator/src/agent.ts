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
      maxTurns: Number(env.MAX_TURNS) || 12,
      ...(sessionId ? { resume: sessionId } : {}),
    },
  });

  const collected: any[] = [];
  for await (const m of response) collected.push(m);
  return reduceMessages(collected);
}
