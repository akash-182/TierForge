#!/usr/bin/env bash
# Starts the full TierForge stack in one step: Postgres, the enrichment
# simulator, the backend, and the frontend dev server.
#
# Usage:
#   ./run.sh
#
# Press Ctrl+C to stop the simulator, backend, and frontend. The Postgres
# container is left running (it has its own lifecycle via docker compose) —
# run `docker compose down` separately if you want to stop it too.
#
# Logs for each service are written to logs/*.log (gitignored) so you can
# `tail -f logs/backend.log` etc. while it's running.

set -uo pipefail
cd "$(dirname "$0")"

REQUIRED_JAVA_MAJOR=21
SIMULATOR_PORT="${SIMULATOR_PORT:-8000}"
BACKEND_PORT="${BACKEND_PORT:-8090}"

fail() {
    echo "Error: $1" >&2
    exit 1
}

# --- Prerequisite checks ---

command -v docker >/dev/null 2>&1 || fail "'docker' not found on PATH. Install Docker Desktop: https://docs.docker.com/desktop/"
command -v node >/dev/null 2>&1 || fail "'node' not found on PATH. Install Node.js: https://nodejs.org/"
command -v java >/dev/null 2>&1 || fail "'java' not found on PATH. Java ${REQUIRED_JAVA_MAJOR}+ is required."

java_version_output="$(java -version 2>&1 | head -n 1)"
java_version_string="$(echo "$java_version_output" | sed -E 's/^[^"]*"([^"]+)".*/\1/')"
java_major="$(echo "$java_version_string" | cut -d. -f1)"
if [ "$java_major" = "1" ]; then
    java_major="$(echo "$java_version_string" | cut -d. -f2)"
fi
if ! [[ "$java_major" =~ ^[0-9]+$ ]] || [ "$java_major" -lt "$REQUIRED_JAVA_MAJOR" ]; then
    fail "found Java '${java_version_output}', but Java ${REQUIRED_JAVA_MAJOR}+ is required."
fi

echo "Prerequisites OK (Java ${java_major}, Docker, Node $(node -v))."
echo

mkdir -p logs

# --- Helpers ---

wait_for_port() {
    local port="$1" name="$2" tries=0
    until (exec 3<>"/dev/tcp/localhost/${port}") 2>/dev/null; do
        exec 3>&- 2>/dev/null || true
        tries=$((tries + 1))
        if [ "$tries" -gt 90 ]; then
            echo "Timed out waiting for ${name} on port ${port}. Check logs/${name}.log" >&2
            return 1
        fi
        sleep 2
    done
    exec 3>&- 2>/dev/null || true
}

PIDS=()
cleanup() {
    echo
    echo "Shutting down simulator/backend/frontend..."
    for pid in "${PIDS[@]:-}"; do
        kill "$pid" 2>/dev/null || true
    done
    wait 2>/dev/null || true
    echo "Stopped. Postgres container left running — 'docker compose down' to stop it too."
}
trap cleanup EXIT INT TERM

# --- Postgres ---

echo "Starting Postgres (docker compose)..."
docker compose up -d
echo "Waiting for Postgres on port 5433..."
wait_for_port 5433 postgres || fail "Postgres never became reachable. Check 'docker compose logs'."
echo "Postgres ready."
echo

# --- Simulator ---

echo "Starting enrichment simulator on port ${SIMULATOR_PORT} (logs/simulator.log)..."
(cd simulator && ./gradlew bootRun --args="--server.port=${SIMULATOR_PORT}") \
    >logs/simulator.log 2>&1 &
PIDS+=($!)
wait_for_port "${SIMULATOR_PORT}" simulator || fail "Simulator failed to start. Check logs/simulator.log."
echo "Simulator ready."
echo

# --- Backend ---

echo "Starting backend on port ${BACKEND_PORT} (logs/backend.log)..."
(cd backend && ./gradlew bootRun) >logs/backend.log 2>&1 &
PIDS+=($!)
wait_for_port "${BACKEND_PORT}" backend || fail "Backend failed to start. Check logs/backend.log."
echo "Backend ready."
echo

# --- Frontend ---

if [ ! -d frontend/node_modules ]; then
    echo "Installing frontend dependencies (first run only)..."
    (cd frontend && npm install)
fi

echo "Starting frontend dev server (logs/frontend.log)..."
(cd frontend && npm run dev) >logs/frontend.log 2>&1 &
PIDS+=($!)
sleep 3
frontend_url="$(grep -Eo 'http://localhost:[0-9]+' logs/frontend.log | head -n 1 || true)"

echo
echo "=================================================================="
echo "TierForge is running:"
echo "  Simulator:  http://localhost:${SIMULATOR_PORT}/health"
echo "  Backend:    http://localhost:${BACKEND_PORT}/api/jobs"
echo "  Frontend:   ${frontend_url:-see logs/frontend.log for the URL}"
echo
echo "Press Ctrl+C to stop the simulator, backend, and frontend."
echo "=================================================================="
echo

wait
