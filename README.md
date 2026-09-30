# TierForge

Resilient bulk store scoring & tiering — a job engine that enriches thousands
of stores against a flaky rate-limited API, then scores and tiers them.

This repo contains three pieces, run together locally:

- `simulator/` — the provided flaky enrichment API (do not modify; see
  `simulator/README.md` for its contract).
- `backend/` — the TierForge Spring Boot application.
- `frontend/` — the TierForge React app (see `frontend/README.md`).

## Prerequisites

- **Java 21+** (`java -version`)
- **Docker** (for PostgreSQL via Docker Compose, and for the backend's tests,
  which use Testcontainers)
- **Node.js** (for the frontend, once it exists)

Gradle itself does not need to be installed — both `simulator/` and
`backend/` ship their own Gradle wrapper.

For the frontend: `cd frontend && npm install` once, then `npm run dev`.

## Running everything locally

**Quickest path — one script starts Postgres, the simulator, the backend, and
the frontend together:**
```
./run.sh
```
It checks prerequisites, waits for each service to come up before starting
the next, runs `npm install` on first use, and prints each service's URL
when ready. Logs go to `logs/*.log`. Ctrl+C stops the simulator, backend,
and frontend (Postgres is left running — `docker compose down` to stop that
too). Then skip to step 4 below.

**Manual path — same steps, one terminal each (useful if you want to
restart a single piece without the others):**

