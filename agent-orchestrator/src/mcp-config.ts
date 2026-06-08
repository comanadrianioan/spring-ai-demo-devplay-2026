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
