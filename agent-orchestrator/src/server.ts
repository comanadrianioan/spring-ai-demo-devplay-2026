import { createApp } from "./app";
import { serverDefs } from "./mcp-config";
import { pingSse } from "./health";

const port = Number(process.env.PORT ?? 4000);
const app = createApp();

app.listen(port, async () => {
  console.log(`agent-orchestrator → http://localhost:${port}`);
  // Friendly startup check so a down MCP server is obvious on stage.
  for (const s of serverDefs()) {
    const up = await pingSse(s.url);
    console.log(`  [${up ? "ok" : "DOWN"}] ${s.key} ${s.url}`);
  }
});
