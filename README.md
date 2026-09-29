# TierForge

Resilient bulk store scoring & tiering — a job engine that enriches thousands
of stores against a flaky rate-limited API, then scores and tiers them.

This repo contains three pieces, run together locally:

- `simulator/` — the provided flaky enrichment API (do not modify; see
  `simulator/README.md` for its contract).
- `backend/` — the TierForge Spring Boot application.
- `frontend/` — the TierForge React app (not yet built — see project status).

## Prerequisites

- **Java 21+** (`java -version`)
- **Docker** (for PostgreSQL via Docker Compose, and for the backend's tests,
  which use Testcontainers)
- **Node.js** (for the frontend, once it exists)

Gradle itself does not need to be installed — both `simulator/` and
`backend/` ship their own Gradle wrapper.

## Running everything locally

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

## Running tests

```
cd backend
./gradlew test
```

Tests use Testcontainers to start their own throwaway Postgres — no need to
have `docker compose up` running first for `./gradlew test` itself (Docker
still needs to be running, though).

## Project status

- Done: Foundation — schema, CSV upload, job creation.
- Not yet built: enrichment job engine (calls the simulator, handles rate
  limits/retries/timeouts), scoring & tiering engine, React frontend/dashboard.

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

## Known limitations (Foundation stage)

- No enrichment, scoring, or frontend yet — CSV upload only creates
  `PENDING` units; nothing processes them yet.
- Running multiple jobs concurrently and resuming a job across a process
  restart are out of scope for the whole exercise.
- Default ports (5432 for Postgres, 8080 for the backend) were remapped to
  5433/8090 to avoid local port conflicts on the machine this was built on;
  adjust back if your environment doesn't have that conflict.
