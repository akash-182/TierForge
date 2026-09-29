# TierForge Enrichment API Simulator

A local stand-in for the "third-party" enrichment API used in the TierForge take-home exercise. Every candidate runs the same simulator with the same behavior, so results are comparable.

Java/Spring Boot port of the original Python/FastAPI simulator — same HTTP contract, same rate limit, same error/hang rates.

## Prerequisites

- **Java 21 or newer** — that's the only thing you need to install yourself.
  - Check what you have: `java -version`
  - Install if missing:
    - macOS (Homebrew): `brew install openjdk@21`
    - Ubuntu/Debian: `sudo apt-get install openjdk-21-jdk`
    - Windows/other: https://adoptium.net/temurin/releases/?version=21
- **Gradle does NOT need to be installed separately.** This project ships the Gradle wrapper (`./gradlew`), which downloads the exact Gradle version it needs on first run.

## Quick start

```
./run.sh
```

This checks your Java version and starts the service on port 8000. To use a different port (e.g. if 8000 is already taken):

```
PORT=8123 ./run.sh
```

Once running, verify it's up:
```
curl http://localhost:8000/health
```

## Running manually (without run.sh)

```
./gradlew bootRun
```

or, to use a different port:

```
./gradlew bootRun --args='--server.port=8123'
```

## Build & test

```
./gradlew build
```

This compiles the project and runs the JUnit test suite (rate limiting, deterministic metrics, `/health`).

## Contract

### `POST /enrich`

Request:
```json
{
  "store_id": "ST000001",
  "store_name": "Fresh Supermarket #1",
  "address": "71 Church Street",
  "city": "New Delhi",
  "state": "Delhi"
}
```

Success response (`200`):
```json
{
  "store_id": "ST000001",
  "estimated_monthly_footfall": 18234,
  "estimated_monthly_revenue": 142033.50,
  "store_size_sqft": 6210
}
```

Metrics are **deterministic per `store_id`** — calling the same store twice returns the same values from this running instance. (The specific numbers are Java-RNG-derived and will differ from the original Python simulator's output for the same `store_id`; both are internally consistent.)

### Documented failure modes

| Behavior | Rate | Notes |
|---|---|---|
| `429 Too Many Requests` | whenever calls exceed **5 requests/second** globally | Applies across all callers, not per-connection. |
| `500 Internal Server Error` | ~10% of successful-rate-limit calls | Transient — treat as retryable. |
| Hangs **50s** before eventually returning `200` | ~2% of successful-rate-limit calls | Simulates a slow/stuck upstream call. Long enough that a system with reasonable timeouts should have already given up on and reclaimed this unit of work before the response arrives — the late response still carries valid data. |

Error responses use the same `{"detail": "..."}` body shape as the original simulator.

### `GET /health`

Returns `{"status": "ok"}`. Not rate-limited.

## Notes for candidates

- Do not modify this service — treat it as a black box you don't control, exactly like a real third-party API.
- The rate limit is global across the whole simulator process, not per client/IP.
