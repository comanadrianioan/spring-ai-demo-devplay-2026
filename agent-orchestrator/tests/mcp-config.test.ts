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
