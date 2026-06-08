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
