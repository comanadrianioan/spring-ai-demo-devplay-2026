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
