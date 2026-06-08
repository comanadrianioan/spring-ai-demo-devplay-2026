import { describe, it, expect, vi } from "vitest";
import { runTurn } from "../src/agent";

function fakeStream(messages: any[]) {
  return (async function* () {
    for (const m of messages) yield m;
  })();
}

describe("runTurn", () => {
  it("passes options through and returns the reduced result", async () => {
    const queryImpl = vi.fn((_params: { prompt: any; options: any }) =>
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
    const queryImpl = vi.fn((_params: { prompt: any; options: any }) => fakeStream([{ type: "result", subtype: "success", session_id: "s2", result: "ok" }]));
    await runTurn("again", "s1", { env: {}, queryImpl: queryImpl as any });
    expect(queryImpl.mock.calls[0][0].options.resume).toBe("s1");
  });
});
