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
