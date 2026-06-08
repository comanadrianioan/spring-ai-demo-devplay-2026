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

/** Mutable accumulator of MCP tool calls, in first-seen order. */
interface ToolAccumulator {
  byId: Map<string, ToolCall>;
  order: string[];
}

/** Record each MCP `tool_use` block from an assistant message. */
function collectToolUses(content: unknown, acc: ToolAccumulator): void {
  for (const block of asBlocks(content)) {
    if (block?.type === "tool_use" && typeof block.name === "string" && block.name.startsWith("mcp__")) {
      const { server, tool } = splitToolName(block.name);
      acc.byId.set(block.id, { id: block.id, server, tool, input: block.input, resultSummary: "", isError: false });
      acc.order.push(block.id);
    }
  }
}

/** Fill in result summary / error flag from a user message's `tool_result` blocks. */
function applyToolResults(content: unknown, acc: ToolAccumulator): void {
  for (const block of asBlocks(content)) {
    if (block?.type !== "tool_result") continue;
    const call = acc.byId.get(block.tool_use_id);
    if (call) {
      call.resultSummary = summarizeResult(block.content);
      call.isError = block.is_error === true;
    }
  }
}

/** A `result` message carries either the final reply (success) or an error. */
function readResult(m: any): { reply?: string; error?: string } {
  if (m.subtype === "success") return { reply: m.result ?? "" };
  return { error: (Array.isArray(m.errors) ? m.errors.join("; ") : "") || m.subtype || "error" };
}

/** Reduce a collected SDK message array into the turn's reply + MCP tool trace. */
export function reduceMessages(messages: any[]): TurnResult {
  const acc: ToolAccumulator = { byId: new Map(), order: [] };
  let reply = "";
  let sessionId: string | null = null;
  let error: string | null = null;

  for (const m of messages) {
    if (m?.session_id) sessionId = m.session_id;

    if (m?.type === "assistant") {
      collectToolUses(m.message?.content, acc);
    } else if (m?.type === "user") {
      applyToolResults(m.message?.content, acc);
    } else if (m?.type === "result") {
      const r = readResult(m);
      if (r.reply !== undefined) reply = r.reply;
      if (r.error !== undefined) error = r.error;
    }
  }

  return { reply, tools: acc.order.map((id) => acc.byId.get(id)!), sessionId, error };
}