1. **Start Postgres:**
   ```
   docker compose up -d
   ```
   Publishes on host port **5433** (not the default 5432 — chosen to avoid
   colliding with any Postgres already running locally on this machine;
   change it in `docker-compose.yml` if you'd rather use a different port).

2. **Start the enrichment simulator** (in one terminal):
   ```
   cd simulator
   ./run.sh
   ```
   Listens on port 8000. See `simulator/README.md` for its API contract and
   documented failure modes.

3. **Start the backend** (in another terminal):
   ```
   cd backend
   ./gradlew bootRun
   ```
   Listens on port **8090** (not the default 8080 — same reasoning as above;
   change it in `backend/src/main/resources/application.properties` if you
   prefer a different port, keeping it in sync with `docker-compose.yml`'s
   Postgres port if you change that too).

4. **Upload a store list:**
   ```
   curl -X POST http://localhost:8090/api/jobs \
     -F "file=@sample-data/stores_5000.csv"
   ```
   Returns the created job as JSON. Fetch it again with
   `curl http://localhost:8090/api/jobs/<id>`.

5. **Start the enrichment job and watch it progress:**
   ```
   curl -X POST http://localhost:8090/api/jobs/<id>/start
   curl http://localhost:8090/api/jobs/<id>
   ```
   The second call's `pending`/`inProgress`/`succeeded`/`failed` counts update as the job runs — at
   5 requests/sec, a 5,000-store job takes 15+ minutes. Poll it again anytime.

   To see why any store failed:
   ```
   curl "http://localhost:8090/api/jobs/<id>/store-units?status=FAILED"
   ```

6. **Configure scoring & tiering, then view the dashboard:**
   ```
   curl -X POST http://localhost:8090/api/jobs/<id>/scoring \
     -H "Content-Type: application/json" \
     -d '{"footfallBar":15000,"footfallWeight":50,"revenueBar":150000,"revenueWeight":30,"sizeBar":8000,"sizeWeight":20,"tierLargeThreshold":70,"tierMediumThreshold":40}'
   ```
   Weights must sum to 100. Returns the tier breakdown (Large/Medium/Small counts) immediately —
   this never calls the enrichment API, only reads data already fetched in step 5, so it's fast and
   safe to resubmit anytime you want to try different bars/weights/thresholds.

   List scored stores, optionally filtered by tier:
   ```
   curl "http://localhost:8090/api/jobs/<id>/scores?tier=LARGE"
   ```

7. **Or skip the curl commands and use the UI** (in another terminal):
   ```
   cd frontend
   npm install   # first time only
   npm run dev
   ```
   Open the printed local URL (defaults to http://localhost:5173, picks the next free port if
   taken). Walks through all of steps 4-6 above — upload, start, watch progress, configure
   scoring, view the dashboard — with the backend running from step 3.

## Running tests

```
cd backend
./gradlew test
```

Tests use Testcontainers to start their own throwaway Postgres — no need to
have `docker compose up` running first for `./gradlew test` itself (Docker
still needs to be running, though).

## Project status

All four steps of the exercise are built: Foundation (schema, CSV upload, job creation), the
enrichment job engine (rate-limited, retrying, self-healing worker pool with progress/failure
reporting), the scoring & tiering engine (configurable bars/weights/thresholds, fast and safely
re-runnable without re-enriching), and the React frontend/dashboard covering the full flow.

## Architecture notes

- Backend and simulator are separate Gradle projects (`backend/`,
  `simulator/`) — not a multi-module build — so each can be built/run/tested
  independently, matching how a real "your app + a black-box third-party
  dependency" setup would look.
- Schema changes go through Flyway migrations (`backend/src/main/resources/db/migration/`)
  rather than Hibernate auto-DDL, so the schema's evolution is explicit and
  reviewable.
- The data model separates jobs, per-store units of work, and (in later
  sub-projects) raw enrichment results and computed scores/tiers into
  distinct tables rather than one wide table.
- Backend integration tests use Testcontainers' singleton-container pattern
  (one Postgres container shared across the whole test JVM, started
  manually in `AbstractIntegrationTest`'s static initializer) rather than
  `@Testcontainers`/`@Container`, which restarts a container per test class
  even for a field inherited from a shared base class.
- The enrichment engine claims each `store_unit` with a fresh random
  `lease_token` (an atomic `UPDATE ... RETURNING`, safe under concurrent
  claimers via Postgres's own row locking). Every outcome write is
  conditional on that exact token — a response that arrives late, after its
  unit has already been reclaimed and retried, finds its token stale and
  silently no-ops instead of corrupting the newer attempt's result.
- Each orchestrator poll tick claims at most `rateLimitPerSecond *
  leaseDurationSeconds` units (not every eligible unit at once). Claiming
  unboundedly — e.g. all 5,000 units of a large job in one shot — means most
  of them just sit queued for a rate-limiter permit until their lease
  expires before ever attempting a real call; they get reclaimed (bumping
  `attempt_count`) without a genuine outcome, and once attempts run out
  they're permanently stuck: unclaimable, but never marked `FAILED` either.
  This was found via a real production-scale run, not caught by tests until
  a dedicated regression test (`EnrichmentOrchestratorHighContentionTest`)
  reproduced the same imbalance at a small scale via property overrides.
- Scoring/tiering is a pure read-then-compute step over `store_units` +
  `enrichment_results` — it never calls the enrichment API, and each
  resubmission fully replaces the job's prior `store_scores` rows (a bulk
  `@Modifying` delete followed by a fresh insert, not the derived
  `deleteBy...` form — that form only queues removal in the persistence
  context, and Hibernate's default flush order runs inserts before deletes
  regardless of call order, which would otherwise violate the table's
  unique constraint on a re-run).
- The frontend (Vite) proxies `/api/*` to the backend in dev (`frontend/vite.config.ts`) instead
  of configuring CORS on the backend — one less moving part for local development.

## Known limitations

- Running multiple jobs concurrently and resuming a job across a process
  restart are out of scope for the whole exercise.
- The frontend assumes a single job at a time (matching the backend's own
  in-scope constraint) and keeps job state in memory only — refreshing the
  page loses track of the current job (you'd need its id to pick back up via
  the API directly).
- Default ports (5432 for Postgres, 8080 for the backend) were remapped to
  5433/8090 to avoid local port conflicts on the machine this was built on;
  adjust back if your environment doesn't have that conflict.
