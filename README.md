# spring-ai-demo-devplay-2026

Spring Boot 4 + Spring AI MCP server demo.

## Running Locally

```bash
./mvnw spring-boot:run
```

## Docker

### Build

```bash
docker build -t spring-ai-devplay-2026 .
```

### Run

```bash
docker run -p 8080:8080 \
  -e SPRING_AI_OPENAI_API_KEY=your_key_here \
  spring-ai-devplay-2026
```

The app will be available at `http://localhost:8080`.

## Deploying to Render.com

This repo includes a `render.yaml` Blueprint for one-click deployment.

### Option A — Blueprint (recommended)

1. Push this repo to GitHub.
2. In the [Render Dashboard](https://dashboard.render.com), click **New → Blueprint**.
3. Connect your repository. Render reads `render.yaml` and creates the Web Service automatically.
4. Add any required environment variables (e.g. API keys) in the Render dashboard.
5. Render builds the Docker image and deploys it — done.

### Option B — Manual Dashboard

1. Push this repo to GitHub.
2. Click **New → Web Service** in Render.
3. Connect your repository.
4. Set **Environment** to **Docker**.
5. Add env vars under **Environment** settings.
6. Click **Create Web Service**.

### Required Environment Variables

| Variable | Description |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Set to `prod` (pre-configured in `render.yaml`) |
| `SPRING_AI_OPENAI_API_KEY` | Your OpenAI API key (or equivalent AI provider key) |
