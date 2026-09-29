# TierForge Frontend

React + TypeScript + Vite. Covers the full flow: upload a store list, run/watch the enrichment
job, configure scoring & tiering, and view the dashboard. See the top-level `README.md` for how
to run this alongside the backend and simulator.

## Development

```
npm install
npm run dev
```

The dev server proxies `/api/*` requests to `http://localhost:8090` (see `vite.config.ts`), so no
CORS setup is needed on the backend — just make sure the backend is running.

## Build

```
npm run build
```
