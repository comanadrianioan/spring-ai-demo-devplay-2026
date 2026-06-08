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
