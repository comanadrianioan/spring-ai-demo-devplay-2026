export async function pingSse(url: string, timeoutMs = 1500): Promise<boolean> {
  try {
    const res = await fetch(url, {
      headers: { Accept: "text/event-stream" },
      signal: AbortSignal.timeout(timeoutMs),
    });
    // SSE keeps the body open; we only needed the headers. Cancel the stream.
    try { await (res as any).body?.cancel?.(); } catch { /* ignore */ }
    return res.ok || res.status === 400 || res.status === 405;
  } catch {
    return false;
  }
}

export function authHint(e: unknown): string {
  const msg = e instanceof Error ? e.message : String(e);
  if (/auth|login|credential|unauthor/i.test(msg)) {
    return `${msg} — make sure you're logged into Claude Code (run: claude login).`;
  }
  return msg;
}
